package com.shine.backend.domain.compat.dto;

/**
 * 프론트의 ParsedTestItem 과 같은 모양.
 * OpenAI Vision이 검사지에서 읽어낸 한 줄이다.
 *
 * value는 "12.2 g/dL (11~15)" 처럼 값·단위·참고치가 한 문자열에 뭉쳐 있고,
 * "음성(0.07)" 같은 혼합 표기도 그대로 들어온다.
 */
public record ParsedTestItemDto(
        String name,
        String value,
        /** 안심 | 주의 | 위험 */
        String status,
        String definition,
        String verdict
) {}
