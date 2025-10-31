package com.example.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public final class McpClientRunner {
    private static final String DEFAULT_GITHUB_SERVER = "https://api.github.com/copilot/mcp";
    private static final String DEFAULT_USER_AGENT = "mcp-java-client/1.0";
    private static final String DEFAULT_SSE_ENDPOINT = "/server/sse";

    private McpClientRunner() {
    }

    public static void run() throws Exception {
        var githubToken = System.getenv("GITHUB_TOKEN");
        if (githubToken == null || githubToken.isBlank()) {
            System.err.println("GITHUB_TOKEN environment variable is required to talk to the GitHub MCP server.");
            return;
        }

        var baseUrl = System.getenv().getOrDefault("GITHUB_MCP_SERVER_URL", DEFAULT_GITHUB_SERVER);
        var userAgent = System.getenv().getOrDefault("GITHUB_MCP_USER_AGENT", DEFAULT_USER_AGENT);
        var toolToCall = System.getenv("GITHUB_MCP_TOOL");
        var sseEndpoint = System.getenv().getOrDefault("GITHUB_MCP_SSE_ENDPOINT", DEFAULT_SSE_ENDPOINT);

        McpSyncHttpClientRequestCustomizer authCustomizer = (builder, method, uri, body, context) -> builder
            .header("Authorization", "Bearer " + githubToken)
            .header("User-Agent", userAgent)
            .header("X-GitHub-Api-Version", "2023-07-07");

        System.out.printf("Connecting to GitHub MCP at %s (SSE endpoint: %s)\n", baseUrl, sseEndpoint);

        var transport = HttpClientSseClientTransport.builder(baseUrl)
            .sseEndpoint(sseEndpoint)
            .customizeClient(clientBuilder -> clientBuilder
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL))
            .httpRequestCustomizer(authCustomizer)
            .build();

        try (McpSyncClient client = McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(30))
            .initializationTimeout(Duration.ofSeconds(30))
            .build()) {

            var initializeResult = client.initialize();
            if (initializeResult != null && initializeResult.serverInfo() != null) {
                var serverInfo = initializeResult.serverInfo();
                System.out.printf("Connected to GitHub MCP server: %s %s%n", serverInfo.name(), serverInfo.version());
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
                var argumentsJson = System.getenv("GITHUB_MCP_TOOL_ARGS_JSON");
                McpSchema.CallToolRequest request;
                try {
                    request = argumentsJson == null || argumentsJson.isBlank()
                        ? new McpSchema.CallToolRequest(toolToCall, Collections.emptyMap())
                        : new McpSchema.CallToolRequest(McpJsonMapper.getDefault(), toolToCall, argumentsJson);
                }
                catch (IllegalArgumentException ex) {
                    System.err.printf("Invalid JSON for GITHUB_MCP_TOOL_ARGS_JSON: %s%n", ex.getMessage());
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
