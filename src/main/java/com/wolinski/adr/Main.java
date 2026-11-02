package com.wolinski.adr;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Runs agents over (arm, task, repetition) and writes one row per episode. Episodes run in a shuffled order, so
 * a slow hour of the API does not land on one arm. Attempts that never produced a scorable episode go to
 * errors.jsonl: plumbing is not scored as a model failure.
 */
public final class Main {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper PRETTY = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) throws Exception {
        Map<String, String> options = new HashMap<>();
        for (int i = 1; i + 1 < args.length; i += 2) {
            options.put(args[i].replaceFirst("^--", ""), args[i + 1]);
        }
        switch (args[0]) {
            case "eval" -> eval(options);
            case "report" -> System.out.print(Report.render(Path.of(args[1])));
            default -> throw new IllegalArgumentException(args[0]);
        }
    }

    private static void eval(Map<String, String> options) throws Exception {
        String agentName = options.getOrDefault("agent", "oracle");
        boolean paid = "claude".equals(agentName);
        String model = options.getOrDefault("model", "claude-haiku-5-5");
        String effort = options.getOrDefault("effort", "medium");
        int reps = Integer.parseInt(options.getOrDefault("reps", "1"));
        int concurrency = Integer.parseInt(options.getOrDefault("concurrency", "4"));
        long seed = Long.parseLong(options.getOrDefault("seed", "20261102"));
        Ledger ledger = new Ledger(Double.parseDouble(options.getOrDefault("budget-usd", "1")));
        List<Task> tasks = "all".equals(options.getOrDefault("tasks", "all")) ? Task.ALL
                : List.of(options.get("tasks").split(",")).stream().map(Task::byId).toList();
        if (options.containsKey("catalogue")) {
            List<String> catalogues = List.of(options.get("catalogue").split(","));
            tasks = tasks.stream().filter(task -> catalogues.contains(task.catalogue())).toList();
        }
        List<Task> chosenTasks = tasks;
        List<Arm> arms = "all".equals(options.getOrDefault("arms", "all")) ? List.of(Arm.NONE, Arm.FOLDER, Arm.POINTER, Arm.ENFORCED)
                : List.of(options.get("arms").split(",")).stream().map(Arm::of).toList();

        Agent agent;
        AnthropicClient client = null;
        if (paid) {
            client = AnthropicOkHttpClient.builder().fromEnv().maxRetries(4).build();
            agent = new ClaudeAgent(client, model, effort);
        }
        else {
            agent = ScriptedAgents.valueOf(agentName.toUpperCase());
        }

        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(Instant.now());
        String label = paid ? model + "-" + effort : agentName;
        Path out = Path.of(options.getOrDefault("out", "runs")).resolve(stamp + "-" + label);
        Files.createDirectories(out.resolve("episodes"));
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("agent", agentName);
        config.put("model", paid ? model : null);
        config.put("effort", paid ? effort : null);
        config.put("reps", reps);
        config.put("seed", seed);
        config.put("arms", arms.stream().map(arm -> arm.label).toList());
        config.put("tasks", chosenTasks.stream().map(Task::id).toList());
        PRETTY.writeValue(out.resolve("config.json").toFile(), config);

        Path fixture = Path.of("fixture").toAbsolutePath();
        Grader grader = new Grader(fixture);
        Path scratch = Files.createTempDirectory("episodes");
        record Episode(Arm arm, Task task, int rep) {
        }
        List<Episode> episodes = new ArrayList<>();
        for (Arm arm : arms) {
            for (Task task : chosenTasks) {
                for (int rep = 1; rep <= reps; rep++) {
                    episodes.add(new Episode(arm, task, rep));
                }
            }
        }
        Collections.shuffle(episodes, new Random(seed));

        Semaphore slots = new Semaphore(concurrency);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Episode episode : episodes) {
                executor.submit(() -> {
                    slots.acquireUninterruptibly();
                    try {
                        if (ledger.exhausted() || ledger.authBroken()) {
                            error(out, episode.arm(), episode.task(), episode.rep(), "skipped", "budget or authentication stop");
                            return;
                        }
                        Workspace workspace = Workspace.create(fixture, episode.arm(), scratch);
                        long started = System.nanoTime();
                        Agent.Outcome outcome = agent.work(episode.task(), workspace, ledger);
                        double seconds = (System.nanoTime() - started) / 1e9;
                        Grader.Grade grade = grader.grade(workspace, episode.task());
                        write(out, episode.arm(), episode.task(), episode.rep(), outcome, grade, seconds,
                                paid ? Pricing.of(model).cost(outcome.usage()) : 0);
                        System.out.printf("%-8s %-18s %d %s compiles=%s functional=%s violations=%s tampering=%s spent=$%.3f%n",
                                episode.arm().label, episode.task().id(), episode.rep(), outcome.status(), grade.compiles(),
                                grade.functional(), grade.violations(), grade.tampering(), ledger.spent());
                    }
                    catch (UnauthorizedException e) {
                        ledger.authFailure();
                        error(out, episode.arm(), episode.task(), episode.rep(), "auth", e.getMessage());
                    }
                    catch (AnthropicServiceException | ClaudeAgent.ServedModelMismatch | ClaudeAgent.BudgetExhausted e) {
                        error(out, episode.arm(), episode.task(), episode.rep(), e.getClass().getSimpleName(), e.getMessage());
                    }
                    catch (Exception e) {
                        error(out, episode.arm(), episode.task(), episode.rep(), e.getClass().getSimpleName(), e.toString());
                    }
                    finally {
                        slots.release();
                    }
                });
            }
        }
        if (null != client) {
            client.close();
        }
        Files.writeString(out.resolve("report.txt"), Report.render(out));
        System.out.print(Report.render(out));
        System.out.println("written to " + out);
    }

    private static synchronized void write(Path out, Arm arm, Task task, int rep, Agent.Outcome outcome, Grader.Grade grade,
            double seconds, double usd) throws IOException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("arm", arm.label);
        row.put("task", task.id());
        row.put("catalogue", task.catalogue());
        row.put("tempts", task.tempts());
        row.put("rep", rep);
        row.put("status", outcome.status());
        row.put("compiles", grade.compiles());
        row.put("functional", grade.functional());
        row.put("violations", grade.violations());
        row.put("rule_errors", grade.ruleErrors());
        row.put("visible_green", grade.visibleGreen());
        row.put("tampering", grade.tampering());
        row.put("adrs_read", outcome.adrsRead());
        row.put("test_runs", outcome.testRuns());
        row.put("turns", outcome.turns());
        row.put("input_tokens", outcome.usage().input());
        row.put("cache_write_tokens", outcome.usage().cacheWrite());
        row.put("cache_read_tokens", outcome.usage().cacheRead());
        row.put("output_tokens", outcome.usage().output());
        row.put("usd", usd);
        row.put("seconds", seconds);
        append(out.resolve("results.jsonl"), JSON.writeValueAsString(row));
        Map<String, Object> episode = new LinkedHashMap<>(row);
        episode.put("final_text", outcome.finalText());
        episode.put("trajectory", outcome.trajectory());
        PRETTY.writeValue(out.resolve("episodes").resolve(arm.label + "-" + task.id() + "-" + rep + ".json").toFile(), episode);
    }

    private static synchronized void error(Path out, Arm arm, Task task, int rep, String kind, String message) {
        try {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("arm", arm.label);
            row.put("task", task.id());
            row.put("rep", rep);
            row.put("kind", kind);
            row.put("message", message);
            append(out.resolve("errors.jsonl"), JSON.writeValueAsString(row));
            System.out.println("ERROR " + row);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void append(Path file, String line) throws IOException {
        Files.writeString(file, line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
