package io.github.mitohondriyaa.product.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class KafkaConfig {
    @Bean
    public NewTopic productCreatedTopic() {
        return new NewTopic("product-created", 3, (short) 2);
    }

    @Bean
    public NewTopic productDeletedTopic() {
        return new NewTopic("product-deleted", 3, (short) 2);
    }
}