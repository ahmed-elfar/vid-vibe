package com.xay.videos_recommender.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Personalized Video Feeds API")
                        .description("Backend API for serving personalized video feeds based on user signals " +
                                "(watch history, engagement, category affinities). Supports multi-tenancy, " +
                                "feature flags, and graceful fallback for cold-start users.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Video Recommender Team")
                                .email("team@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")
                ));
    }
}

