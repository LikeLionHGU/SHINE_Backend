package com.shine.backend.domain.record.controller;

import com.shine.backend.domain.record.dto.RecordListResponse;
import com.shine.backend.domain.record.service.RecordService;
import com.shine.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Records", description = "기록 타임라인")
@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @Operation(summary = "기록 타임라인",
            description = "검사지를 최신순으로. 카드를 누르면 GET /test-sheets/{id}로 상세를 연다.")
    @GetMapping
    public ApiResponse<RecordListResponse> getRecords(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(recordService.getRecords(userId, cursor, size));
    }
}
