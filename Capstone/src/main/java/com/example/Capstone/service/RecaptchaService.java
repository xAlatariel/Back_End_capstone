package com.example.Capstone.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecaptchaService {

    private final WebClient.Builder webClientBuilder;

    @Value("${recaptcha.secret.key:}")
    private String secretKey;

    @Value("${recaptcha.verify.url:https://www.google.com/recaptcha/api/siteverify}")
    private String verifyUrl;

    public boolean verifyRecaptcha(String recaptchaResponse) {
        if (recaptchaResponse == null || recaptchaResponse.trim().isEmpty()) {
            log.warn("reCAPTCHA response is empty");
            return false;
        }

        if (secretKey == null || secretKey.trim().isEmpty()) {
            log.warn("reCAPTCHA secret key not configured, skipping verification");
            return true; // In development, skip reCAPTCHA
        }

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("secret", secretKey);
            formData.add("response", recaptchaResponse);

            WebClient webClient = webClientBuilder.build();

            Map<String, Object> response = webClient.post()
                    .uri(verifyUrl)
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                Boolean success = (Boolean) response.get("success");
                Double score = (Double) response.get("score");

                if (success != null && success) {
                    // For reCAPTCHA v3, check score (0.0 to 1.0, higher is better)
                    if (score != null) {
                        boolean scoreAcceptable = score >= 0.5; // Adjust threshold as needed
                        log.info("reCAPTCHA verification: success={}, score={}, acceptable={}",
                                success, score, scoreAcceptable);
                        return scoreAcceptable;
                    } else {
                        // For reCAPTCHA v2, just check success
                        log.info("reCAPTCHA v2 verification successful");
                        return true;
                    }
                } else {
                    log.warn("reCAPTCHA verification failed: {}", response.get("error-codes"));
                    return false;
                }
            }

            log.error("No response from reCAPTCHA service");
            return false;

        } catch (Exception e) {
            log.error("Error verifying reCAPTCHA: {}", e.getMessage());
            return false;
        }
    }

    public boolean isRecaptchaEnabled() {
        return secretKey != null && !secretKey.trim().isEmpty();
    }
}