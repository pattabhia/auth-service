package com.haiintel.authservice.infrastructure.security;

import com.haiintel.authservice.domain.model.UserPrincipal;
import com.haiintel.authservice.domain.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * JWT authentication filter.
 * Extracts and validates JWT token from Authorization header.
 * 
 * ✅ P0 FIX: Token revocation check (in JwtService.validateToken)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // Add request ID for distributed tracing
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-ID", requestId);
        
        try {
            String token = extractToken(request);
            
            if (token != null) {
                try {
                    UserPrincipal user = jwtService.validateToken(token);
                    
                    // Create authentication token with role
                    List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                    );
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                    
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("User authenticated: email={}, role={}", 
                        user.getEmail(), user.getRole());
                    
                } catch (Exception e) {
                    log.error("JWT validation failed", e);
                    // Don't set authentication - will be handled by AuthenticationEntryPoint
                }
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            MDC.clear();
        }
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}

