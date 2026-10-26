package com.hospital.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Hospital Appointment Booking API",
        version = "1.0",
        description = "REST API for departments, doctors, NLP symptom analysis and appointments. "
                + "Use the Authorize button with a demo account (e.g. patient@hospital.com / patient123)."))
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
public class AppConfig {

    /** Injected into services so date/time rules can be tested with a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
