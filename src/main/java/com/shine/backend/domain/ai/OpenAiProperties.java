package com.shine.backend.domain.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * OpenAI 프록시 설정.
 *
 * 키는 반드시 서버 환경변수로만 들어온다. 앱 번들에 박히면 배포 즉시 누구나 꺼내 쓸 수 있다.
 *
 * @param allowedModels     프론트가 아무 모델이나 부르지 못하게 막는다.
 * @param maxTokensLimit    응답 상한. 프론트가 더 큰 값을 보내도 여기서 잘라 요금을 묶는다.
 * @param dailyLimitPerUser 사용자당 하루 호출 수. 검사지 한 장에 OCR 1~2회 + 요약 1회가 든다.
 */
@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String apiKey,
        String baseUrl,
        List<String> allowedModels,
        Integer maxTokensLimit,
        Integer dailyLimitPerUser,
        Integer timeoutSeconds,
        Integer maxRequestBytes
) {
    public OpenAiProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.openai.com/v1";
        if (allowedModels == null || allowedModels.isEmpty()) {
            // OCR은 gpt-4o. mini는 수치를 잘못 읽거나 지어낸다(프론트에서 확인됨).
            allowedModels = List.of("gpt-4o", "gpt-4o-mini");
        }
        if (maxTokensLimit == null) maxTokensLimit = 8192;
        if (dailyLimitPerUser == null) dailyLimitPerUser = 60;
        if (timeoutSeconds == null) timeoutSeconds = 120;
        if (maxRequestBytes == null) maxRequestBytes = 12 * 1024 * 1024;   // 사진이 base64로 실려 온다
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
