package com.shine.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 우측 상단에 Authorize 버튼을 만든다.
 * 한 번 토큰을 넣어두면 모든 요청에 Authorization 헤더가 자동으로 붙는다.
 */
@Configuration
public class SwaggerConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("산전 검사지 AI 앱 API")
                        .version("v1")
                        .description("검사 결과지를 촬영하면 쉬운 말로 번역하고 수치 변화를 추적하는 앱의 백엔드"))
                .components(new Components().addSecuritySchemes(SCHEME_NAME, bearer))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
