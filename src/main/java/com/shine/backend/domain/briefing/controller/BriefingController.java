package com.shine.backend.domain.briefing.controller;

import com.shine.backend.domain.briefing.dto.BriefingResponse;
import com.shine.backend.domain.briefing.service.BriefingService;
import com.shine.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Briefing", description = "진료 상세 · 3초 닥터 브리핑")
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class BriefingController {

    private final BriefingService briefingService;

    @Operation(summary = "진료 상세",
            description = "진료일 기준으로 당일 검사지와 이전 검사지를 나눠 준다. 질문은 일정과 검사지 양쪽에서 모은다.")
    @GetMapping("/{appointmentId}/briefing")
    public ApiResponse<BriefingResponse> getBriefing(@AuthenticationPrincipal Long userId,
                                                     @PathVariable Long appointmentId) {
        return ApiResponse.success(briefingService.getBriefing(userId, appointmentId));
    }
}
