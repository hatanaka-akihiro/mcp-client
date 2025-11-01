package com.example.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpTransportException;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public final class GithubMcpClientRunner {
    private static final String DEFAULT_SERVER_BASE = "https://api.githubcopilot.com";
    private static final String DEFAULT_ENDPOINT = "/mcp";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final String DEFAULT_USER_AGENT = "mcp-java-client/1.0";

    private GithubMcpClientRunner() {
    }

    public static void run() throws Exception {
        var env = System.getenv();
        var serverBase = env.getOrDefault("GITHUB_MCP_SERVER_URL", DEFAULT_SERVER_BASE);
        var endpoint = env.getOrDefault("GITHUB_MCP_ENDPOINT", DEFAULT_ENDPOINT);
        var pat = resolvePersonalAccessToken(env);

        if (pat == null || pat.isBlank()) {
            System.err.println("GitHub PAT is required. Set GITHUB_PAT, GITHUB_TOKEN, or GITHUB_MCP_PAT.");
            return;
        }

        var userAgent = env.getOrDefault("GITHUB_MCP_USER_AGENT", DEFAULT_USER_AGENT);
        var authScheme = env.getOrDefault("GITHUB_MCP_AUTH_SCHEME", "Bearer");
        var toolToCall = env.get("MCP_TOOL");
        var toolArgsJson = env.get("MCP_TOOL_ARGS_JSON");

        McpSyncHttpClientRequestCustomizer requestCustomizer = (builder, method, uri, body, context) -> {
            builder.header("Authorization", authScheme + " " + pat);
            builder.header("Content-Type", "application/json");
            builder.header("Accept", "application/json, text/event-stream");
            builder.header("User-Agent", userAgent);
            builder.header("Origin", "https://github.com");
        };

        var transport = HttpClientStreamableHttpTransport.builder(serverBase)
            .endpoint(endpoint)
            .connectTimeout(DEFAULT_TIMEOUT)
            .resumableStreams(false)
            .openConnectionOnStartup(false)
            .customizeClient(builder -> builder.version(java.net.http.HttpClient.Version.HTTP_1_1))
            .httpRequestCustomizer(requestCustomizer)
            .build();

        try (McpSyncClient client = McpClient.sync(transport)
            .requestTimeout(DEFAULT_TIMEOUT)
            .initializationTimeout(DEFAULT_TIMEOUT)
            .build()) {
            System.out.printf("Connecting to GitHub MCP server at %s%s%n", serverBase, endpoint);

            var initializeResult = client.initialize();
            logInitializeResponse(initializeResult);
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
        catch (McpTransportException ex) {
            logTransportError(ex);
            throw ex;
        }
    }

    private static void logInitializeResponse(McpSchema.InitializeResult initializeResult) {
        if (initializeResult == null) {
            System.out.println("Initialize response: (null)");
            return;
        }

        try {
            var json = McpJsonMapper.getDefault().writeValueAsString(initializeResult);
            System.out.printf("Initialize response: %s%n", json);
        }
        catch (IOException ex) {
            System.out.printf("Initialize response (toString): %s%n", initializeResult);
        }
    }

    private static void logTransportError(McpTransportException ex) {
        System.err.printf("GitHub MCP transport error: %s%n", ex.getMessage());
    }

    private static String resolvePersonalAccessToken(java.util.Map<String, String> env) {
        var explicit = env.get("GITHUB_MCP_PAT");
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }

        var pat = env.get("GITHUB_PAT");
        if (pat != null && !pat.isBlank()) {
            return pat.trim();
        }

        var token = env.get("GITHUB_TOKEN");
        if (token != null && !token.isBlank()) {
            return token.trim();
        }

        return null;
    }
}
