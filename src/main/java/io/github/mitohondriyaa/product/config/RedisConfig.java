package io.github.mitohondriyaa.product.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisConnectionFactory redisCacheConnectionFactory() {
        return new LettuceConnectionFactory("localhost", 6379);
    }

    @Bean
    public RedisConnectionFactory redisCounterConnectionFactory() {
        return new LettuceConnectionFactory("localhost", 6380);
    }

    @Bean
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