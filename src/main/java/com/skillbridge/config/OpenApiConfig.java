package com.skillbridge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI skillBridgeOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SkillBridge Platform API")
                .description("REST API documentation for the SkillBridge Platform")
                .version("v0.0.1"));
    }
}