package com.shine.backend.domain.testsheet.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 개발용 가짜 OCR.
 *
 * 실제 산전검진 조회결과지에서 그대로 옮긴 20행을 돌려준다.
 * API 키 없이 파싱·매칭·판정 전 과정을 테스트할 수 있고,
 * 호출 비용도 들지 않는다.
 *
 * 실제 엔진을 붙이면 @Primary를 그쪽으로 옮긴다.
 */
@Slf4j
@Primary
@Component
public class FixtureOcrClient implements OcrClient {

    private static final String C = "CHEMISTRY";
    private static final String H = "HEMATOLOGY";
    private static final String I = "IMMUNO-SEROLOGY";
    private static final String U = "URINALYSIS";
    private static final String G = "그룹검사";
    private static final String E = "장비검사";

    @Override
    public OcrResult analyze(List<String> imageKeys) {
        log.info("픽스처 OCR 사용 — 실제 엔진이 아닙니다. imageKeys={}", imageKeys);

        List<OcrRow> rows = List.of(
                OcrRow.of(C, "AST(SGOT)", "18", "IU/L", "8 ~ 38", "정상"),
                OcrRow.of(C, "ALT(SGPT)", "16", "IU/L", "4 ~ 44", "정상"),

                OcrRow.of(H, "혈색소(헤모글로빈)", "12.2", "g/dl", "12 ~ 16", "정상"),
                OcrRow.of(H, "헤마토크리트", "36.1", "%", "35 ~ 54", "정상"),
                OcrRow.of(H, "적혈구수(RBC)", "3.98", "M/UL", "3.5 ~ 5.8", "정상"),
                OcrRow.of(H, "백혈구수(WBC)", "6.1", "K/UL", "4.0 ~ 10.0", "정상"),
                OcrRow.of(H, "혈소판수(Platelet)", "213", "K/UL", "110 ~ 450", "정상"),
                OcrRow.of(H, "A,B,O 혈액형검사 - 혈구", "A", null, "AB,A,O,미검,B", "정상"),
                OcrRow.of(H, "Rho,D형혈액형검사,Rh-Ir", "RH(+)", null, "미검,RH-,RH+", "정상"),

                OcrRow.of(I, "매독반응검사 RPR", "Non Reactive", null, "Non Reactive,미검", "정상"),
                OcrRow.of(I, "매독항체검사", "음성", null, "미검,음성", "정상"),
                OcrRow.of(I, "B형간염표면항원-HBs Ag(CIA)", "음성(0.07)", null, "음성", "정상"),
                OcrRow.of(I, "B형간염표면항체-HBs Ab(CIA)", "양성", null,
                        "Borderline( 8~12 ), 양성,음성,약양성", "정상"),
                OcrRow.of(I, "정밀면역검사-HIV 항원/항체 동시 선별", "음성(0.08)", "S/CO", "미검,음성", "정상"),

                OcrRow.of(U, "요단백", "음성(-)", null, "음성,음성(-)", "정상"),
                OcrRow.of(U, "요당", "음성(-)", null, "음성,음성(-)", "정상"),

                OcrRow.of(G, "바이러스항체,정밀-IgG-Rubella(진단검사의학과전문의판독)", "양성", null,
                        "약양성,미검,양성,음성", "정상"),
                OcrRow.of(G, "바이러스항체,정밀-IgM-Rubella(진단검사의학과전문의판독)", "음성", null,
                        "약양성,음성,미검,양성", "정상"),

                OcrRow.of(E, "흉부 [Chest PA](폐결핵)", "정상", null, "비활동성,정상,미검", "정상"),

                // 카탈로그에 없는 항목 — 미매칭 처리를 확인하기 위해 일부러 넣었다
                OcrRow.of(C, "혈청 삼투압", "285", "mOsm/kg", null, null)
        );

        return new OcrResult(
                null,                 // 검사일을 못 읽은 상황을 재현한다
                "OO 산부인과",
                rows,
                "{\"fixture\":true}",
                engineName());
    }

    @Override
    public String engineName() {
        return "FIXTURE";
    }
}
