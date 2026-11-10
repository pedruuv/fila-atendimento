package com.filaatendimento.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configures the clock used by the token service, so issued and expired
 * tokens behave consistently across the application and in tests.
 */
@Configuration
public class SystemClockConfiguration {

    @Bean
    public Clock systemUtcClock() {
        return Clock.systemUTC();
    }
}
