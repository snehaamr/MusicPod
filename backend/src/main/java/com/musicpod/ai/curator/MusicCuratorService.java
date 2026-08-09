package com.musicpod.ai.curator;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.UUID;

import com.musicpod.ai.audit.AgentRunService;
import com.musicpod.auth.CurrentUserProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.ToolCallback;

import com.musicpod.ai.audit.AgentExecutionContext;
import com.musicpod.ai.audit.AuditedToolCallback;
import com.musicpod.ai.audit.AuditedToolsFactory;

@Service
@ConditionalOnProperty(
        name = "app.ai.curator.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MusicCuratorService {

    private static final String SYSTEM_PROMPT = """
            You are MusicPod's AI Playlist Curator.

            MusicPod tools are the source of truth.

            CATALOG RULES:

            1. Use MusicPod tools before recommending tracks.

            2. Never invent artists, albums, tracks, track IDs,
               playlist IDs, likes, or listening history.

            3. Only recommend tracks returned by MusicPod tools.

            4. Respect explicit artist constraints.

            5. When multiple specific artists are requested, search
               each artist separately when necessary.

            6. If the user specifies a duration constraint, use the
               duration tool to verify the chosen tracks.

            7. If MusicPod does not contain enough matching tracks,
               say so instead of inventing music.

            8. Do not quote song lyrics.

            PERSONALIZATION RULES:

            9. When the user asks for recommendations based on their
               taste, preferences, favorites, likes, or listening
               habits, use personalized MusicPod tools.

            10. Liked tracks and recently played tracks belong to the
                authenticated MusicPod user. Never ask for or invent
                another user ID.

            11. Personalization is supporting context. Explicit user
                constraints such as requested artists, duration, or
                mood still take priority.

            WRITE SAFETY RULES:

            12. Playlist write tools may or may not be available for
                a request. MusicPod controls their availability.

            13. Only call a playlist write tool when the user
                explicitly asks to create, save, or modify a
                playlist.

            14. A request to recommend, suggest, design, generate,
                or show a playlist does NOT by itself authorize a
                persistent write.

            15. If write tools are unavailable, do not claim that a
                playlist was created or modified.

            16. Before writing a playlist, discover and select real
                MusicPod tracks first.

            17. Track IDs used for writes must come from MusicPod
                tools.

            18. When a playlist is successfully created and tracks
                are added, clearly tell the user that it was saved
                and summarize what was added.

            19. Never delete playlists or remove tracks. Destructive
                capabilities are not available to you.
            """;

    private final ChatClient chatClient;

    
    private final CurrentUserProvider currentUserProvider;

    private final AgentRunService agentRunService;
    
    private final CuratorToolProvider toolProvider;

    public MusicCuratorService(
            ChatClient.Builder chatClientBuilder,
            CurrentUserProvider currentUserProvider,
            AgentRunService agentRunService,
            CuratorToolProvider toolProvider) {

        this.chatClient =
                chatClientBuilder
                        .defaultSystem(
                                SYSTEM_PROMPT
                        )
                        .build();

        this.currentUserProvider =
                currentUserProvider;

        this.agentRunService =
                agentRunService;

        this.toolProvider =
                toolProvider;
    }

    public CuratorResponse curate(
            CuratorRequest request) {

        UUID userId =
                currentUserProvider.userId();

        UUID runId =
                agentRunService.start(
                        userId,
                        request.prompt(),
                        request.allowWrite()
                );

        try {

            AgentExecutionContext executionContext =
                    new AgentExecutionContext(
                            runId
                    );

            String content =
                    executeCurator(
                            request,
                            executionContext
                    );

            if (content == null
                    || content.isBlank()) {

                throw new IllegalStateException(
                        "AI curator returned an empty response"
                );
            }

            agentRunService.complete(
                    runId,
                    content
            );

            return new CuratorResponse(
                    content
            );

        } catch (Exception exception) {

            agentRunService.fail(
                    runId,
                    exception
            );

            throw exception;
        }
    }
    
    private String executeCurator(
            CuratorRequest request,
            AgentExecutionContext executionContext) {

        List<ToolCallback> tools =
                toolProvider.toolsFor(
                        request.allowWrite()
                );

        return chatClient
                .prompt()
                .user(
                        request.prompt()
                )
                .tools(
                        tools
                )
                .toolContext(
                        Map.of(
                                AuditedToolCallback
                                        .AGENT_EXECUTION_CONTEXT_KEY,
                                executionContext
                        )
                )
                .call()
                .content();
    }
    
}