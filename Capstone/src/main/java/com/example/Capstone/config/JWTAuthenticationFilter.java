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

        final String authHeader = request.getHeader("Authorization");

        // AGGIUNGI LOGGING PER DEBUG
        log.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());
        log.debug("Authorization header: {}", authHeader != null ? "presente" : "assente");

        // 1. Se l'header non esiste o non inizia con "Bearer ", procedi senza autenticare.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found, proceeding without authentication");
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Estrai il token e l'email dell'utente
        String token = authHeader.substring(7);
        String userEmail = null;

        try {
            userEmail = jwtTools.extractEmail(token);
            log.debug("Extracted email from token: {}", userEmail);

            // 3. Se abbiamo l'email e non c'è già un'autenticazione nel contesto di sicurezza
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new RuntimeException("Utente associato al token non trovato"));

                log.debug("User found: {}, enabled: {}", user.getEmail(), user.getEnabled());

                // 4. Valida il token e lo stato dell'utente
                if (jwtTools.validateToken(token, user.getEmail())) {

                    // Controlli sullo stato dell'utente
                    if (!user.getEnabled()) {
                        log.warn("Tentativo di accesso con token valido per account disabilitato: {}", userEmail);
                        writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Account disabilitato");
                        return;
                    }

                    if (!user.getEmailVerified()) {
                        log.warn("Tentativo di accesso con email non verificata: {}", userEmail);
                        writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Email non verificata");
                        return;
                    }

                    // 5. Crea l'oggetto di autenticazione e impostalo nel contesto di sicurezza
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRuolo().name()))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Utente autenticato con successo tramite JWT: {}", userEmail);
                } else {
                    log.warn("Token JWT non valido per utente: {}", userEmail);
                    writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token non valido");
                    return;
                }
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.warn("Token JWT scaduto per utente: {}", e.getClaims().getSubject());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token scaduto");
            return;
        } catch (MalformedJwtException e) {
            log.warn("Token JWT malformato: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token malformato");
            return;
        } catch (Exception e) {
            log.error("Errore nel filtro JWT: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Errore di autenticazione");
            return;
        }
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                java.time.LocalDateTime.now().toString(),
                status,
                status == 401 ? "Unauthorized" : "Error",
                message
        );

        response.getWriter().write(jsonResponse);
    }
}