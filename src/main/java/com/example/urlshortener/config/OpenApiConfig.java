package com.example.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI 3 document that powers the interactive Swagger UI
 * served at {@code /swagger-ui.html} and the raw spec at {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI urlShortenerOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("URL Shortener API")
                .version("1.0.0")
                .description("Create short aliases for long URLs and redirect visitors to the original destination.")
                .contact(new Contact().name("Platform Team").email("platform@example.com"))
                .license(new License().name("Apache 2.0")));
    }
}
