package com.shine.backend.domain.analysis.controller;

import com.shine.backend.domain.analysis.dto.AnalysisListResponse;
import com.shine.backend.domain.analysis.dto.AnalysisTrendResponse;
import com.shine.backend.domain.analysis.service.AnalysisService;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import com.shine.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Analysis", description = "분석 탭 · 수치 추이")
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "항목 리스트",
            description = "date를 비우면 최근 검사지. 위험→주의→안심→미분류 순으로 정렬된다.")
    @GetMapping("/items")
    public ApiResponse<AnalysisListResponse> getItems(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ResultStatus status) {
        return ApiResponse.success(analysisService.getItems(userId, date, keyword, status));
    }

    @Operation(summary = "항목 추이 상세",
            description = "X축은 검사일 또는 임신 주수다. 항목별 개별 그래프이며 여러 항목을 겹치지 않는다.")
    @GetMapping("/items/{testItemId}")
    public ApiResponse<AnalysisTrendResponse> getTrend(@AuthenticationPrincipal Long userId,
                                                       @PathVariable Long testItemId) {
        return ApiResponse.success(analysisService.getTrend(userId, testItemId));
    }
}
