package com.agentmate.validation;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import java.time.Duration;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Enables a two-JVM restart probe with only synthetic data. */
@Component
@ConditionalOnProperty(name = "validation.probe")
public class ProbeRunner implements ApplicationRunner {
    private final ReActAgent agent;
    public ProbeRunner(ReActAgent agent) { this.agent = agent; }
    @Override public void run(ApplicationArguments args) {
        String mode = args.getOptionValues("validation.probe").get(0);
        if (!mode.equals("write") && !mode.equals("read")) throw new IllegalArgumentException("write or read required");
        var ctx = RuntimeContext.builder().userId("probe-user").sessionId("probe-session").build();
        String result = agent.call(mode.equals("write") ? "investigate marker-42" : "recall", ctx)
                .block(Duration.ofSeconds(15)).getTextContent();
        if (mode.equals("read") && !result.equals("REMEMBERED:marker-42")) {
            throw new IllegalStateException("Process restart lost history: " + result);
        }
        System.out.println("VALIDATION_PROBE=" + result);
    }
}
