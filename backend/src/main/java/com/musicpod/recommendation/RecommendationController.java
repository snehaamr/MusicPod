package com.musicpod.recommendation;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.musicpod.auth.CurrentUserProvider;

@RestController
@RequestMapping(
        "/api/v1/me/recommendations"
)
public class RecommendationController {

    private final RecommendationService
            recommendationService;

    private final CurrentUserProvider
            currentUserProvider;

    public RecommendationController(
            RecommendationService recommendationService,
            CurrentUserProvider currentUserProvider) {

        this.recommendationService =
                recommendationService;

        this.currentUserProvider =
                currentUserProvider;
    }

    @GetMapping
    public List<RecommendationResponse>
            getRecommendations(

                    @RequestParam(
                            required = false
                    )
                    Integer size) {

        UUID userId =
                currentUserProvider.userId();

        return recommendationService
                .getRecommendations(
                        userId,
                        size
                );
    }
}