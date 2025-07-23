package io.github.mitohondriyaa.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisCounterService {
    private final StringRedisTemplate redisCounterStringRedisTemplate;

    public void increment(String key) {
        redisCounterStringRedisTemplate.opsForValue().increment(key);
    }
}