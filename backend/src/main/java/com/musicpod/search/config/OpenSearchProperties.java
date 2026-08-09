package com.musicpod.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "app.opensearch"
)
public record OpenSearchProperties(
        String host,
        int port,
        String scheme
) {
}