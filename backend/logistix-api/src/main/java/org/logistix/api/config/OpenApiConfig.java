package org.logistix.api.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Springdoc OpenAPI configuration for LogistiX API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI logistixOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LogistiX AI Platform API")
                        .description("Open Source AI Platform for Logistics & Transportation with Explainable Decision Intelligence")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("LogistiX Open Source Project")
                                .url("https://github.com/logistix-ai/logistix")
                                .email("maintainers@logistix.org"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/").description("Default Server")
                ))
                .externalDocs(new ExternalDocumentation()
                        .description("LogistiX Documentation")
                        .url("https://docs.logistix.org"));
    }
}
