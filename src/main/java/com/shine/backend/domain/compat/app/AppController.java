package com.shine.backend.domain.compat.app;

import com.shine.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 프론트 client.ts 의 목 데이터를 그대로 대체하는 엔드포인트.
 * 응답 형태를 프론트 타입에 맞춰놨으므로 함수 본문만 fetch로 바꾸면 된다.
 */
@Tag(name = "App", description = "프론트 타입에 맞춘 응답")
@RestController
@RequestMapping("/api/v1/app")
@RequiredArgsConstructor
public class AppController {

    private final AppQueryService appQueryService;
    private final AppCalendarService appCalendarService;

    @Operation(summary = "마이 프로필", description = "client.ts getUserProfile 대체")
    @GetMapping("/me")
    public ApiResponse<AppDtos.UserProfile> getProfile(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(appQueryService.getProfile(userId));
    }

    @Operation(summary = "개인정보 수정",
            description = "PATCH /users/me 와 같은 일을 하되 응답이 GET /app/me 와 같은 모양이라 "
                    + "저장 후 프로필을 다시 조회할 필요가 없다. 보낸 칸만 바뀌고, 빈 문자열은 '지운다'는 뜻이다.")
    @PatchMapping("/me")
    public ApiResponse<AppDtos.UserProfile> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AppDtos.UserProfileUpdate request) {
        return ApiResponse.success(appQueryService.updateProfile(userId, request), "저장되었습니다.");
    }

    @Operation(summary = "기록 타임라인", description = "report.ts DEMO_RECORDS 대체")
    @GetMapping("/records")
    public ApiResponse<List<AppDtos.RecordEntry>> getRecords(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(appQueryService.getRecords(userId));
    }

    @Operation(summary = "분석 탭 지표 목록", description = "report.ts DEMO_TREND_INDICATORS 대체")
    @GetMapping("/trends")
    public ApiResponse<List<AppDtos.TrendIndicator>> getTrends(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(appQueryService.getTrends(userId));
    }

    @Operation(summary = "지표 추이 상세", description = "report.ts getTrendIndicator 대체. id는 항목 코드 소문자")
    @GetMapping("/trends/{id}")
    public ApiResponse<AppDtos.TrendIndicator> getTrend(@AuthenticationPrincipal Long userId,
                                                        @PathVariable String id) {
        return ApiResponse.success(appQueryService.getTrend(userId, id));
    }

    // ---------- 캘린더 ----------

    @Operation(summary = "등록한 일정 전체", description = "client.ts getVisits 대체")
    @GetMapping("/visits")
    public ApiResponse<List<AppDtos.CalendarVisit>> getVisits(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(appCalendarService.getVisits(userId));
    }

    @Operation(summary = "일정 추가·수정",
            description = "client.ts saveVisit 대체. id가 서버에 없으면 새로 만들고 응답에 서버 id를 담아준다.")
    @PostMapping("/visits")
    public ApiResponse<AppDtos.CalendarVisit> saveVisit(@AuthenticationPrincipal Long userId,
                                                        @RequestBody AppDtos.CalendarVisit request) {
        return ApiResponse.success(appCalendarService.saveVisit(userId, request));
    }

    @Operation(summary = "일정 삭제", description = "client.ts deleteVisit 대체")
    @DeleteMapping("/visits/{id}")
    public ApiResponse<Void> deleteVisit(@AuthenticationPrincipal Long userId,
                                         @PathVariable String id) {
        appCalendarService.deleteVisit(userId, id);
        return ApiResponse.success();
    }

    @Operation(summary = "캘린더 월 표시", description = "client.ts getCalendarMonthMarks 대체")
    @GetMapping("/calendar/marks")
    public ApiResponse<AppDtos.CalendarMonthMarks> getMonthMarks(
            @AuthenticationPrincipal Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.success(appCalendarService.getMonthMarks(userId, year, month));
    }

    @Operation(summary = "진료 상세", description = "client.ts getVisitDetail 대체. date는 YY.MM.DD")
    @GetMapping("/visits/{date}/detail")
    public ApiResponse<AppDtos.VisitDetail> getVisitDetail(@AuthenticationPrincipal Long userId,
                                                           @PathVariable String date) {
        return ApiResponse.success(appCalendarService.getVisitDetail(userId, date));
    }
}
