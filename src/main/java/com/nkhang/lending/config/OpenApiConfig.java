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
                .title("Mini Lending Credit Line API")
                .description("Credit assessment, utilization and repayment simulator")
                .version("v1"));
    }
}

