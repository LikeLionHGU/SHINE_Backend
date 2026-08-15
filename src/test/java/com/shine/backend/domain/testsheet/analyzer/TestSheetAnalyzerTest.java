package com.shine.backend.domain.testsheet.analyzer;

import com.shine.backend.domain.testitem.entity.ResultType;
import com.shine.backend.domain.testsheet.entity.NormalRangeSource;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import com.shine.backend.domain.testsheet.ocr.OcrClient;
import com.shine.backend.domain.testsheet.ocr.OcrResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 산전검진 결과지 20행을 파서·매처·판정 엔진에 통과시킨다.
 * 시딩된 카탈로그를 그대로 쓰므로, 별칭이 실제 검사지 표기를 커버하는지도 함께 검증된다.
 */
@SpringBootTest
class TestSheetAnalyzerTest {

    @Autowired private OcrClient ocrClient;
    @Autowired private TestSheetAnalyzer analyzer;

    private List<AnalyzedRow> rows;

    @BeforeEach
    void analyze() {
        OcrResult ocr = ocrClient.analyze(List.of("fixture.jpg"));
        rows = analyzer.analyze(ocr);
    }

    private AnalyzedRow row(String labelPart) {
        return rows.stream()
                .filter(r -> r.ocrLabel().contains(labelPart))
                .findFirst()
                .orElseThrow(() -> new AssertionError("행을 찾지 못했습니다: " + labelPart));
    }

    @Test
    @DisplayName("20행을 읽고 19행을 카탈로그와 연결한다")
    void matchesAlmostEveryRow() {
        assertThat(rows).hasSize(20);

        long matched = rows.stream().filter(AnalyzedRow::isMatched).count();
        assertThat(matched).isEqualTo(19);
    }

    @Test
    @DisplayName("검사방법 표기(CIA)가 붙어 있어도 항목을 찾는다")
    void matchesDespiteMethodSuffix() {
        assertThat(row("HBs Ag(CIA)").item().getCode()).isEqualTo("HBSAG");
        assertThat(row("HBs Ab(CIA)").item().getCode()).isEqualTo("HBSAB");
    }

    @Test
    @DisplayName("콤마로 여러 표기가 붙어 와도 항목을 찾는다")
    void matchesCommaJoinedLabel() {
        assertThat(row("Rho,D형혈액형검사").item().getCode()).isEqualTo("RH");
        assertThat(row("A,B,O 혈액형검사").item().getCode()).isEqualTo("ABO");
    }

    @Test
    @DisplayName("전문의 판독 표기가 붙은 항목도 찾는다")
    void matchesRadiologyAndGroupTests() {
        assertThat(row("IgG-Rubella").item().getCode()).isEqualTo("RUB_IGG");
        assertThat(row("IgM-Rubella").item().getCode()).isEqualTo("RUB_IGM");
        assertThat(row("Chest PA").item().getCode()).isEqualTo("CXR");
    }

    @Test
    @DisplayName("★ 혈색소는 검사지 참고치(12~16)를 무시하고 임신 기준으로 판정한다")
    void hemoglobinUsesPregnancyRange() {
        AnalyzedRow hb = row("혈색소");

        assertThat(hb.item().getCode()).isEqualTo("HB");
        assertThat(hb.numberValue()).isEqualByComparingTo("12.2");
        assertThat(hb.unit()).isEqualTo("g/dL");           // 원문은 "g/dl"
        assertThat(hb.normalRangeSource()).isEqualTo(NormalRangeSource.CATALOG);
        assertThat(hb.resultStatus()).isEqualTo(ResultStatus.NORMAL);
    }

    @Test
    @DisplayName("일반 항목은 검사지에 인쇄된 참고치를 쓴다")
    void ordinaryItemUsesSheetRange() {
        AnalyzedRow ast = row("AST");

        assertThat(ast.normalRangeSource()).isEqualTo(NormalRangeSource.SHEET);
        assertThat(ast.sheetNormalMin()).isEqualByComparingTo("8");
        assertThat(ast.sheetNormalMax()).isEqualByComparingTo("38");
        assertThat(ast.resultStatus()).isEqualTo(ResultStatus.NORMAL);
    }

    @Test
    @DisplayName("음성(0.07)은 판정과 측정치를 모두 남긴다")
    void keepsBothPartsOfMixedValue() {
        AnalyzedRow hbsag = row("HBs Ag(CIA)");

        assertThat(hbsag.resultType()).isEqualTo(ResultType.MIXED);
        assertThat(hbsag.textValue()).isEqualTo("음성");
        assertThat(hbsag.numberValue()).isEqualByComparingTo("0.07");
        assertThat(hbsag.resultStatus()).isEqualTo(ResultStatus.NORMAL);
    }

    @Test
    @DisplayName("기호 표기를 표준값으로 바꾼다")
    void normalizesSymbolNotation() {
        AnalyzedRow urineProtein = row("요단백");

        assertThat(urineProtein.rawValue()).isEqualTo("음성(-)");   // 원문 보존
        assertThat(urineProtein.textValue()).isEqualTo("음성");     // 표준값
        assertThat(urineProtein.resultStatus()).isEqualTo(ResultStatus.NORMAL);
    }

    @Test
    @DisplayName("Non Reactive도 음성으로 알아본다")
    void normalizesNonReactive() {
        AnalyzedRow rpr = row("매독반응검사");

        assertThat(rpr.textValue()).isEqualTo("음성");
        assertThat(rpr.resultStatus()).isEqualTo(ResultStatus.NORMAL);
    }

    @Test
    @DisplayName("열거형 참고치는 판정에 쓰지 않고 원문만 보존한다")
    void doesNotUseEnumerationAsRange() {
        // "Borderline( 8~12 ), 양성,음성,약양성" 에서 8~12를 정상 범위로 읽으면 판정이 뒤집힌다
        AnalyzedRow hbsab = row("HBs Ab(CIA)");

        assertThat(hbsab.sheetNormalMin()).isNull();
        assertThat(hbsab.sheetNormalMax()).isNull();
        assertThat(hbsab.sheetNormalText()).contains("Borderline");
        assertThat(hbsab.resultStatus()).isEqualTo(ResultStatus.NORMAL);
    }

    @Test
    @DisplayName("모르는 항목은 버리지 않고 원문을 남긴 채 판정만 보류한다")
    void keepsUnmatchedRow() {
        AnalyzedRow unknown = row("혈청 삼투압");

        assertThat(unknown.isMatched()).isFalse();
        assertThat(unknown.ocrLabel()).isEqualTo("혈청 삼투압");
        assertThat(unknown.rawValue()).isEqualTo("285");
        assertThat(unknown.resultStatus()).isEqualTo(ResultStatus.UNKNOWN);
    }

    @Test
    @DisplayName("혈액형은 정상/이상 판정 대상이 아니다")
    void bloodTypeIsNotJudged() {
        assertThat(row("A,B,O 혈액형검사").resultStatus()).isEqualTo(ResultStatus.UNKNOWN);
        assertThat(row("Rho,D형혈액형검사").resultStatus()).isEqualTo(ResultStatus.UNKNOWN);
    }

    @Test
    @DisplayName("이 검사지에는 위험 항목이 없다")
    void noDangerInThisSheet() {
        assertThat(rows).noneMatch(r -> r.resultStatus() == ResultStatus.DANGER);
    }
}
