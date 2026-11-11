
package cl.petshop.ms_productos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI petshopOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("PetShop Chile - API de Productos")
                        .version("v1")
                        .description(
                                "Microservicio encargado de gestionar "
                                + "los productos y categorias de PetShop Chile. "
                                + "Permite consultar productos, buscar por codigo, "
                                + "filtrar por categoria, consultar ofertas "
                                + "y administrar el catalogo."
                        )
                );
    }
}
