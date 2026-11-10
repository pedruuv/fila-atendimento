package com.filaatendimento.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the {@code Authorization: Bearer <token>} header on every request.
 * When a valid token is present, the caller identity ({@link AuthenticatedUser})
 * is stored in the Spring Security context so the request can be authorized
 * by role. An absent, malformed or expired token simply leaves the request
 * unauthenticated — protected routes then reject it with 401.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService tokenService;

    public JwtAuthenticationFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
            try {
                AuthenticatedUser caller = tokenService.readAuthenticatedUser(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        caller, token, List.of(caller.role().asAuthority()));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (com.filaatendimento.common.InvalidAccessTokenException ignored) {
                // Token presente mas inválido: segue sem autenticar. Rotas protegidas
                // responderão 401 por não haver contexto de autenticação.
            }
        }

        filterChain.doFilter(request, response);
    }
}
