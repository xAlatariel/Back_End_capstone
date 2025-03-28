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


        if (request.getMethod().equals("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");


        String requestURI = request.getRequestURI();
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        String userEmail = null;

        try {

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                userEmail = jwtTools.extractEmail(token);


                if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    User user = userRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    if (jwtTools.validateToken(token, userEmail)) {
                        // Create authentication token with user role
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRuolo().name());
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                Collections.singletonList(authority)
                        );

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            sendError(response, "JWT token expired");
        } catch (MalformedJwtException e) {
            sendError(response, "Invalid JWT token");
        } catch (JwtException e) {
            sendError(response, "JWT token error: " + e.getMessage());
        } catch (Exception e) {
            sendError(response, "Authentication error: " + e.getMessage());
        }
    }

    private boolean isPublicEndpoint(String uri) {
        List<String> publicEndpoints = List.of(
                "/api/users/register",
                "/api/users/login",
                "/swagger-ui",
                "/v3/api-docs"
        );

        return publicEndpoints.stream().anyMatch(uri::startsWith);
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
        response.getWriter().flush();
    }
}