package com.musicpod.ai.curator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/ai/curator")
@ConditionalOnProperty(
        name = "spring.ai.model.chat",
        havingValue = "openai"
)
public class MusicCuratorController {

    private final MusicCuratorService musicCuratorService;

    public MusicCuratorController(
            MusicCuratorService musicCuratorService) {

        this.musicCuratorService =
                musicCuratorService;
    }

    @PostMapping
    public ResponseEntity<CuratorResponse> curate(

            @Valid
            @RequestBody
            CuratorRequest request) {

        return ResponseEntity.ok(
                musicCuratorService.curate(
                        request.prompt()
                )
        );
    }
}