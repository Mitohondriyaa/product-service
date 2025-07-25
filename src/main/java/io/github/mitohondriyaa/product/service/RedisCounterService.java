package io.github.mitohondriyaa.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RedisCounterService {
    private final StringRedisTemplate redisCounterStringRedisTemplate;
    @Value("${counter.ttl.seconds}")
    private String counterTtlSeconds;

    public Long incrementAndGet(String key) {
        String script = """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """;

        return redisCounterStringRedisTemplate.execute(
            RedisScript.of(script, Long.class),
            Collections.singletonList(key),
            counterTtlSeconds
        );
    }

    public void delete(String key) {
        redisCounterStringRedisTemplate.delete(key);
    }
}