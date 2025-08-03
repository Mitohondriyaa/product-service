package io.github.mitohondriyaa.product.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("!test")
public class RedisConfig {
    @Value("${redis.cache.host}")
    private String redisCacheHost;
    @Value("${redis.cache.port}")
    private Integer redisCachePort;
    @Value("${redis.counter.host}")
    private String redisCounterHost;
    @Value("${redis.counter.port}")
    private Integer redisCounterPort;

    @Bean
    public RedisConnectionFactory redisCacheConnectionFactory() {
        return new LettuceConnectionFactory(redisCacheHost, redisCachePort);
    }

    @Bean
    public RedisConnectionFactory redisCounterConnectionFactory() {
        return new LettuceConnectionFactory(redisCounterHost, redisCounterPort);
    }

    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisCacheRedisTemplate(
        @Qualifier("redisCacheConnectionFactory")
        RedisConnectionFactory redisCacheConnectionFactory
    ){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisCacheConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate redisCounterStringRedisTemplate(
        @Qualifier("redisCounterConnectionFactory")
        RedisConnectionFactory redisCounterConnectionFactory
    ) {
        return new StringRedisTemplate(redisCounterConnectionFactory);
    }
}