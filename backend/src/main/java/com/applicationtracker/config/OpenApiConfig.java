package com.applicationtracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI 3 / Swagger documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI applicationTrackerOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Application Tracker API")
                .description("REST API for Job Application Tracking, Status Lifecycle Progression, Analytics, and Follow-ups")
                .version("1.0.0")
                .contact(new Contact().name("Application Tracker Team"))
            );
    }
}
