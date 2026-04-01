package com.finance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures SpringDoc to generate Swagger UI at /swagger-ui.html.
 * Also registers the "Bearer Auth" security scheme so testers can
 * paste a JWT directly into Swagger UI and test all protected endpoints.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI financeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Dashboard API")
                        .description("""
                                Backend API for the Finance Dashboard system.
                                
                                **Default credentials (from seed data):**
                                - Admin:    admin@finance.com / Admin@123
                                - Analyst:  analyst@finance.com / Analyst@123
                                - Viewer:   viewer@finance.com / Viewer@123
                                
                                **Auth flow:** POST /api/auth/login → copy token → click 'Authorize' above → paste token.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Finance Backend").email("dev@finance.com"))
                )
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the JWT token obtained from /api/auth/login")
                        )
                );
    }
}
