package com.wolinski.adr;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** One episode's copy of the shop, and the only file system the agent's tools can reach. */
public final class Workspace {

    private static final int MAX_OUTPUT = 8_000;
    private static final Duration TEST_TIMEOUT = Duration.ofMinutes(3);

    final Path root;
    final Arm arm;

    private Workspace(Path root, Arm arm) {
        this.root = root;
        this.arm = arm;
    }

    public static Workspace create(Path fixture, Arm arm, Path parent) throws IOException {
        Path root = Files.createTempDirectory(parent, arm.label + "-");
        copyTree(fixture.resolve("base"), root);
        if (arm.hasDecisions()) {
            copyTree(fixture.resolve("overlays/folder"), root);
        }
        if (arm.hasRules()) {
            copyTree(fixture.resolve("overlays/enforced"), root);
        }
        Files.copy(fixture.resolve("agents").resolve(arm.agentsFile), root.resolve("AGENTS.md"));
        return new Workspace(root, arm);
    }

    public String instructions() throws IOException {
        return Files.readString(root.resolve("AGENTS.md"));
    }

    /** Refuses anything outside the workspace, including through a symbolic link, and the build output. */
    Path resolve(String relative) throws IOException {
        Path target = root.resolve(relative).normalize();
        if (false == target.startsWith(root)) {
            throw new IllegalArgumentException("path is outside the repository: " + relative);
        }
        for (Path walk = target; false == walk.equals(root); walk = walk.getParent()) {
            if (Files.isSymbolicLink(walk)) {
                throw new IllegalArgumentException("symbolic links are not followed: " + relative);
            }
        }
        return target;
    }

    public String list(String directory) throws IOException {
        Path start = resolve(directory);
        try (Stream<Path> files = Files.walk(start)) {
            return files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> root.relativize(path).toString())
                    .filter(path -> false == path.startsWith("target/"))
                    .sorted()
                    .reduce("", (all, path) -> all + path + "\n");
        }
    }

    public String read(String path) throws IOException {
        return Files.readString(resolve(path));
    }

    public String search(String regex) throws IOException {
        Pattern pattern = Pattern.compile(regex);
        List<String> hits = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).toList()) {
                String relative = root.relativize(file).toString();
                if (relative.startsWith("target/")) {
                    continue;
                }
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (pattern.matcher(lines.get(i)).find()) {
                        hits.add(relative + ":" + (i + 1) + ": " + lines.get(i).strip());
                    }
                }
            }
        }
        return hits.isEmpty() ? "no matches" : String.join("\n", hits);
    }

    public String write(String path, String content) throws IOException {
        Path target = resolve(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
        return "written " + path;
    }

    public String edit(String path, String oldText, String newText) throws IOException {
        String content = read(path);
        int first = content.indexOf(oldText);
        if (0 > first) {
            throw new IllegalArgumentException("old_text not found in " + path);
        }
        if (first != content.lastIndexOf(oldText)) {
            throw new IllegalArgumentException("old_text occurs more than once in " + path);
        }
        Files.writeString(resolve(path), content.substring(0, first) + newText + content.substring(first + oldText.length()));
        return "edited " + path;
    }

    public String delete(String path) throws IOException {
        Files.delete(resolve(path));
        return "deleted " + path;
    }

    public TestRun runTests() throws IOException, InterruptedException {
        return maven(root, "test");
    }

    public record TestRun(boolean passed, String output) {
    }

    /**
     * Runs code the model wrote. The API key is removed from its environment, and in the container it runs
     * as an unprivileged user, so it cannot read the key from the harness process either.
     */
    static TestRun maven(Path directory, String... goals) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        boolean isolate = Files.exists(Path.of("/usr/bin/setpriv")) && "root".equals(System.getProperty("user.name"));
        if (isolate) {
            new ProcessBuilder("chown", "-R", "1000:1000", directory.toString()).inheritIO().start().waitFor();
            for (Path parent = directory.getParent(); null != parent && false == parent.equals(Path.of("/tmp"))
                    && false == parent.equals(Path.of("/")); parent = parent.getParent()) {
                Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("rwxr-xr-x"));
            }
            command.addAll(List.of("setpriv", "--reuid=1000", "--regid=1000", "--clear-groups"));
        }
        command.addAll(List.of("mvn", "-o", "-q", "-B", "-ntp", "-Dmaven.repo.local=/m2", "-Dsurefire.useFile=true"));
        command.addAll(List.of(goals));
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);
        builder.environment().remove("ANTHROPIC_API_KEY");
        builder.environment().put("HOME", "/tmp");
        builder.environment().remove("MAVEN_ARGS");
        Path log = Files.createTempFile("mvn", ".log");
        builder.redirectOutput(log.toFile());
        Process process = builder.start();
        try {
            if (false == process.waitFor(TEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                return new TestRun(false, "tests did not finish within " + TEST_TIMEOUT.toMinutes() + " minutes");
            }
            String output = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
            int exit = process.exitValue();
            String tail = MAX_OUTPUT < output.length() ? output.substring(output.length() - MAX_OUTPUT) : output;
            return new TestRun(0 == exit, 0 == exit ? "tests passed" : tail);
        }
        finally {
            Files.deleteIfExists(log);
        }
    }

    static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            files.filter(path -> false == from.relativize(path).toString().startsWith("target")).forEach(source -> {
                try {
                    Path target = to.resolve(from.relativize(source).toString());
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    }
                    else {
                        Files.createDirectories(target.getParent());
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
