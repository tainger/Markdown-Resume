package com.agentmate.validation;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    @TempDir Path state;

    ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(ValidationApplication.class)
                .web(WebApplicationType.NONE)
                .properties("validation.state-dir=" + state, "spring.main.banner-mode=off")
                .run();
    }
    RuntimeContext context(String user, String session) {
        return RuntimeContext.builder().userId(user).sessionId(session).build();
    }
    String ask(ConfigurableApplicationContext app, String text, String user, String session) {
        return app.getBean(ReActAgent.class).call(text, context(user, session))
                .block(Duration.ofSeconds(15)).getTextContent();
    }

    @Test void bootStartsAndRealAgentExecutesTwoTools() {
        try (var app = start()) {
            String answer = ask(app, "investigate marker-42", "alice", "case-1");
            assertTrue(answer.contains("CODE_E1"), answer);
            assertTrue(answer.contains("LOG_E2"), answer);
            assertTrue(answer.contains("version mismatch"), answer);
            assertEquals(List.of("read_source:fixture-mse", "query_logs"), app.getBean(EvidenceTools.class).calls);
        }
    }

    @Test void fileStateSurvivesSpringContextRecreation() {
        try (var first = start()) { ask(first, "investigate marker-42", "alice", "case-1"); }
        try (var second = start()) {
            assertEquals("REMEMBERED:marker-42", ask(second, "recall", "alice", "case-1"));
            assertTrue(second.getBean(EvidenceTools.class).calls.isEmpty());
        }
    }

    @Test void differentUserAndSessionDoNotReceiveHistory() {
        try (var app = start()) {
            ask(app, "investigate marker-42", "alice", "case-1");
            assertEquals("NO_HISTORY", ask(app, "recall", "bob", "case-1"));
            assertEquals("NO_HISTORY", ask(app, "recall", "alice", "case-2"));
        }
    }

    @Test void unauthorizedRepositoryRejectedByTool() {
        try (var app = start()) {
            assertEquals("ACCESS_DENIED", ask(app, "forbidden", "alice", "case-denied"));
            assertEquals(List.of("read_source:private-other-tenant"), app.getBean(EvidenceTools.class).calls);
        }
    }

    @Test void eventStreamIncludesToolActivityAndCompletion() {
        try (var app = start()) {
            var events = app.getBean(ReActAgent.class)
                    .streamEvents(List.of(new UserMessage("investigate")), context("alice", "stream"))
                    .collectList().block(Duration.ofSeconds(15));
            assertNotNull(events);
            var types = events.stream().map(e -> e.getType().name()).toList();
            assertTrue(types.contains("TOOL_CALL_START"), types.toString());
            assertTrue(types.contains("AGENT_RESULT"), types.toString());
        }
    }
}
