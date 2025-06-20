package com.example.Capstone.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JWTAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Autowired
    public SecurityConfig(JWTAuthenticationFilter jwtAuthFilter, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CONFIGURAZIONE HEADERS DI SICUREZZA SEMPLIFICATA PER DEBUG
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .contentTypeOptions(contentTypeOptions -> {})
                )

                // CONFIGURAZIONE AUTORIZZAZIONI
                .authorizeHttpRequests(auth -> auth
                        // Endpoint pubblici esistenti
                        .requestMatchers(
                                "/api/users/register",
                                "/api/users/login",
                                "/api/users/verify-email",
                                "/api/users/resend-verification",
                                "/api/users/account-status",
                                "/api/debug/**",
                                "/api/health",
                                "/api/info",
                                "/error"
                        ).permitAll()

                        // NUOVI ENDPOINT MENU PUBBLICI
                        .requestMatchers(
                                "/api/menus/active",
                                "/api/menus/daily/today",
                                "/api/menus/seasonal/current"
                        ).permitAll()

                        // Actuator endpoints (solo health)
                        .requestMatchers("/actuator/health").permitAll()

                        // Swagger/OpenAPI (solo per development)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ADMIN ENDPOINTS
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/reservations/date/**").hasRole("ADMIN")
                        .requestMatchers("/api/menus/**").hasRole("ADMIN") // NUOVO: solo admin può gestire menu

                        // User endpoints protetti
                        .requestMatchers("/api/reservations").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/users/**").authenticated()

                        // Tutti gli altri endpoint richiedono autenticazione
                        .anyRequest().authenticated()
                )

                // Exception handling
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // JWT Filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // CONFIGURAZIONE CORS PIÙ PERMISSIVA PER DEVELOPMENT
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // Oppure specifica origin esatti (decommentare se serve più sicurezza)
        /*
        List<String> allowedOrigins = Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:3000"
        );
        configuration.setAllowedOrigins(allowedOrigins);
        */

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Cambiato da /api/** a /**
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}