package com.example.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class BotRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(BotRateLimitService.class);
    private static final String KEY_PREFIX = "bot:rate-limit:";

    private final StringRedisTemplate redisTemplate;

    @Value("${bot.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${bot.rate-limit.max-per-minute:60}")
    private long maxPerMinute;

    @Value("${bot.rate-limit.window-seconds:60}")
    private long windowSeconds;

    public BotRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns true when the request is within the rate limit. Returns false when the limit is exceeded.
     * Fails open on Redis errors so the bot remains available.
     */
    public boolean isAllowed(String tenantId, String channel) {
        if (!enabled) {
            return true;
        }
        long effectiveMax = Math.max(maxPerMinute, 1);
        long effectiveWindow = Math.max(windowSeconds, 1);
        String key = buildKey(tenantId, channel, effectiveWindow);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                log.warn("Redis returned null for rate limit check, failing open");
                return true;
            }
            if (count == 1) {
                redisTemplate.expire(key, effectiveWindow, TimeUnit.SECONDS);
            }
            return count <= effectiveMax;
        } catch (Exception e) {
            log.warn("Rate limit check failed, failing open: {}", e.getMessage());
            return true;
        }
    }

    private String buildKey(String tenantId, String channel, long windowSeconds) {
        String safeTenant = (tenantId == null || tenantId.trim().isEmpty())
                ? "default" : tenantId.trim().toLowerCase(Locale.ROOT);
        String safeChannel = channel == null ? "unknown" : channel.trim().toLowerCase(Locale.ROOT);
        long bucket = System.currentTimeMillis() / (windowSeconds * 1000);
        return KEY_PREFIX + safeTenant + ":" + safeChannel + ":" + bucket;
    }
}
