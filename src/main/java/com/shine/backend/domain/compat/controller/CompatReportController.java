package com.shine.backend.domain.compat.controller;

import com.shine.backend.domain.compat.dto.ReportResponse;
import com.shine.backend.domain.compat.dto.ReportUploadRequest;
import com.shine.backend.domain.compat.service.CompatReportService;
import com.shine.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reports", description = "프론트 OCR 결과 수신")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class CompatReportController {

    private final CompatReportService compatReportService;

    @Operation(summary = "검사지 저장",
            description = """
                    프론트가 OpenAI Vision으로 읽어낸 결과를 그대로 보내면 된다.
                    parseTestReport / generateReportInsights 결과를 합쳐 한 번에 보내면 되고,
                    응답의 items는 서버가 임신 기준으로 판정을 다시 계산한 값이다.
                    저장해두면 추이 그래프·기록 탭·진료 상세가 채워진다.
                    """)
    @PostMapping
    public ApiResponse<ReportResponse> save(@AuthenticationPrincipal Long userId,
                                            @Valid @RequestBody ReportUploadRequest request) {
        return ApiResponse.success(compatReportService.save(userId, request), "검사지가 저장되었습니다.");
    }
}
