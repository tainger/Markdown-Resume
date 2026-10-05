package com.agentmate.validation;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Synthetic fixtures only; no production repository, logs or write APIs. */
public class EvidenceTools {
    public final List<String> calls = new CopyOnWriteArrayList<>();

    @Tool(name = "read_source", description = "Read synthetic code evidence for an allowed repository")
    public String source(@ToolParam(name = "repository", description = "Repository identifier") String repository) {
        calls.add("read_source:" + repository);
        // Fixed server-side allowlist for this PoC. Production needs authenticated ACLs.
        if (!"fixture-mse".equals(repository)) throw new SecurityException("REPOSITORY_DENIED");
        return "CODE_E1 repo=fixture-mse commit=fixture-c1 path=Config.java lines=10-12: missing listener may skip update";
    }

    @Tool(name = "query_logs", description = "Return synthetic runtime evidence")
    public String logs() {
        calls.add("query_logs");
        return "LOG_E2 instance=fixture-1: listener registered; update rejected due to version mismatch";
    }
}
