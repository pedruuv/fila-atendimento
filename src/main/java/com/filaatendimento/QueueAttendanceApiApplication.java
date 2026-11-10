package com.filaatendimento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Starting point of the queue attendance REST API.
 *
 * <p>The code is organized by business modules:
 * {@code com.filaatendimento.attendance} (queue and tickets),
 * {@code com.filaatendimento.access} (users and authentication),
 * {@code com.filaatendimento.security} (JWT, roles and CORS) and
 * {@code com.filaatendimento.common} (shared error handling).</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class QueueAttendanceApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueAttendanceApiApplication.class, args);
    }
}
