package com.musicpod.messaging.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@ConditionalOnProperty(
        name = "app.kafka.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KafkaTopicConfig {

    @Bean
    KafkaAdmin kafkaAdmin(
            @Value(
                    "${spring.kafka.bootstrap-servers:localhost:9092}"
            )
            String bootstrapServers) {

        Map<String, Object> configuration =
                new HashMap<>();

        configuration.put(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        KafkaAdmin kafkaAdmin =
                new KafkaAdmin(configuration);

        /*
         * If Kafka is enabled but unavailable,
         * fail startup instead of silently starting
         * without the required topic.
         */
        kafkaAdmin.setFatalIfBrokerNotAvailable(true);

        return kafkaAdmin;
    }

    @Bean
    NewTopic playbackRecordedTopic() {

        return TopicBuilder
                .name(
                        KafkaTopics.PLAYBACK_RECORDED
                )
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    NewTopic trackSearchChangedTopic() {

        return TopicBuilder
                .name(
                        KafkaTopics.TRACK_SEARCH_CHANGED
                )
                .partitions(3)
                .replicas(1)
                .build();
    }
}