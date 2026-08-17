package com.shine.backend.domain.home.controller;

import com.shine.backend.domain.home.dto.HomeResponse;
import com.shine.backend.domain.home.service.HomeService;
import com.shine.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "홈 화면")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "홈 화면 통합 조회",
            description = "검사지가 없으면 latestSheet가 null이고 questions·nutritions도 비어 있다. 화면은 빈 상태 문구를 띄우면 된다.")
    @GetMapping
    public ApiResponse<HomeResponse> getHome(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(homeService.getHome(userId));
    }
}
