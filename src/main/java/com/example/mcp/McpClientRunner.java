package com.example.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public final class McpClientRunner {
    private static final String DEFAULT_SERVER_BASE = "http://localhost:3001";
    private static final String DEFAULT_ENDPOINT = "/mcp";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private McpClientRunner() {
    }

    public static void run() throws Exception {
        var serverBase = System.getenv().getOrDefault("MCP_SERVER_URL", DEFAULT_SERVER_BASE);
        var endpoint = System.getenv().getOrDefault("MCP_SERVER_ENDPOINT", DEFAULT_ENDPOINT);
        var toolToCall = System.getenv("MCP_TOOL");
        var toolArgsJson = System.getenv("MCP_TOOL_ARGS_JSON");

        var transport = HttpClientStreamableHttpTransport.builder(serverBase)
            .endpoint(endpoint)
            .connectTimeout(DEFAULT_TIMEOUT)
            .build();

        try (McpSyncClient client = McpClient.sync(transport)
            .requestTimeout(DEFAULT_TIMEOUT)
            .initializationTimeout(DEFAULT_TIMEOUT)
            .build()) {

            var initializeResult = client.initialize();
            if (initializeResult != null && initializeResult.serverInfo() != null) {
                var serverInfo = initializeResult.serverInfo();
                System.out.printf("Connected to MCP server: %s %s%n", serverInfo.name(), serverInfo.version());
            }

            var toolsResult = client.listTools();
            List<McpSchema.Tool> tools = toolsResult != null ? toolsResult.tools() : List.of();
            System.out.println("== Available Tools ==");
            if (tools == null || tools.isEmpty()) {
                System.out.println("(no tools exposed)");
            }
            else {
                tools.forEach(tool -> System.out.println("- " + tool.name() + " : " + tool.description()));
            }

            if (toolToCall != null && !toolToCall.isBlank()) {
                McpSchema.CallToolRequest request;
                try {
                    request = toolArgsJson == null || toolArgsJson.isBlank()
                        ? new McpSchema.CallToolRequest(toolToCall, Collections.emptyMap())
                        : new McpSchema.CallToolRequest(McpJsonMapper.getDefault(), toolToCall, toolArgsJson);
                }
                catch (IllegalArgumentException ex) {
                    System.err.printf("Invalid JSON for MCP_TOOL_ARGS_JSON: %s%n", ex.getMessage());
                    return;
                }

                var callResult = client.callTool(request);
                System.out.println("== Call Result ==");
                if (callResult == null) {
                    System.out.println("(null response)");
                }
                else if (callResult.structuredContent() != null) {
                    System.out.println(callResult.structuredContent());
                }
                else if (callResult.content() != null && !callResult.content().isEmpty()) {
                    callResult.content().forEach(item -> System.out.println(item.toString()));
                }
                else {
                    System.out.println("(empty response)");
                }
            }
        }
    }
}
