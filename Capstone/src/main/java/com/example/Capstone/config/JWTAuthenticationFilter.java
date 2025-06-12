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

        final String authHeader = request.getHeader("Authorization");

        // 1. Se l'header non esiste o non inizia con "Bearer ", procedi senza autenticare.
        //    Sarà compito di SecurityConfig decidere se questo endpoint era pubblico o meno.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Estrai il token e l'email dell'utente
        String token = authHeader.substring(7);
        String userEmail = null;

        try {
            userEmail = jwtTools.extractEmail(token);

            // 3. Se abbiamo l'email e non c'è già un'autenticazione nel contesto di sicurezza
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new RuntimeException("Utente associato al token non trovato"));

                // 4. Valida il token e lo stato dell'utente
                if (jwtTools.validateToken(token, user.getEmail())) {

                    // Controlli sullo stato dell'utente (opzionali qui, ma buona pratica)
                    if (!user.getEnabled()) {
                        log.warn("Tentativo di accesso con token valido per account disabilitato: {}", userEmail);
                        // L'eccezione verrà gestita dal JwtAuthenticationEntryPoint
                        throw new RuntimeException("Account disabilitato");
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
                }
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.warn("Token JWT scaduto per utente: {}", e.getClaims().getSubject());
            // Lascia che l'entry point gestisca l'errore di token scaduto
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Token scaduto\"}");
        } catch (Exception e) {
            log.error("Errore nel filtro JWT: {}", e.getMessage());
            // Per altri errori, passa al gestore di eccezioni successivo
            filterChain.doFilter(request, response);
        }
    }




}