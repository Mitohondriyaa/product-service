package io.github.mitohondriyaa.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisCacheService {
    private final RedisTemplate<String, Object> redisCacheTemplate;

    public void setValue(String key, Object value) {
        redisCacheTemplate.opsForValue().set(key, value, Duration.ofMinutes(10));
    }

    public Object getValue(String key) {
        return redisCacheTemplate.opsForValue().get(key);
    }
}