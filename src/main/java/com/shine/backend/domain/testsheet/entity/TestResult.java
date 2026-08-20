package com.shine.backend.domain.testsheet.entity;

import com.shine.backend.domain.testitem.entity.ResultType;
import com.shine.backend.domain.testitem.entity.TestItemCatalog;
import com.shine.backend.domain.user.entity.User;
import com.shine.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 검사지에 적힌 값 1개. 영수증의 한 줄에 해당한다.
 *
 * 설계결정②: 행 단위 분리는 타협 불가능하다.
 *   JSON으로 합치면 추이 그래프가 구현 자체가 안 되고, 나중에 고치려면 마이그레이션이 필요하다.
 * 설계결정⑧: user/testDate/pregnancyWeek는 TestSheet의 복사본이다.
 *   추이 그래프가 가장 자주 실행되는 쿼리라 조인 없이 인덱스만으로 끝나야 한다.
 */
@Getter
@Entity
@Builder
@Table(name = "test_results")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_sheet_id", nullable = false)
    private TestSheet testSheet;

    /** 설계결정⑤: 매칭 실패해도 버리지 않는다. NULL이면 미매칭이고 ocrLabel에 원문이 남는다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_item_id")
    private TestItemCatalog testItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    @Column(name = "pregnancy_week", nullable = false)
    private Integer pregnancyWeek;

    @Column(name = "ocr_label", nullable = false, length = 100)
    private String ocrLabel;

    @Column(name = "ocr_category", length = 30)
    private String ocrCategory;

    /** 결과 원문 그대로. "음성(0.07)", "RH(+)", "12.2" */
    @Column(name = "raw_value", nullable = false, length = 100)
    private String rawValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type")
    private ResultType resultType;

    /** MIXED 항목은 이 둘을 동시에 채운다. "음성(0.07)" → textValue=음성, numberValue=0.07 */
    @Column(name = "number_value", precision = 12, scale = 4)
    private BigDecimal numberValue;

    @Column(name = "text_value", length = 100)
    private String textValue;

    @Column(length = 30)
    private String unit;

    /** 병원마다 g/dl, g/dL, M/UL 로 제각각이라 원문을 따로 남긴다. */
    @Column(name = "unit_raw", length = 30)
    private String unitRaw;

    @Column(name = "sheet_normal_min", precision = 12, scale = 4)
    private BigDecimal sheetNormalMin;

    @Column(name = "sheet_normal_max", precision = 12, scale = 4)
    private BigDecimal sheetNormalMax;

    /** 열거형("양성,음성,약양성")은 정상값 목록이 아니라 가능한 값 목록이라 판정에 쓰지 않는다. */
    @Column(name = "sheet_normal_text", length = 255)
    private String sheetNormalText;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_range_source", nullable = false)
    private NormalRangeSource normalRangeSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false)
    private ResultStatus resultStatus;

    /** 검사지에 이미 인쇄된 판정. 우리 판정과 대조해 데이터 품질을 감시한다. */
    @Column(name = "sheet_verdict", length = 20)
    private String sheetVerdict;

    @Column(name = "verdict_mismatch", nullable = false)
    private boolean verdictMismatch;

    @Column(name = "is_edited_by_user", nullable = false)
    private boolean editedByUser;

    @Column(name = "brief_for_mom", length = 500)
    private String briefForMom;

    @Column(name = "brief_for_doctor", length = 500)
    private String briefForDoctor;

    /**
     * 프론트 판정 엔진이 낸 세분화 상태(safe/watch/recheck/…).
     * result_status 는 이걸 넷으로 접은 값이라 "재검 필요"와 "지켜보기"를 구분할 수 없다.
     */
    @Column(name = "engine_status", length = 20)
    private String engineStatus;

    /**
     * 판정 근거·출처·추천 질문 원본(JSON).
     *
     * 저장하지 않으면 기록 탭에서 지난 검사지를 다시 열었을 때 근거와 출처가 사라진다.
     * 값으로 재판정을 시도해도 인용문까지는 복원되지 않는다(전달사항 2번).
     * 판정 로직이 서버로 넘어오기 전까지는 프론트가 만든 것을 그대로 보관한다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "engine_meta")
    private String engineMeta;

    /** 원본 이미지 위 하이라이트. 0~1 정규화 비율이며 픽셀 절대값이 아니다. */
    @Column(name = "bbox_page")
    private Integer bboxPage;

    @Column(name = "bbox_x", precision = 6, scale = 5)
    private BigDecimal bboxX;

    @Column(name = "bbox_y", precision = 6, scale = 5)
    private BigDecimal bboxY;

    @Column(name = "bbox_width", precision = 6, scale = 5)
    private BigDecimal bboxWidth;

    @Column(name = "bbox_height", precision = 6, scale = 5)
    private BigDecimal bboxHeight;

    // ---------- 변경 ----------

    public boolean isUnmatched() {
        return testItem == null;
    }

    public void applyEvaluation(ResultStatus status, NormalRangeSource source, boolean mismatch) {
        this.resultStatus = status;
        this.normalRangeSource = source;
        this.verdictMismatch = mismatch;
    }

    /** 사용자가 OCR 오독을 직접 고친 경우. LLM은 재호출하지 않는다. */
    public void editByUser(BigDecimal numberValue, String textValue) {
        this.numberValue = numberValue;
        this.textValue = textValue;
        this.editedByUser = true;
        this.briefForDoctor = null;
    }

    /** 프론트 판정 엔진이 붙여 보낸 근거를 그대로 보관한다 */
    public void applyEngineMeta(String engineStatus, String engineMeta) {
        this.engineStatus = engineStatus;
        this.engineMeta = engineMeta;
    }

    public void applyBrief(String briefForMom, String briefForDoctor) {
        this.briefForMom = briefForMom;
        this.briefForDoctor = briefForDoctor;
    }

    /** 검사일이 확정되면 비정규화된 복사본도 함께 갱신한다(설계결정⑧) */
    public void applySheetDate(java.time.LocalDate testDate, int pregnancyWeek) {
        this.testDate = testDate;
        this.pregnancyWeek = pregnancyWeek;
    }
}
