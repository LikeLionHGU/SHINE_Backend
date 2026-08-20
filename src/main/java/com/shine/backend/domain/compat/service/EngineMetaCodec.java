package com.shine.backend.domain.compat.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.shine.backend.domain.compat.dto.ParsedTestItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 프론트 판정 엔진이 붙여 보낸 근거를 JSON 한 덩어리로 접었다 편다.
 *
 * 왜 컬럼을 쪼개지 않았나:
 *   근거 필드는 아직 모양이 굳지 않았고(엔진이 계속 자란다), 서버는 이 값을 읽거나
 *   조건에 걸지 않는다. 그대로 보관했다가 그대로 돌려주기만 하면 되는 값이라
 *   컬럼을 늘리는 대신 JSON 한 칸에 담는다. 판정 로직이 서버로 넘어오면 그때
 *   정규화하면 된다.
 *
 * 직렬화에 실패해도 검사지 저장 자체는 살린다. 근거가 없으면 화면 품질이 떨어질 뿐이지만,
 * 여기서 예외가 나가면 사용자는 검사지를 아예 저장하지 못한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EngineMetaCodec {

    private final ObjectMapper objectMapper;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EngineMeta(
            String engineStatus,
            String badgeLabel,
            String basisLabel,
            String contrastNote,
            List<String> caveats,
            List<ParsedTestItemDto.CitationDto> citations,
            String doctorQuestion,
            String trendNote,
            Boolean needsConfirm,
            String originalName
    ) {
        /** @return 담을 게 하나도 없으면 null — 빈 JSON을 저장하지 않는다 */
        public static EngineMeta from(ParsedTestItemDto d) {
            if (d == null) return null;

            EngineMeta meta = new EngineMeta(
                    blankToNull(d.engineStatus()),
                    blankToNull(d.badgeLabel()),
                    blankToNull(d.basisLabel()),
                    blankToNull(d.contrastNote()),
                    emptyToNull(d.caveats()),
                    emptyToNull(d.citations()),
                    blankToNull(d.doctorQuestion()),
                    blankToNull(d.trendNote()),
                    d.needsConfirm(),
                    blankToNull(d.originalName()));

            return meta.isEmpty() ? null : meta;
        }

        public boolean isEmpty() {
            return engineStatus == null && badgeLabel == null && basisLabel == null
                    && contrastNote == null && caveats == null && citations == null
                    && doctorQuestion == null && trendNote == null
                    && needsConfirm == null && originalName == null;
        }

        private static String blankToNull(String s) {
            return s == null || s.isBlank() ? null : s;
        }

        private static <T> List<T> emptyToNull(List<T> list) {
            return list == null || list.isEmpty() ? null : list;
        }
    }

    /** @return null·빈 값이면 null. 저장 컬럼에 "null" 문자열이 들어가지 않게 한다 */
    public String toJson(Object value) {
        if (value == null) return null;
        if (value instanceof List<?> list && list.isEmpty()) return null;

        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("판정 근거 직렬화 실패 — 근거 없이 저장한다", e);
            return null;
        }
    }

    /** @return 못 읽으면 null. 지난 검사지가 열리지 않는 것보다 근거만 비는 편이 낫다 */
    public EngineMeta read(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, EngineMeta.class);
        } catch (Exception e) {
            log.warn("판정 근거 역직렬화 실패 resultJson={}", abbreviate(json), e);
            return null;
        }
    }

    private String abbreviate(String s) {
        return s.length() <= 120 ? s : s.substring(0, 120) + "…";
    }
}
