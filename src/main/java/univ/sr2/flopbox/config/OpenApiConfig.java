package univ.sr2.flopbox.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        System.out.println("OpenAPI bean initialized");
        return new OpenAPI()
                .info(new Info()
                        .title("Flopbox API")
                        .version("1.0")
                        .description("API REST pour la gestion multi-serveurs FTP (SR2)"));
    }
}