package com.musicpod.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;
import java.time.Instant;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import org.springframework.test.context.ActiveProfiles;

import com.musicpod.auth.JwtProperties;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;

import io.modelcontextprotocol.client.transport
        .HttpClientStreamableHttpTransport;

import io.modelcontextprotocol.spec.McpSchema;

@SpringBootTest(
        webEnvironment =
                SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("mcp-test")
class MusicPodMcpIntegrationTests {

    private static final Duration
            REQUEST_TIMEOUT =
            Duration.ofSeconds(10);

    private static final Set<String>
            EXPECTED_TOOLS =
            		Set.of(
            		        "musicpod_search_tracks",
            		        "musicpod_get_track",
            		        "musicpod_get_liked_tracks",
            		        "musicpod_get_recently_played",
            		        "musicpod_create_playlist",
            		        "musicpod_add_tracks_to_playlist",
            		        "musicpod_get_recommendations"
            		);

    private static final Set<String>
            EXPECTED_RESOURCE_TEMPLATES =
            Set.of(
                    "musicpod://tracks/{trackId}",
                    "musicpod://me/playlists/{playlistId}"
            );

    @LocalServerPort
    private int port;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    /*
     * This test deliberately does not use the
     * MCP Java client.
     *
     * We want to verify the HTTP security
     * boundary itself.
     *
     * Spring Security should reject /mcp
     * before the MCP protocol request is
     * processed.
     */
    @Test
    void unauthenticatedMcpRequestIsRejected()
            throws Exception {

        HttpClient httpClient =
                HttpClient.newHttpClient();

        HttpRequest request =
                HttpRequest
                        .newBuilder()
                        .uri(
                                URI.create(
                                        baseUrl()
                                                + "/mcp"
                                )
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .header(
                                "Accept",
                                "application/json, "
                                        + "text/event-stream"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                initializeRequest()
                                        )
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse
                                .BodyHandlers
                                .ofString()
                );

        assertEquals(
                401,
                response.statusCode()
        );
    }

    /*
     * This verifies the complete MCP
     * initialization handshake:
     *
     * client
     *   -> HTTP
     *   -> Spring Security
     *   -> MCP server
     *   -> initialize response
     */
    @Test
    void authenticatedClientCanInitialize() {

        try (
                McpSyncClient client =
                        createAuthenticatedClient()
        ) {

            McpSchema.InitializeResult result =
                    client.initialize();

            assertNotNull(
                    result
            );

            assertTrue(
                    client.isInitialized()
            );

            assertNotNull(
                    result.protocolVersion()
            );

            assertNotNull(
                    result.capabilities()
            );

            assertNotNull(
                    result.serverInfo()
            );

            assertEquals(
                    "musicpod",
                    result.serverInfo()
                            .name()
            );

            assertEquals(
                    "1.0.0",
                    result.serverInfo()
                            .version()
            );
        }
    }

    /*
     * We deliberately verify the exact tool set.
     *
     * If a tool accidentally disappears,
     * or an unexpected tool is exposed,
     * this test should fail.
     */
    @Test
    void exposesExpectedTools() {

        try (
                McpSyncClient client =
                        createAuthenticatedClient()
        ) {

            client.initialize();

            McpSchema.ListToolsResult result =
                    client.listTools();

            assertNotNull(
                    result
            );

            assertNotNull(
                    result.tools()
            );

            Set<String> toolNames =
                    result
                            .tools()
                            .stream()
                            .map(
                                    McpSchema.Tool::name
                            )
                            .collect(
                                    Collectors.toSet()
                            );

            assertEquals(
                    7,
                    result.tools().size()
            );

            assertEquals(
                    EXPECTED_TOOLS,
                    toolNames
            );
        }
    }

    /*
     * Same idea as tools/list, but for
     * resources/templates/list.
     *
     * We expect exactly two parameterized
     * MusicPod resources.
     */
    @Test
    void exposesExpectedResourceTemplates() {

        try (
                McpSyncClient client =
                        createAuthenticatedClient()
        ) {

            client.initialize();

            McpSchema.ListResourceTemplatesResult
                    result =
                    client.listResourceTemplates();

            assertNotNull(
                    result
            );

            assertNotNull(
                    result.resourceTemplates()
            );

            Set<String> resourceUris =
                    result
                            .resourceTemplates()
                            .stream()
                            .map(
                                    McpSchema
                                            .ResourceTemplate
                                            ::uriTemplate
                            )
                            .collect(
                                    Collectors.toSet()
                            );

            assertEquals(
                    2,
                    result
                            .resourceTemplates()
                            .size()
            );

            assertEquals(
                    EXPECTED_RESOURCE_TEMPLATES,
                    resourceUris
            );
        }
    }

    /*
     * Build a real MCP Java client.
     *
     * The Authorization header is placed
     * on the base HttpRequest.Builder.
     *
     * The Streamable HTTP transport copies
     * this builder for initialize, POST,
     * GET/SSE, and session requests.
     */
    private McpSyncClient
            createAuthenticatedClient() {

        String token =
                createAccessToken();

        HttpRequest.Builder requestBuilder =
                HttpRequest
                        .newBuilder()
                        .header(
                                "Authorization",
                                "Bearer " + token
                        );

        HttpClientStreamableHttpTransport
                transport =
                HttpClientStreamableHttpTransport
                        .builder(
                                baseUrl()
                        )
                        .endpoint(
                                "/mcp"
                        )
                        .requestBuilder(
                                requestBuilder
                        )
                        .connectTimeout(
                                Duration.ofSeconds(5)
                        )
                        .resumableStreams(
                                false
                        )
                        .build();

        return McpClient
                .sync(
                        transport
                )
                .requestTimeout(
                        REQUEST_TIMEOUT
                )
                .initializationTimeout(
                        REQUEST_TIMEOUT
                )
                .build();
    }

    /*
     * We intentionally generate the token
     * directly rather than:
     *
     * register user
     *     ->
     * login
     *     ->
     * parse access token
     *
     * These tests are checking MCP protocol
     * integration, not AuthService.
     */
    private String createAccessToken() {

        Instant now =
                Instant.now();

        JwtClaimsSet claims =
                JwtClaimsSet
                        .builder()
                        .issuer(
                                jwtProperties
                                        .issuer()
                        )
                        .issuedAt(
                                now
                        )
                        .expiresAt(
                                now.plus(
                                        jwtProperties
                                                .accessTokenTtl()
                                )
                        )
                        .subject(
                                UUID
                                        .randomUUID()
                                        .toString()
                        )
                        .claim(
                                "email",
                                "mcp-integration-test"
                                        + "@musicpod.test"
                        )
                        .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters
                                .from(
                                        claims
                                )
                )
                .getTokenValue();
    }

    private String baseUrl() {

        return "http://localhost:"
                + port;
    }

    /*
     * Valid enough initialize payload for the
     * unauthenticated HTTP security test.
     *
     * It should never reach the MCP handler
     * because Spring Security rejects it first.
     */
    private String initializeRequest() {

        return """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2025-11-25",
                    "capabilities": {},
                    "clientInfo": {
                      "name": "musicpod-integration-test",
                      "version": "1.0.0"
                    }
                  }
                }
                """;
    }
}