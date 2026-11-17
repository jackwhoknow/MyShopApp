package com.example.myshopapp.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 在线接口文档：Swagger UI 默认地址 /swagger-ui.html，OpenAPI JSON 地址 /v3/api-docs。
 * 生产环境通过 API_DOCS_ENABLED=false 关闭，避免暴露内部接口结构。
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI myShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyShopApp API")
                        .description("商城交易系统接口文档：买家、商家、平台管理三端统一鉴权与错误码约定")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录接口返回的 token，填入后即可调试需要登录的接口")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
