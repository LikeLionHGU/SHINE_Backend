package com.shine.backend.domain.testsheet.entity;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 업로드한 검사지 1건. 영수증 1장에 해당한다. */
@Getter
@Entity
@Builder
@Table(name = "test_sheets")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestSheet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    /**
     * OCR이 검사지에서 날짜를 읽어냈거나 사용자가 확인해준 경우 true.
     * false면 추이 그래프의 X축이 어긋날 수 있으므로 사용자에게 물어봐야 한다.
     */
    @Column(name = "test_date_confirmed", nullable = false)
    private boolean testDateConfirmed;

    /** 설계결정①의 예외 — 그 시점의 주수가 의미 있으므로 스냅샷으로 저장한다. */
    @Column(name = "pregnancy_week", nullable = false)
    private Integer pregnancyWeek;

    @Column(name = "hospital_name", length = 100)
    private String hospitalName;

    @Column(name = "sheet_issued_date")
    private LocalDate sheetIssuedDate;

    /** S3 key 배열. 순서가 곧 페이지 순서다. 결과지는 보통 2~3장. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_keys", nullable = false)
    private List<String> imageKeys;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false)
    private AnalysisStatus analysisStatus;

    @Column(name = "failure_reason", length = 30)
    private String failureReason;

    /**
     * 설계결정③: OCR 응답을 통째로 보존한다.
     * 초기 파서는 반드시 항목을 놓치는데, 원본이 있으면 파서를 고친 뒤
     * 이미지 재업로드 없이 다시 돌려 복구할 수 있다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ocr_raw_json")
    private String ocrRawJson;

    @Column(name = "ocr_engine", length = 30)
    private String ocrEngine;

    /** 검사지에는 이름·주소가 인쇄돼 있다. 저장 전에 마스킹해야 한다. */
    @Column(name = "pii_masked", nullable = false)
    private boolean piiMasked;

    @Column(name = "summary_for_mom", length = 2000)
    private String summaryForMom;

    @Column(name = "summary_for_doctor", length = 2000)
    private String summaryForDoctor;

    /**
     * 프론트 AI가 추천한 음식. 화면마다 다른 재료가 뜨지 않도록 저장해두고 재사용한다.
     * [{"name":"시금치","reason":"철분이 많아요"}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nutrition_foods")
    private String nutritionFoods;

    @Column(name = "llm_model", length = 50)
    private String llmModel;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    // ---------- 상태 전이 ----------

    public void markAnalyzing() {
        this.analysisStatus = AnalysisStatus.ANALYZING;
    }

    public void markDone(String summaryForMom, String summaryForDoctor,
                         String llmModel, String promptVersion) {
        this.analysisStatus = AnalysisStatus.DONE;
        this.summaryForMom = summaryForMom;
        this.summaryForDoctor = summaryForDoctor;
        this.llmModel = llmModel;
        this.promptVersion = promptVersion;
        this.analyzedAt = LocalDateTime.now();
    }

    public void markFailed(String failureReason) {
        this.analysisStatus = AnalysisStatus.FAILED;
        this.failureReason = failureReason;
    }

    public void saveOcrRaw(String ocrRawJson, String ocrEngine, boolean piiMasked) {
        this.ocrRawJson = ocrRawJson;
        this.ocrEngine = ocrEngine;
        this.piiMasked = piiMasked;
    }

    public void confirmTestDate(LocalDate testDate, int pregnancyWeek) {
        this.testDate = testDate;
        this.pregnancyWeek = pregnancyWeek;
        this.testDateConfirmed = true;
    }

    public void applyOcrTestDate(LocalDate testDate, int pregnancyWeek) {
        if (testDate == null) return;
        this.testDate = testDate;
        this.pregnancyWeek = pregnancyWeek;
        this.testDateConfirmed = true;
    }

    public void applyHospitalName(String hospitalName) {
        if (this.hospitalName == null) this.hospitalName = hospitalName;
    }

    public void applyNutritionFoods(String nutritionFoods) {
        this.nutritionFoods = nutritionFoods;
    }
}
