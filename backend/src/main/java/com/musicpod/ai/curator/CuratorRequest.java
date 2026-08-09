package com.musicpod.ai.curator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CuratorRequest(

        @NotBlank(
                message = "Prompt is required"
        )
        @Size(
                max = 1000,
                message = "Prompt must not exceed 1000 characters"
        )
        String prompt,

        boolean allowWrite
) {
}