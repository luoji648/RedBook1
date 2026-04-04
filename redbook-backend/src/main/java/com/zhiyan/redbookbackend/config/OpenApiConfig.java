package com.zhiyan.redbookbackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / SpringDoc OpenAPI 全局信息（中文）。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI redbookOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("小红书 RedBook API")
                        .description("UGC 社区 REST 接口，统一响应 Result（success、errorMsg、data、total）。")
                        .version("1.0.0")
                        .contact(new Contact().name("redbook")));
    }
}
