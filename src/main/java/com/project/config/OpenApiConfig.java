package com.project.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for Swagger UI.
 *
 * Provides interactive API documentation at:
 * - Swagger UI: /swagger-ui.html
 * - OpenAPI JSON: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Scalable Spring Boot API}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName)
                        .version("1.0.0")
                        .description("Production-ready scalable REST API with PostgreSQL, Redis, RabbitMQ, and Kafka integration")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@project.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.production.com")
                                .description("Production Server")))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("apiKey",
                                new SecurityScheme()
                                        .name("X-API-Key")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("API Key for authentication. Pass your API key in the X-API-Key header.")));
    }
}
