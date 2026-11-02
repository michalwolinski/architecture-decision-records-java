package com.wolinski.adr;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;

/** The agent under test: a manual tool loop over the Messages API. */
public final class ClaudeAgent implements Agent {

    private static final int MAX_TURNS = 40;
    private static final int RECORDED_OUTPUT = 4_000;
    private static final String SYSTEM = """
            You are a software engineer working in a Java repository, through the tools you are given. \
            These are the repository's instructions for agents:

            """;

    private final Function<MessageCreateParams, Message> api;
    private final String model;
    private final String effort;

    public ClaudeAgent(AnthropicClient client, String model, String effort) {
        this(client.messages()::create, model, effort);
    }

    ClaudeAgent(Function<MessageCreateParams, Message> api, String model, String effort) {
        this.api = api;
        this.model = model;
        this.effort = effort;
    }

    @Override
    public Outcome work(Task task, Workspace workspace, Ledger ledger) throws Exception {
        Pricing pricing = Pricing.of(model);
        List<MessageParam> messages = new ArrayList<>();
        messages.add(MessageParam.ofUser(task.userMessage()));
        List<Map<String, Object>> trajectory = new ArrayList<>();
        TreeSet<String> adrsRead = new TreeSet<>();
        Usage usage = Usage.NONE;
        int testRuns = 0;
        String system = SYSTEM + workspace.instructions();

        for (int turn = 1; MAX_TURNS >= turn; turn++) {
            if (ledger.exhausted()) {
                throw new BudgetExhausted();
            }
            MessageCreateParams.Builder request = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(16_000L)
                    .system(system)
                    .cacheControl(CacheControlEphemeral.builder().build())
                    .messages(messages);
            AgentTools.ALL.forEach(request::addTool);
            if (false == "none".equals(effort)) {
                request.outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.of(effort)).build());
            }
            Message response = api.apply(request.build());
            requireServedModel(response.model().asString());

            Usage used = new Usage(response.usage().inputTokens(),
                    response.usage().cacheCreationInputTokens().orElse(0L),
                    response.usage().cacheReadInputTokens().orElse(0L),
                    response.usage().outputTokens());
            usage = usage.plus(used);
            ledger.record(pricing.cost(used));
            messages.add(response.toParam());

            String text = record(response, trajectory);
            StopReason stop = response.stopReason().orElse(StopReason.END_TURN);
            if (StopReason.END_TURN.equals(stop)) {
                return new Outcome("done", text, trajectory, turn, usage, List.copyOf(adrsRead), testRuns);
            }
            if (StopReason.REFUSAL.equals(stop)) {
                return new Outcome("refusal", text, trajectory, turn, usage, List.copyOf(adrsRead), testRuns);
            }
            if (false == StopReason.TOOL_USE.equals(stop)) {
                return new Outcome("truncated", text, trajectory, turn, usage, List.copyOf(adrsRead), testRuns);
            }

            List<ContentBlockParam> results = new ArrayList<>();
            for (ContentBlock block : response.content()) {
                if (false == block.isToolUse()) {
                    continue;
                }
                ToolUseBlock call = block.asToolUse();
                Map<String, String> input = ObjectMappers.jsonMapper().convertValue(
                        call._input(), new TypeReference<Map<String, String>>() {
                        });
                AgentTools.Result result = AgentTools.execute(workspace, call.name(), input);
                testRuns += "run_tests".equals(call.name()) ? 1 : 0;
                if ("read_file".equals(call.name()) && false == result.isError()
                        && input.getOrDefault("path", "").contains("docs/adr/")) {
                    adrsRead.add(input.get("path").replaceAll(".*docs/adr/", ""));
                }
                trajectory.add(entry("tool_result", call.name(), trim(result.output())));
                results.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                        .toolUseId(call.id()).content(result.output()).isError(result.isError()).build()));
            }
            messages.add(MessageParam.ofUserOfContentBlockParams(results));
        }
        return new Outcome("max_turns", "", trajectory, MAX_TURNS, usage, List.copyOf(adrsRead), testRuns);
    }

    private void requireServedModel(String served) {
        if (false == Pattern.matches(Pattern.quote(model) + "(-\\d{8})?", served)) {
            throw new ServedModelMismatch(model + " was requested, " + served + " answered");
        }
    }

    private static String record(Message response, List<Map<String, Object>> trajectory) {
        StringBuilder text = new StringBuilder();
        for (ContentBlock block : response.content()) {
            block.text().ifPresent(part -> {
                text.append(part.text());
                trajectory.add(entry("text", null, part.text()));
            });
            block.toolUse().ifPresent(call -> trajectory.add(entry("tool_use", call.name(), trim(String.valueOf(call._input())))));
        }
        return text.toString();
    }

    private static Map<String, Object> entry(String type, String tool, String content) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", type);
        if (null != tool) {
            entry.put("tool", tool);
        }
        entry.put("content", content);
        return entry;
    }

    private static String trim(String output) {
        return RECORDED_OUTPUT < output.length() ? output.substring(0, RECORDED_OUTPUT) + "\n[trimmed in this record]" : output;
    }

    public static final class ServedModelMismatch extends RuntimeException {
        ServedModelMismatch(String message) {
            super(message);
        }
    }

    public static final class BudgetExhausted extends RuntimeException {
    }
}
