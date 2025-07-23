package io.github.mitohondriyaa.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisCacheService {
    private final RedisTemplate<String, Object> redisCacheRedisTemplate;
    @Value("${cache.ttl.minutes}")
    private Integer cacheTtlMinutes;

    public void setValue(String key, Object value) {
        redisCacheRedisTemplate.opsForValue().set(key, value, Duration.ofMinutes(cacheTtlMinutes));
    }

    public Object getValue(String key) {
        return redisCacheRedisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisCacheRedisTemplate.delete(key);
    }
}