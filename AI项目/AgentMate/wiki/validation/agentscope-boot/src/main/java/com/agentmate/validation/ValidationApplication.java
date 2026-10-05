package com.agentmate.validation;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.core.tool.Toolkit;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ValidationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ValidationApplication.class, args);
    }

    @Bean
    Model model() { return new ScriptedModel(); }

    @Bean
    EvidenceTools evidenceTools() { return new EvidenceTools(); }

    @Bean
    AgentStateStore stateStore(@Value("${validation.state-dir:target/agent-state}") String directory) {
        return new JsonFileAgentStateStore(Path.of(directory));
    }

    @Bean
    ReActAgent investigator(Model model, EvidenceTools tools, AgentStateStore store) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(tools);
        return ReActAgent.builder().name("agentmate-validation")
                .sysPrompt("Use evidence tools. This is a synthetic integration test, not MSE diagnosis.")
                .model(model).toolkit(toolkit).stateStore(store).maxIters(6).build();
    }
}
