package com.musicpod.search.track;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SearchControllerTests {

    @Mock
    private TrackSearchService trackSearchService;

    @Mock
    private SemanticTrackSearchService semanticTrackSearchService;

    @Mock
    private HybridTrackSearchService hybridTrackSearchService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        SearchController controller =
                new SearchController(
                        trackSearchService,
                        semanticTrackSearchService,
                        hybridTrackSearchService
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .build();
    }

    @Test
    void lexicalSearchUsesDefaultSize()
            throws Exception {

        when(
                trackSearchService.search(
                        "queen",
                        20
                )
        ).thenReturn(
                List.of()
        );

        mockMvc.perform(
                        get("/api/v1/search")
                                .param(
                                        "q",
                                        "queen"
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                trackSearchService
        ).search(
                "queen",
                20
        );
    }

    @Test
    void lexicalSearchUsesRequestedSize()
            throws Exception {

        when(
                trackSearchService.search(
                        "queen",
                        5
                )
        ).thenReturn(
                List.of()
        );

        mockMvc.perform(
                        get("/api/v1/search")
                                .param(
                                        "q",
                                        "queen"
                                )
                                .param(
                                        "size",
                                        "5"
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                trackSearchService
        ).search(
                "queen",
                5
        );
    }

    @Test
    void semanticEndpointDelegatesToSemanticSearch()
            throws Exception {

        when(
                semanticTrackSearchService.search(
                        "romantic songs",
                        20
                )
        ).thenReturn(
                List.of()
        );

        mockMvc.perform(
                        get(
                                "/api/v1/search/semantic"
                        )
                                .param(
                                        "q",
                                        "romantic songs"
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                semanticTrackSearchService
        ).search(
                "romantic songs",
                20
        );
    }

    @Test
    void hybridEndpointDelegatesToHybridSearch()
            throws Exception {

        when(
                hybridTrackSearchService.search(
                        "songs by Queen",
                        10
                )
        ).thenReturn(
                List.of()
        );

        mockMvc.perform(
                        get(
                                "/api/v1/search/hybrid"
                        )
                                .param(
                                        "q",
                                        "songs by Queen"
                                )
                                .param(
                                        "size",
                                        "10"
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                hybridTrackSearchService
        ).search(
                "songs by Queen",
                10
        );
    }

    @Test
    void searchRequiresQueryParameter()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/search"
                        )
                )
                .andExpect(
                        status().isBadRequest()
                );
    }
}