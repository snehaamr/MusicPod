package com.musicpod.search.track;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final TrackSearchService
            trackSearchService;

    private final SemanticTrackSearchService
            semanticTrackSearchService;

    private final HybridTrackSearchService
            hybridTrackSearchService;

    public SearchController(
            TrackSearchService trackSearchService,
            SemanticTrackSearchService
                    semanticTrackSearchService,
            HybridTrackSearchService
                    hybridTrackSearchService) {

        this.trackSearchService =
                trackSearchService;

        this.semanticTrackSearchService =
                semanticTrackSearchService;

        this.hybridTrackSearchService =
                hybridTrackSearchService;
    }

    /*
     * Lexical / BM25 search.
     *
     * Kept for comparison and debugging.
     */
    @GetMapping
    public List<TrackSearchResult> search(
            @RequestParam("q")
            String query,

            @RequestParam(
                    defaultValue = "20"
            )
            int size) {

        return trackSearchService.search(
                query,
                size
        );
    }

    /*
     * Pure vector search.
     *
     * Kept for comparison and debugging.
     */
    @GetMapping("/semantic")
    public List<TrackSearchResult>
            semanticSearch(

            @RequestParam("q")
            String query,

            @RequestParam(
                    defaultValue = "20"
            )
            int size) {

        return semanticTrackSearchService
                .search(
                        query,
                        size
                );
    }

    /*
     * Production-style hybrid search.
     *
     * BM25 + vector search + RRF.
     */
    @GetMapping("/hybrid")
    public List<TrackSearchResult>
            hybridSearch(

            @RequestParam("q")
            String query,

            @RequestParam(
                    defaultValue = "20"
            )
            int size) {

        return hybridTrackSearchService
                .search(
                        query,
                        size
                );
    }
}