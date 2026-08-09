package com.musicpod.search.track;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "/api/v1/admin/search"
)
public class SearchAdminController {

    private final TrackSearchIndexer
            trackSearchIndexer;

    public SearchAdminController(
            TrackSearchIndexer trackSearchIndexer) {

        this.trackSearchIndexer =
                trackSearchIndexer;
    }

    @PostMapping("/backfill")
    public ResponseEntity<SearchBackfillResponse>
            backfill() {

        SearchBackfillResponse response =
                trackSearchIndexer.backfill();

        return ResponseEntity.ok(
                response
        );
    }
}