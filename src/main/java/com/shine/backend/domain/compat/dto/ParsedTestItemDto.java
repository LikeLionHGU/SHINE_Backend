package com.shine.backend.domain.compat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 프론트의 ParsedTestItem 과 같은 모양.
 * 검사지에서 읽어낸 한 줄 + 프론트 판정 엔진(src/lib/labs)이 붙인 근거다.
 *
 * value는 "12.2 g/dL (11~15)" 처럼 값·단위·참고치가 한 문자열에 뭉쳐 있고,
 * "음성(0.07)" 같은 혼합 표기도 그대로 들어온다.
 *
 * 엔진 필드는 전부 optional 이다. 프론트가 구버전이거나 엔진이 모르는 항목이면
 * 통째로 비어 온다. 모르는 필드가 하나 늘었다고 400을 내면 앱이 검사지를 아예
 * 저장하지 못하므로, 여기서는 알 수 없는 필드를 조용히 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ParsedTestItemDto(
        String name,
        String value,
        /** 안심 | 주의 | 위험 | 확인 필요 */
        String status,
        String definition,
        String verdict,

        /**
         * 검사지에 인쇄돼 있던 원문 항목명.
         * 서버가 카탈로그 대표명으로 바꿔주므로, 사용자가 손에 든 종이와 대조할 수 있게
         * 원문을 함께 들고 다닌다. 대표명과 같으면 비어 있다.
         */
        String originalName,

        // ---------- 프론트 판정 엔진이 채우는 값 (전부 optional) ----------

        /** safe | watch | recheck | indeterminate | alert | info_only | unsupported */
        String engineStatus,
        /** 세부 라벨. "중등도 빈혈", "재검 필요" — 상세 화면에서만 쓴다 */
        String badgeLabel,
        /** 무엇과 비교한 판정인지. "임신 주수 기준 10.1~14.1" */
        String basisLabel,
        /** 검사지 기준과 임신 중 기준이 엇갈릴 때의 설명 */
        String contrastNote,
        /** 검사법 편차 등 함께 알려야 하는 주의사항 */
        List<String> caveats,
        /** 판정 근거. 비어 있으면 프론트는 판정을 화면에 띄우지 않는다 */
        List<CitationDto> citations,
        /** 다음 진료 때 물어보면 좋을 질문 한 문장 */
        String doctorQuestion,
        /** 이전 검사 대비 변화가 클 때의 안내 */
        String trendNote,
        /** "이 숫자 맞나요?" 하고 되물어야 하는 항목 */
        Boolean needsConfirm,

        // ---------- 서버가 채워 응답에만 싣는 값 ----------

        /**
         * 저장된 test_results 의 id.
         *
         * 지금 프론트는 요청과 응답을 배열 인덱스로 짝짓는데, 그 방식은 서버가 정렬이나
         * 중복 제거를 한 번만 해도 항목 이름이 뒤섞인다. 인덱스 대신 쓸 수 있는 안정적인
         * 키를 여기서 내려준다(전달사항 3번). 프론트가 이걸로 옮겨가면 순서 의존이 사라진다.
         */
        Long resultId,
        /** 카탈로그 항목 코드("HB"). 매칭 실패면 null */
        String itemCode
) {

    /** 판정 근거 한 건 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CitationDto(String label, String url, String quote, String badge) {}

    /** 프론트 판정 엔진이 판정을 내려준 항목인지 */
    public boolean hasEngineVerdict() {
        return engineStatus != null && !engineStatus.isBlank();
    }

    /**
     * 엔진 판정은 그대로 두고 이름만 카탈로그 대표명으로 바꾼다.
     * 원문명은 잃지 않는다 — 사용자가 종이와 대조해야 한다.
     */
    public ParsedTestItemDto withCatalogName(String catalogName, Long resultId, String itemCode) {
        String display = (catalogName == null || catalogName.isBlank()) ? name : catalogName;
        String origin = originalName != null ? originalName
                : (display.equals(name) ? null : name);

        return new ParsedTestItemDto(
                display, value, status, definition, verdict, origin,
                engineStatus, badgeLabel, basisLabel, contrastNote, caveats, citations,
                doctorQuestion, trendNote, needsConfirm,
                resultId, itemCode);
    }

    /** 엔진이 모르는 항목 — 서버 판정으로 이름·상태·설명을 덮어쓴다 */
    public ParsedTestItemDto withServerVerdict(String catalogName, String status,
                                               String definition, String verdict,
                                               Long resultId, String itemCode) {
        String display = (catalogName == null || catalogName.isBlank()) ? name : catalogName;
        String origin = originalName != null ? originalName
                : (display.equals(name) ? null : name);

        return new ParsedTestItemDto(
                display, value, status, definition, verdict, origin,
                engineStatus, badgeLabel, basisLabel, contrastNote, caveats, citations,
                doctorQuestion, trendNote, needsConfirm,
                resultId, itemCode);
    }
}
