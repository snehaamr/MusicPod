package com.musicpod.ai.curator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallback;

import com.musicpod.ai.audit.AuditedToolsFactory;

@ExtendWith(MockitoExtension.class)
class CuratorToolProviderTests {

    @Mock
    private MusicCuratorTools curatorTools;

    @Mock
    private PersonalizedCuratorTools personalizedCuratorTools;

    @Mock
    private MusicCuratorWriteTools writeTools;

    @Mock
    private AuditedToolsFactory auditedToolsFactory;

    @Mock
    private ToolCallback catalogReadCallback;

    @Mock
    private ToolCallback personalizedReadCallback;

    @Mock
    private ToolCallback writeCallback;

    private CuratorToolProvider toolProvider;

    @BeforeEach
    void setUp() {

        toolProvider =
                new CuratorToolProvider(
                        curatorTools,
                        personalizedCuratorTools,
                        writeTools,
                        auditedToolsFactory
                );
    }

    @Test
    void readOnlyRequestDoesNotExposeWriteTools() {

        when(
                auditedToolsFactory.create(
                        curatorTools,
                        CuratorToolRisk.READ_ONLY
                )
        ).thenReturn(
                List.of(
                        catalogReadCallback
                )
        );

        when(
                auditedToolsFactory.create(
                        personalizedCuratorTools,
                        CuratorToolRisk.READ_ONLY
                )
        ).thenReturn(
                List.of(
                        personalizedReadCallback
                )
        );

        List<ToolCallback> tools =
                toolProvider.toolsFor(
                        false
                );

        assertEquals(
                2,
                tools.size()
        );

        assertSame(
                catalogReadCallback,
                tools.get(0)
        );

        assertSame(
                personalizedReadCallback,
                tools.get(1)
        );

        verify(
                auditedToolsFactory,
                never()
        ).create(
                writeTools,
                CuratorToolRisk.USER_WRITE
        );
    }

    @Test
    void writeEnabledRequestExposesControlledWriteTools() {

        when(
                auditedToolsFactory.create(
                        curatorTools,
                        CuratorToolRisk.READ_ONLY
                )
        ).thenReturn(
                List.of(
                        catalogReadCallback
                )
        );

        when(
                auditedToolsFactory.create(
                        personalizedCuratorTools,
                        CuratorToolRisk.READ_ONLY
                )
        ).thenReturn(
                List.of(
                        personalizedReadCallback
                )
        );

        when(
                auditedToolsFactory.create(
                        writeTools,
                        CuratorToolRisk.USER_WRITE
                )
        ).thenReturn(
                List.of(
                        writeCallback
                )
        );

        List<ToolCallback> tools =
                toolProvider.toolsFor(
                        true
                );

        assertEquals(
                3,
                tools.size()
        );

        assertSame(
                catalogReadCallback,
                tools.get(0)
        );

        assertSame(
                personalizedReadCallback,
                tools.get(1)
        );

        assertSame(
                writeCallback,
                tools.get(2)
        );

        verify(
                auditedToolsFactory
        ).create(
                writeTools,
                CuratorToolRisk.USER_WRITE
        );
    }
}