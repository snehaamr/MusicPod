package com.musicpod.ai.curator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import com.musicpod.ai.audit.AuditedToolsFactory;

@Component
public class CuratorToolProvider {

    private final MusicCuratorTools curatorTools;
    private final PersonalizedCuratorTools personalizedCuratorTools;
    private final MusicCuratorWriteTools writeTools;
    private final AuditedToolsFactory auditedToolsFactory;

    public CuratorToolProvider(
            MusicCuratorTools curatorTools,
            PersonalizedCuratorTools personalizedCuratorTools,
            MusicCuratorWriteTools writeTools,
            AuditedToolsFactory auditedToolsFactory) {

        this.curatorTools =
                curatorTools;

        this.personalizedCuratorTools =
                personalizedCuratorTools;

        this.writeTools =
                writeTools;

        this.auditedToolsFactory =
                auditedToolsFactory;
    }

    public List<ToolCallback> toolsFor(
            boolean allowWrite) {

        List<ToolCallback> tools =
                new ArrayList<>();

        tools.addAll(
                auditedToolsFactory.create(
                        curatorTools,
                        CuratorToolRisk.READ_ONLY
                )
        );

        tools.addAll(
                auditedToolsFactory.create(
                        personalizedCuratorTools,
                        CuratorToolRisk.READ_ONLY
                )
        );

        if (allowWrite) {

            tools.addAll(
                    auditedToolsFactory.create(
                            writeTools,
                            CuratorToolRisk.USER_WRITE
                    )
            );
        }

        return List.copyOf(
                tools
        );
    }
}