package cl.petshop.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI petshopOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("PetShop Chile - API REST")
                .version("v1")
                .description("Productos, categorias, usuarios, autenticacion JWT y pedidos."))
            .schemaRequirement("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Pega el JWT recibido al iniciar sesión. Swagger añade el prefijo Bearer."));
    }
}
