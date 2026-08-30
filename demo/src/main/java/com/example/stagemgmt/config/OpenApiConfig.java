package com.example.stagemgmt.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentation interactive disponible sur /swagger-ui.html une fois l'appli lancée.
 *  Ne couvre que les endpoints REST (@RestController) - les pages Thymeleaf classiques
 *  ne sont pas des API et n'apparaissent pas ici, ce qui est normal. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI internBridgeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("InternBridge API")
                .description("Endpoints REST de l'application de gestion des stagiaires (notifications, chatbot).")
                .version("1.0"));
    }
}
