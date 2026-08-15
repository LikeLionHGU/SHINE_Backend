package com.shine.backend.domain.appointment.controller;

import com.shine.backend.domain.appointment.dto.*;
import com.shine.backend.domain.appointment.service.AppointmentService;
import com.shine.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Calendar", description = "캘린더 · 병원 방문 일정")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "월 캘린더 조회",
            description = "날짜별 임신 주차와 마커를 준다. 마커의 visited가 true면 지난 일정(●), false면 예정(○).")
    @GetMapping("/calendar")
    public ApiResponse<CalendarResponse> getCalendar(
            @AuthenticationPrincipal Long userId,
            @RequestParam String yearMonth) {
        return ApiResponse.success(appointmentService.getCalendar(userId, yearMonth));
    }

    @Operation(summary = "예정된 방문 목록", description = "오늘 이후 일정만 시간순으로")
    @GetMapping("/appointments/upcoming")
    public ApiResponse<List<AppointmentResponse>> getUpcoming(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(appointmentService.getUpcoming(userId));
    }

    @Operation(summary = "일정 추가")
    @PostMapping("/appointments")
    public ApiResponse<AppointmentResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AppointmentCreateRequest request) {
        return ApiResponse.success(appointmentService.create(userId, request), "일정이 추가되었습니다.");
    }

    @Operation(summary = "일정 수정", description = "보낸 필드만 반영된다.")
    @PatchMapping("/appointments/{appointmentId}")
    public ApiResponse<AppointmentResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody AppointmentUpdateRequest request) {
        return ApiResponse.success(appointmentService.update(userId, appointmentId, request));
    }

    @Operation(summary = "일정 삭제")
    @DeleteMapping("/appointments/{appointmentId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId,
                                    @PathVariable Long appointmentId) {
        appointmentService.delete(userId, appointmentId);
        return ApiResponse.success();
    }
}
