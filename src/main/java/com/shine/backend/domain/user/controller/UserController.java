package com.shine.backend.domain.user.controller;

import com.shine.backend.domain.user.dto.*;
import com.shine.backend.domain.user.service.UserService;
import com.shine.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Users", description = "회원 정보 · 마이 화면")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회",
            description = "주수와 출산예정일은 저장값이 아니라 최종월경일로 매번 계산한 값이다.")
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMe(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMe(userId));
    }

    @Operation(summary = "프로필 · 연락처 · 이메일 수정", description = "보낸 필드만 반영된다.")
    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> update(@AuthenticationPrincipal Long userId,
                                                   @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(userId, request));
    }

    @Operation(summary = "임신 주차 수정", description = "pregnancyWeek 또는 lastPeriodDate 중 하나만 보낸다.")
    @PatchMapping("/me/pregnancy")
    public ApiResponse<UserProfileResponse> updatePregnancy(@AuthenticationPrincipal Long userId,
                                                            @Valid @RequestBody PregnancyUpdateRequest request) {
        return ApiResponse.success(userService.updatePregnancy(userId, request));
    }

    @Operation(summary = "설정 변경", description = "카메라 동의 · 알림 설정")
    @PatchMapping("/me/settings")
    public ApiResponse<UserProfileResponse.Settings> updateSettings(@AuthenticationPrincipal Long userId,
                                                                    @RequestBody SettingsUpdateRequest request) {
        return ApiResponse.success(userService.updateSettings(userId, request));
    }

    @Operation(summary = "회원 탈퇴", description = "의료 데이터라 즉시 삭제한다. 복구되지 않는다.")
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal Long userId,
                                      @RequestBody Map<String, String> body) {
        userService.withdraw(userId, body.get("password"));
        return ApiResponse.success();
    }
}
