package io.github.mitohondriyaa.product.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@TestConfiguration
public class TestRedisConfig {
    @Value("${redis.cache.port}")
    private int redisCachePort;
    @Value("${redis.counter.port}")
    private int redisCounterPort;

    @Bean
    public RedisConnectionFactory redisCacheConnectionFactory() {
        return new LettuceConnectionFactory("localhost", redisCachePort);
    }

    @Bean
    public RedisConnectionFactory redisCounterConnectionFactory() {
        return new LettuceConnectionFactory("localhost", redisCounterPort);
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