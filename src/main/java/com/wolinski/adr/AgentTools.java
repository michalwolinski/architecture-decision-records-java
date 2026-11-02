package com.wolinski.adr;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The tools the agent gets. There is no shell: reading, editing and one fixed test command. */
final class AgentTools {

    static final List<Tool> ALL = List.of(
            tool("list_files", "List the files under a directory of the repository. Use \".\" for all of them.",
                    Map.of("directory", "Directory relative to the repository root")),
            tool("read_file", "Read one file.", Map.of("path", "Path relative to the repository root")),
            tool("search", "Search all files for a Java regular expression, line by line.",
                    Map.of("regex", "The regular expression")),
            tool("write_file", "Create a file, or replace all of its content.",
                    Map.of("path", "Path relative to the repository root", "content", "The full new content")),
            tool("edit_file", "Replace text that occurs exactly once in a file.",
                    Map.of("path", "Path relative to the repository root", "old_text", "The text to replace",
                            "new_text", "Its replacement")),
            tool("delete_file", "Delete one file.", Map.of("path", "Path relative to the repository root")),
            tool("run_tests", "Compile the project and run its whole test suite.", Map.of()));

    private AgentTools() {
    }

    private static Tool tool(String name, String description, Map<String, String> parameters) {
        Tool.InputSchema.Properties.Builder properties = Tool.InputSchema.Properties.builder();
        new TreeMap<>(parameters).forEach((key, text) -> properties.putAdditionalProperty(key,
                JsonValue.from(Map.of("type", "string", "description", text))));
        return Tool.builder().name(name).description(description)
                .inputSchema(Tool.InputSchema.builder().properties(properties.build())
                        .required(List.copyOf(new TreeMap<>(parameters).keySet())).build())
                .build();
    }

    /** Failures go back to the model as text, the way a real tool's error would. */
    static Result execute(Workspace workspace, String name, Map<String, String> input) {
        try {
            String output = switch (name) {
                case "list_files" -> workspace.list(required(input, "directory"));
                case "read_file" -> workspace.read(required(input, "path"));
                case "search" -> workspace.search(required(input, "regex"));
                case "write_file" -> workspace.write(required(input, "path"), required(input, "content"));
                case "edit_file" -> workspace.edit(required(input, "path"), required(input, "old_text"),
                        required(input, "new_text"));
                case "delete_file" -> workspace.delete(required(input, "path"));
                case "run_tests" -> {
                    Workspace.TestRun run = workspace.runTests();
                    if (false == run.passed()) {
                        yield "TESTS FAILED\n" + run.output();
                    }
                    yield run.output();
                }
                default -> throw new IllegalArgumentException("unknown tool " + name);
            };
            return new Result(output, false);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result("interrupted", true);
        }
        catch (Exception e) {
            return new Result(e.getClass().getSimpleName() + ": " + e.getMessage(), true);
        }
    }

    private static String required(Map<String, String> input, String key) {
        String value = input.get(key);
        if (null == value) {
            throw new IllegalArgumentException("missing parameter " + key);
        }
        return value;
    }

    record Result(String output, boolean isError) {
    }
}
