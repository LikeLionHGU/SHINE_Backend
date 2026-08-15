package com.shine.backend.domain.auth.controller;

import com.shine.backend.domain.auth.dto.*;
import com.shine.backend.domain.auth.service.AuthService;
import com.shine.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth", description = "인증 · 회원가입 · 토큰")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "아이디 중복 확인",
            description = "회원가입 화면에서 아이디 입력 필드를 벗어날 때 호출한다. 최종 검증은 회원가입에서 다시 한다.")
    @GetMapping("/check-login-id")
    public ApiResponse<Map<String, Boolean>> checkLoginId(@RequestParam String loginId) {
        boolean available = authService.isLoginIdAvailable(loginId);
        return ApiResponse.success(Map.of("available", available));
    }

    @Operation(summary = "회원가입",
            description = "임신 주차를 받지만 저장하지 않는다. 서버가 최종월경일로 역산해 보관한다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다."));
    }

    @Operation(summary = "로그인",
            description = "autoLogin이 true일 때만 리프레시 토큰을 함께 발급한다.")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "로그인에 성공했습니다.");
    }

    @Operation(summary = "토큰 재발급",
            description = "Rotation 적용. 이미 사용된 리프레시가 다시 들어오면 해당 사용자의 토큰을 전부 무효화한다.")
    @PostMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(@RequestBody Map<String, String> body) {
        return ApiResponse.success(authService.reissue(body.get("refreshToken")), "토큰이 재발급되었습니다.");
    }

    @Operation(summary = "로그아웃",
            description = "리프레시를 삭제하고 액세스 토큰을 남은 만료 시간만큼 블랙리스트에 등록한다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Long userId,
                                    HttpServletRequest request) {
        // Authorize 버튼으로 넣은 헤더를 그대로 읽는다. 별도 파라미터로 받지 않는다.
        String header = request.getHeader("Authorization");
        authService.logout(userId, header == null ? "" : header.replace("Bearer ", "").trim());
        return ApiResponse.success();
    }
}
