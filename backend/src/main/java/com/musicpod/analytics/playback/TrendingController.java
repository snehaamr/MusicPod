package com.musicpod.analytics.playback;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "/api/v1/trending"
)
public class TrendingController {

    private final TrendingService
            trendingService;

    public TrendingController(
            TrendingService trendingService) {

        this.trendingService =
                trendingService;
    }

    @GetMapping("/tracks")
    public List<TrendingTrackResponse>
            getTrendingTracks(

                    @RequestParam(
                            required = false
                    )
                    Integer size) {

        return trendingService
                .getTrendingTracks(
                        size
                );
    }
}