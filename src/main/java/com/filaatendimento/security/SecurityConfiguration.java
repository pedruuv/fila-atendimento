package com.filaatendimento.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import java.util.List;

import static com.filaatendimento.access.domain.UserRole.ADMIN;
import static com.filaatendimento.access.domain.UserRole.ATTENDANT;

/**
 * Central wiring of HTTP security: stateless setup with Bearer token
 * authentication, public routes, role based route protection and CORS rules.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    /* Public routes: authentication endpoints and the OpenAPI documentation. */
    private static final String[] PUBLIC_POST_ROUTES = {
            "/api/v1/auth/login",
            "/api/v1/users"
    };
    private static final String[] PUBLIC_DOCUMENTATION_ROUTES = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    };

    private final JwtAuthenticationFilter authenticationFilter;
    private final SecurityRejectionHandler rejectionHandler;
    private final CorsProperties corsProperties;

    public SecurityConfiguration(
            JwtAuthenticationFilter authenticationFilter,
            SecurityRejectionHandler rejectionHandler,
            CorsProperties corsProperties) {
        this.authenticationFilter = authenticationFilter;
        this.rejectionHandler = rejectionHandler;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(rejectionHandler)
                        .accessDeniedHandler(rejectionHandler))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR, jakarta.servlet.DispatcherType.FORWARD).permitAll()
                        .requestMatchers(PUBLIC_DOCUMENTATION_ROUTES).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, PUBLIC_POST_ROUTES).permitAll()
                        // Ticket management: any authenticated user may issue and consult.
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST, "/api/v1/tickets").authenticated()
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET, "/api/v1/tickets/**").authenticated()
                        // Attendance actions need ATTENDANT or ADMIN. Issue changes middleware?
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/v1/tickets/*/cancel",
                                "/api/v1/queue/calls").hasAnyRole(ATTENDANT.name(), ADMIN.name())
                        // Destructive actions are ADMIN only.
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/api/v1/tickets/*").hasRole(ADMIN.name())
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/v1/queue/clear").hasRole(ADMIN.name())
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/v1/queue/**").authenticated()
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/v1/users/**").hasRole(ADMIN.name())
                        .anyRequest().authenticated())
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12: securely slow in 2026 while remaining acceptable on typical requests.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
