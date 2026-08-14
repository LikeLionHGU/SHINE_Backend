package com.shine.backend.domain.testitem.entity;

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
import java.util.List;

/**
 * 검사 항목 사전(마스터).
 * 검사지의 자식 테이블이 아니라 앱 전체가 공유하는 사전이다.
 * 사용자가 10만 명이 되어도 행 수는 그대로다.
 */
@Getter
@Entity
@Builder
@Table(name = "test_item_catalog")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestItemCatalog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String code;

    @Column(name = "name_ko", nullable = false, length = 50)
    private String nameKo;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(nullable = false, length = 30)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false)
    private ResultType resultType;

    @Column(length = 30)
    private String unit;

    @Column(name = "normal_min", precision = 12, scale = 4)
    private BigDecimal normalMin;

    @Column(name = "normal_max", precision = 12, scale = 4)
    private BigDecimal normalMax;

    @Column(name = "normal_text", length = 50)
    private String normalText;

    /** OCR 자릿수 오독 탐지용. 12.0을 120으로 읽는 소수점 누락이 가장 흔하다. */
    @Column(name = "hard_limit_min", precision = 12, scale = 4)
    private BigDecimal hardLimitMin;

    @Column(name = "hard_limit_max", precision = 12, scale = 4)
    private BigDecimal hardLimitMax;

    @Column(name = "caution_margin_ratio", nullable = false, precision = 4, scale = 3)
    private BigDecimal cautionMarginRatio;

    /**
     * ★ 임신 중 정상범위가 비임신 성인과 크게 다른 항목.
     * 실제 산전검사지도 참고치가 비임신 기준으로 찍혀 나오므로,
     * TRUE면 검사지 참고치를 무시하고 이 카탈로그의 임신 기준을 쓴다.
     */
    @Column(name = "is_pregnancy_specific", nullable = false)
    private boolean pregnancySpecific;

    /** FALSE면 추이 그래프 제외. 영상검사·판독 소견은 꺾은선을 그릴 수 없다. */
    @Column(name = "is_trendable", nullable = false)
    private boolean trendable;

    /**
     * 설계결정④: 병원별 표기 차이를 코드의 if문이 아니라 데이터로 흡수한다.
     * ["혈색소", "헤모글로빈", "Hb", "HGB", "Hemoglobin"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "name_variants", nullable = false)
    private List<String> nameVariants;

    @Column(name = "brief_for_mom", length = 500)
    private String briefForMom;

    @Column(name = "brief_for_doctor", length = 500)
    private String briefForDoctor;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "loinc_code", length = 20)
    private String loincCode;

    public boolean isQualitative() {
        return resultType == ResultType.TEXT || resultType == ResultType.MIXED;
    }
}
