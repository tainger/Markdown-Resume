package com.agentmate.validation;

import io.agentscope.core.message.*;
import io.agentscope.core.model.*;
import io.agentscope.core.util.JsonUtils;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;

/** Deterministic model double: validates framework plumbing, never model intelligence. */
public class ScriptedModel implements Model {
    @Override public String getModelName() { return "scripted-no-network"; }

    @Override
    public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
        int lastUser = -1;
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getRole() == MsgRole.USER) lastUser = i;
        }
        String request = messages.get(lastUser).getTextContent();
        if (request.equals("recall")) {
            boolean found = messages.subList(0, lastUser).stream()
                    .anyMatch(m -> m.getTextContent().contains("marker-42"));
            return text(found ? "REMEMBERED:marker-42" : "NO_HISTORY");
        }
        var results = messages.subList(lastUser + 1, messages.size()).stream()
                .flatMap(m -> m.getContentBlocks(ToolResultBlock.class).stream()).toList();
        if (results.isEmpty()) {
            String repository = request.contains("forbidden") ? "private-other-tenant" : "fixture-mse";
            return response(toolCall("source-call", "read_source", Map.of("repository", repository)));
        }
        String evidence = results.stream().flatMap(r -> r.getOutput().stream())
                .filter(TextBlock.class::isInstance).map(TextBlock.class::cast)
                .map(TextBlock::getText).reduce("", (a, b) -> a + b);
        if (evidence.contains("REPOSITORY_DENIED")) return text("ACCESS_DENIED");
        if (results.size() == 1) return response(toolCall("logs-call", "query_logs", Map.of()));
        return text("SYNTHETIC_RESULT: " + evidence);
    }

    private ToolUseBlock toolCall(String id, String name, Map<String, Object> input) {
        // The streaming contract validates raw JSON as well as parsed arguments.
        return ToolUseBlock.builder().id(id).name(name).input(input)
                .content(JsonUtils.getJsonCodec().toJson(input)).build();
    }

    private Flux<ChatResponse> text(String value) {
        return response(TextBlock.builder().text(value).build());
    }
    private Flux<ChatResponse> response(ContentBlock block) {
        return Flux.just(ChatResponse.builder().content(List.of(block)).build());
    }
}
