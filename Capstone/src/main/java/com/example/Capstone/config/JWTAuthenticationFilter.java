package com.example.Capstone.config;

import com.example.Capstone.entity.User;
import com.example.Capstone.repository.UserRepository;
import com.example.Capstone.utils.JWTTools;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final JWTTools jwtTools;
    private final UserRepository userRepository;

    @Autowired
    public JWTAuthenticationFilter(JWTTools jwtTools, UserRepository userRepository) {
        this.jwtTools = jwtTools;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Gestione OPTIONS per CORS
        if (request.getMethod().equals("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();

        // Skip JWT validation per endpoint pubblici
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        String userEmail = null;

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);

                // Validazione formato token
                if (token.trim().isEmpty()) {
                    sendError(response, "Token JWT vuoto");
                    return;
                }

                userEmail = jwtTools.extractEmail(token);

                if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    User user = userRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new RuntimeException("Utente non trovato"));

                    // Controlli di sicurezza aggiuntivi
                    if (!user.getEnabled()) {
                        sendError(response, "Account disabilitato");
                        return;
                    }

                    if (!user.getEmailVerified()) {
                        sendError(response, "Email non verificata");
                        return;
                    }

                    if (user.isAccountLocked()) {
                        sendError(response, "Account temporaneamente bloccato");
                        return;
                    }

                    if (jwtTools.validateToken(token, userEmail)) {
                        // Creazione del contesto di sicurezza
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRuolo().name());
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                Collections.singletonList(authority)
                        );

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        log.debug("Utente autenticato: {} con ruolo: {}", userEmail, user.getRuolo());
                    }
                }
            } else if (!isPublicEndpoint(requestURI)) {
                // Token mancante per endpoint protetti
                log.warn("Tentativo di accesso senza token a endpoint protetto: {}", requestURI);
                sendError(response, "Token di autorizzazione richiesto");
                return;
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.warn("Token JWT scaduto per utente: {}", userEmail);
            sendError(response, "Token JWT scaduto");
        } catch (MalformedJwtException e) {
            log.warn("Token JWT malformato da IP: {}", getClientIpAddress(request));
            sendError(response, "Token JWT non valido");
        } catch (JwtException e) {
            log.warn("Errore JWT: {}", e.getMessage());
            sendError(response, "Errore token JWT: " + e.getMessage());
        } catch (Exception e) {
            log.error("Errore di autenticazione: {}", e.getMessage(), e);
            sendError(response, "Errore di autenticazione");
        }
    }

    private boolean isPublicEndpoint(String uri) {
        List<String> publicEndpoints = List.of(
                "/api/users/register",
                "/api/users/login",
                "/api/users/verify-email",
                "/api/users/resend-verification",
                "/api/users/account-status",
                "/api/health",
                "/api/info",
                "/swagger-ui",
                "/v3/api-docs",
                "/actuator/health"
        );

        return publicEndpoints.stream().anyMatch(uri::startsWith);
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}",
                java.time.LocalDateTime.now().toString(),
                message
        );

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    private String getClientIpAddress(HttpServletRequest request) {
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
}