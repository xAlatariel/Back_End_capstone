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

    // Registration rate limiting: 20 attempts per 15 minutes per IP (PIÙ PERMISSIVO per dev)
    public boolean isRegistrationAllowed(String clientIp) {
        String key = "registration:" + clientIp;
        return getBucket(key, 20, Duration.ofMinutes(15)).tryConsume(1);
    }

    // Login rate limiting: 50 attempts per hour per IP (MOLTO PERMISSIVO per dev/test)
    public boolean isLoginAllowed(String clientIp) {
        String key = "login:" + clientIp;
        return getBucket(key, 50, Duration.ofHours(1)).tryConsume(1);
    }

    // Email verification resend: 10 attempts per hour per IP (aumentato)
    public boolean isEmailResendAllowed(String clientIp) {
        String key = "email_resend:" + clientIp;
        return getBucket(key, 10, Duration.ofHours(1)).tryConsume(1);
    }

    // General API rate limiting: 500 requests per minute per IP (MOLTO PERMISSIVO)
    public boolean isApiCallAllowed(String clientIp) {
        String key = "api:" + clientIp;
        return getBucket(key, 500, Duration.ofMinutes(1)).tryConsume(1);
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
        if (buckets.size() > 10000) {
            buckets.clear();
            log.info("Cleared rate limiting buckets cache");
        }
    }

    // METODO PER RESET MANUALE DURANTE DEVELOPMENT
    public void resetRateLimitForIp(String clientIp) {
        buckets.entrySet().removeIf(entry -> entry.getKey().contains(clientIp));
        log.info("Rate limit reset per IP: {}", clientIp);
    }

    // METODO PER VERIFICARE LO STATO CORRENTE
    public long getRemainingTokens(String clientIp, String operation) {
        String key = operation + ":" + clientIp;
        Bucket bucket = buckets.get(key);
        return bucket != null ? bucket.getAvailableTokens() : -1;
    }
}