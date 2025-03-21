package com.example.Capstone.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Applica CORS a tutti gli endpoint
                .allowedOrigins("http://localhost:5173") // Porta corretta del frontend
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Metodi consentiti
                .allowedHeaders("*") // Consenti tutti gli headers
                .allowCredentials(true); // Consenti l'invio di credenziali (cookie, token)
    }
}