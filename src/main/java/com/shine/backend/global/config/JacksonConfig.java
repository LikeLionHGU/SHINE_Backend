package com.shine.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

/**
 * 요청 본문에 모르는 필드가 섞여 있어도 400을 내지 않는다.
 *
 * 프론트 판정 엔진이 자라면서 items[]에 필드가 계속 붙는다(전달사항 2번).
 * 서버가 아직 모르는 필드 하나 때문에 400이 나면, 사용자는 검사지를 아예 저장하지 못한다.
 * Jackson 3 는 FAIL_ON_UNKNOWN_PROPERTIES 가 기본으로 꺼져 있지만, 나중에 누가 켜더라도
 * ParsedTestItemDto 의 @JsonIgnoreProperties(ignoreUnknown = true) 가 막아준다.
 *
 * @ConfigurationPropertiesScan 은 OpenAiProperties 를 빈으로 잡기 위한 것이다.
 */
@Configuration
@ConfigurationPropertiesScan("com.shine.backend")
public class JacksonConfig {
}
