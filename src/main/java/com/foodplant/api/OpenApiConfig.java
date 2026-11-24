package com.foodplant.api;
import org.springframework.context.annotation.*;
import io.swagger.v3.oas.models.OpenAPI; import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components; import io.swagger.v3.oas.models.security.SecurityScheme;
@Configuration class OpenApiConfig {
 @Bean OpenAPI api() { return new OpenAPI().servers(java.util.List.of(new io.swagger.v3.oas.models.servers.Server().url("/"))).info(new Info().title("Food Plant Traceability API").version("1.0.0").description("Fictional food plant. Authenticate via POST /api/v1/auth/token; batch IDs, not batch numbers, are canonical."))
 .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))); }
}
