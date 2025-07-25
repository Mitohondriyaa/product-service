package io.github.mitohondriyaa.product.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic productCreatedTopic() {
        return new NewTopic("product-created", 3, (short) 2);
    }
}