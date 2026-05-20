package com.testrank.hcf.core.redis;

public record RedisEnvelope(String sourceServer, String payload) {
    public static RedisEnvelope parse(String raw) {
        int split = raw.indexOf('|');
        if (split == -1) {
            return new RedisEnvelope("unknown", raw);
        }
        return new RedisEnvelope(raw.substring(0, split), raw.substring(split + 1));
    }
}
