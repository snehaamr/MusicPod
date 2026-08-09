package com.musicpod.search.config;

import org.apache.hc.core5.http.HttpHost;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.musicpod.search.config.OpenSearchProperties;

@Configuration
@EnableConfigurationProperties(
        OpenSearchProperties.class
)
public class OpenSearchConfig {

    @Bean(destroyMethod = "close")
    public OpenSearchTransport openSearchTransport(
            OpenSearchProperties properties) {

        HttpHost host =
                new HttpHost(
                        properties.scheme(),
                        properties.host(),
                        properties.port()
                );

        return ApacheHttpClient5TransportBuilder
                .builder(host)
                .build();
    }

    @Bean
    public OpenSearchClient openSearchClient(
            OpenSearchTransport transport) {

        return new OpenSearchClient(
                transport
        );
    }
}