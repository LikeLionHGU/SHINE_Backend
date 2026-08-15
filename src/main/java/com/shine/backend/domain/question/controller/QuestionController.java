package com.shine.backend.domain.question.controller;

import com.shine.backend.domain.question.dto.*;
import com.shine.backend.domain.question.service.QuestionService;
import com.shine.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Questions", description = "진료 때 물어볼 질문 메모")
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "질문 목록",
            description = "appointmentId 또는 testSheetId 중 하나로 조회한다.")
    @GetMapping
    public ApiResponse<List<QuestionResponse>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long appointmentId,
            @RequestParam(required = false) Long testSheetId) {

        if (appointmentId != null) {
            return ApiResponse.success(questionService.getByAppointment(userId, appointmentId));
        }
        return ApiResponse.success(questionService.getByTestSheet(userId, testSheetId));
    }

    @Operation(summary = "질문 등록",
            description = "홈·검사지·캘린더의 '질문 입력하기'가 모두 이 API를 쓴다. AI 답변은 생성하지 않는다.")
    @PostMapping
    public ApiResponse<QuestionResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody QuestionCreateRequest request) {
        return ApiResponse.success(questionService.create(userId, request), "질문이 등록되었습니다.");
    }

    @Operation(summary = "질문 수정 / 답변 기록")
    @PatchMapping("/{questionId}")
    public ApiResponse<QuestionResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionUpdateRequest request) {
        return ApiResponse.success(questionService.update(userId, questionId, request));
    }

    @Operation(summary = "질문 삭제")
    @DeleteMapping("/{questionId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId,
                                    @PathVariable Long questionId) {
        questionService.delete(userId, questionId);
        return ApiResponse.success();
    }
}
