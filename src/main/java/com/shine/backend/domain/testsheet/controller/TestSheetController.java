package com.shine.backend.domain.testsheet.controller;

import com.shine.backend.domain.testsheet.dto.*;
import com.shine.backend.domain.testsheet.service.TestSheetService;
import com.shine.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "TestSheets", description = "검사지 업로드 · 분석 · 조회")
@RestController
@RequestMapping("/api/v1/test-sheets")
@RequiredArgsConstructor
public class TestSheetController {

    private final TestSheetService testSheetService;

    @Operation(summary = "검사지 업로드 및 분석 시작",
            description = "즉시 202를 반환한다. 분석이 끝날 때까지 status를 2초 간격으로 폴링한다. 최대 5장.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TestSheetUploadResponse>> upload(
            @AuthenticationPrincipal Long userId,
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate testDate,
            @RequestParam(required = false) String hospitalName) {

        var response = testSheetService.upload(userId, files, testDate, hospitalName);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "분석을 시작했습니다."));
    }

    @Operation(summary = "검사지에 원본 사진 붙이기",
            description = """
                    이미 저장된 검사지에 사진만 추가한다. 필드명은 POST /test-sheets 와 같은 files.

                    프론트가 OCR을 직접 하는 경로(POST /reports)에서는 판정만 먼저 올라와서
                    검사지 원본이 비어 있다. 이 API로 사진을 붙이면 기록 탭·진료 상세에서
                    원본을 볼 수 있다.

                    분석은 다시 돌지 않는다. 이미 나온 판정과 근거를 그대로 둔다.
                    다시 올리면 기존 사진을 갈아끼운다. 최대 5장.
                    """)
    @PostMapping(value = "/{testSheetId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TestSheetDetailResponse> attachImages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long testSheetId,
            @RequestPart("files") List<MultipartFile> files) {
        return ApiResponse.success(
                testSheetService.attachImages(userId, testSheetId, files),
                "검사지 사진이 저장되었습니다.");
    }

    @Operation(summary = "분석 상태 폴링",
            description = "testDateConfirmRequired가 true면 검사일을 사용자에게 확인받아야 한다.")
    @GetMapping("/{testSheetId}/status")
    public ApiResponse<AnalysisStatusResponse> getStatus(@AuthenticationPrincipal Long userId,
                                                         @PathVariable Long testSheetId) {
        return ApiResponse.success(testSheetService.getStatus(userId, testSheetId));
    }

    @Operation(summary = "분석 결과 조회",
            description = "번역 화면과 기록 탭의 과거 검사지가 이 API를 함께 쓴다.")
    @GetMapping("/{testSheetId}")
    public ApiResponse<TestSheetDetailResponse> getDetail(@AuthenticationPrincipal Long userId,
                                                          @PathVariable Long testSheetId) {
        return ApiResponse.success(testSheetService.getDetail(userId, testSheetId));
    }

    @Operation(summary = "검사일 확정",
            description = "OCR이 날짜를 못 읽었을 때 사용자가 직접 선택한다.")
    @PatchMapping("/{testSheetId}/test-date")
    public ApiResponse<TestSheetDetailResponse> confirmTestDate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long testSheetId,
            @RequestBody Map<String, String> body) {
        return ApiResponse.success(
                testSheetService.confirmTestDate(userId, testSheetId, LocalDate.parse(body.get("testDate"))),
                "검사일이 저장되었습니다.");
    }

    @Operation(summary = "검사지 원본 이미지", description = "page는 1부터 시작한다.")
    @GetMapping("/{testSheetId}/images/{page}")
    public ResponseEntity<InputStreamResource> image(@AuthenticationPrincipal Long userId,
                                                     @PathVariable Long testSheetId,
                                                     @PathVariable int page) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new InputStreamResource(testSheetService.readImage(userId, testSheetId, page)));
    }

    @Operation(summary = "검사지 삭제", description = "이미지 파일과 검사값이 함께 지워진다.")
    @DeleteMapping("/{testSheetId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId,
                                    @PathVariable Long testSheetId) {
        testSheetService.delete(userId, testSheetId);
        return ApiResponse.success();
    }
}
