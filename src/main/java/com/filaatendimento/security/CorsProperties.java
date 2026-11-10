package com.filaatendimento.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origins allowed to call the API from a browser (CORS), bound from the
 * {@code security.cors} configuration section. Each value may be an exact
 * origin or a pattern such as {@code http://localhost:*}; the special value
 * {@code *} allows every origin and should only be used in development.
 *
 * @param allowedOrigins list of allowed origin patterns
 */
@ConfigurationProperties(prefix = "security.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("*");
        }
    }
}
