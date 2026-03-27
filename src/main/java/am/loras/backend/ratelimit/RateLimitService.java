package am.loras.backend.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Attempts to consume one token from the bucket associated with the given IP.
     * If no bucket exists for the IP yet, one is created with the specified capacity
     * and greedy refill period.
     *
     * @param ip               the client IP address used as bucket key
     * @param capacity         the maximum number of requests allowed per refill period
     * @param refillDuration   the duration over which the full capacity is refilled
     * @return {@code true} if a token was consumed (request is allowed),
     *         {@code false} if the bucket is empty (request should be rate-limited)
     */
    public boolean tryConsume(String ip, int capacity, Duration refillDuration) {
        Bucket bucket = buckets.computeIfAbsent(ip, key -> createBucket(capacity, refillDuration));
        return bucket.tryConsume(1);
    }

    private Bucket createBucket(int capacity, Duration refillDuration) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, refillDuration)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
