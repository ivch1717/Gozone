package com.example.payments.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {
    @Bean
    public NewTopic paymentResultsTopic() {
        return TopicBuilder.name("payment-results")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
