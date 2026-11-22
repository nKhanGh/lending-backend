package com.nkhang.lending.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI lendingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Mini PayLater API")
                .description("A concurrency-safe PayLater simulator")
                .version("v1"));
    }
}

