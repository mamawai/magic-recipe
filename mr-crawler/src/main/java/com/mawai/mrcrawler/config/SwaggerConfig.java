package com.mawai.mrcrawler.config;

import com.mawai.mrcommon.config.BaseSwaggerConfig;
import com.mawai.mrcommon.domain.SwaggerProperties;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig extends BaseSwaggerConfig {


    @Override
    public SwaggerProperties swaggerProperties() {
        return SwaggerProperties.builder()
                .apiBasePackage("com.mawai.mrcrawler.controller")
                .title("mrcrawler-api")
                .description("mrcrawler搜索相关接口文档")
                .contactName("magic-recipe-crawler")
                .version("1.0")
                .enableSecurity(true)
                .build();
    }

    @Bean
    public OpenAPI customOpenApi() {
        return createOpenApi();
    }
}
