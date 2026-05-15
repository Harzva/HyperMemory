package com.example.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class BotIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(BotIdempotencyService.class);
    private static final String KEY_PREFIX = "bot:idempotency:";

    private final StringRedisTemplate redisTemplate;

    @Value("${bot.idempotency.enabled:true}")
    private boolean enabled;

    @Value("${bot.idempotency.ttl-seconds:600}")
    private long ttlSeconds;

    public BotIdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns true when processing may continue. Returns false for a duplicate or in-flight message.
     * Fails open on Redis errors so the bot remains available.
     */
    public boolean acquire(String tenantId, String channel, String messageId) {
        if (!enabled) {
            return true;
        }
        String key = buildKey(tenantId, channel, messageId);
        long effectiveTtl = Math.max(ttlSeconds, 1);
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", effectiveTtl, TimeUnit.SECONDS);
            if (acquired == null) {
                log.warn("Redis returned null for idempotency check, failing open");
                return true;
            }
            return acquired;
        } catch (Exception e) {
            log.warn("Idempotency check failed, failing open: {}", e.getMessage());
            return true;
        }
    }

    public void release(String tenantId, String channel, String messageId) {
        if (!enabled) {
            return;
        }
        try {
            redisTemplate.delete(buildKey(tenantId, channel, messageId));
        } catch (Exception e) {
            log.warn("Idempotency release failed, continuing: {}", e.getMessage());
        }
    }

    private String buildKey(String tenantId, String channel, String messageId) {
        String safeTenant = (tenantId == null || tenantId.trim().isEmpty())
                ? "default" : tenantId.trim().toLowerCase(Locale.ROOT);
        String safeChannel = channel == null ? "unknown" : channel.trim().toLowerCase(Locale.ROOT);
        String safeMessageId = messageId == null ? "" : messageId.trim();
        return KEY_PREFIX + safeTenant + ":" + safeChannel + ":" + safeMessageId;
    }
}
