package com.musicpod.ai.curator;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "spring.ai.model.chat",
        havingValue = "openai"
)
public class MusicCuratorService {

    private static final String SYSTEM_PROMPT = """
            You are MusicPod's AI Playlist Curator.

            You help users discover music that actually exists
            in the MusicPod catalog.

            RULES:

            1. You must use MusicPod tools before recommending tracks.

            2. Never invent an artist, album, track, or track ID.

            3. Only recommend tracks returned by MusicPod tools.

            4. If the user names a specific artist, respect that
               artist constraint.

            5. If multiple artists are requested, search for each
               requested artist separately.

            6. When a user specifies a duration constraint, use the
               duration tool to verify the proposed playlist duration.

            7. If MusicPod does not contain enough tracks to satisfy
               the request, say so clearly instead of inventing tracks.

            8. Prefer a concise playlist explanation followed by the
               selected tracks.

            9. Do not claim that a playlist was created. You currently
               have recommendation tools only and cannot mutate playlists.

            10. Do not quote song lyrics.

            You are operating on MusicPod's catalog, not your general
            knowledge of music. MusicPod tool results are the source
            of truth.
            """;

    private final ChatClient chatClient;
    private final MusicCuratorTools curatorTools;

    public MusicCuratorService(
            ChatClient.Builder chatClientBuilder,
            MusicCuratorTools curatorTools) {

        this.chatClient =
                chatClientBuilder
                        .defaultSystem(
                                SYSTEM_PROMPT
                        )
                        .build();

        this.curatorTools =
                curatorTools;
    }

    public CuratorResponse curate(
            String prompt) {

        String content =
                chatClient
                        .prompt()
                        .user(prompt)
                        .tools(
                                curatorTools
                        )
                        .call()
                        .content();

        if (content == null
                || content.isBlank()) {

            throw new IllegalStateException(
                    "AI curator returned an empty response"
            );
        }

        return new CuratorResponse(
                content
        );
    }
}