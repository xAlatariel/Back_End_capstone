package com.example.Capstone.service;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Registration rate limiting: 3 attempts per 15 minutes per IP
    public boolean isRegistrationAllowed(String clientIp) {
        String key = "registration:" + clientIp;
        return getBucket(key, 3, Duration.ofMinutes(15)).tryConsume(1);
    }

    // Login rate limiting: 5 attempts per hour per IP
    public boolean isLoginAllowed(String clientIp) {
        String key = "login:" + clientIp;
        return getBucket(key, 5, Duration.ofHours(1)).tryConsume(1);
    }

    // Email verification resend: 3 attempts per hour per IP
    public boolean isEmailResendAllowed(String clientIp) {
        String key = "email_resend:" + clientIp;
        return getBucket(key, 3, Duration.ofHours(1)).tryConsume(1);
    }

    // General API rate limiting: 100 requests per minute per IP
    public boolean isApiCallAllowed(String clientIp) {
        String key = "api:" + clientIp;
        return getBucket(key, 100, Duration.ofMinutes(1)).tryConsume(1);
    }

    private Bucket getBucket(String key, int capacity, Duration refillDuration) {
        return buckets.computeIfAbsent(key, k -> createNewBucket(capacity, refillDuration));
    }

    private Bucket createNewBucket(int capacity, Duration refillDuration) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, refillDuration));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    // Cleanup old buckets periodically
    public void cleanupOldBuckets() {
        // This would be called by a scheduled task
        // For now, we keep it simple
        if (buckets.size() > 10000) {
            buckets.clear();
            log.info("Cleared rate limiting buckets cache");
        }
    }
}