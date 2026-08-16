package com.shine.backend.domain.testsheet.dto;

import com.shine.backend.domain.testsheet.entity.AnalysisStatus;

/** 업로드 직후 응답. 분석은 아직 진행 중이라 클라이언트는 status를 폴링한다. */
public record TestSheetUploadResponse(
        Long testSheetId,
        AnalysisStatus analysisStatus,
        int pollIntervalMs,
        int estimatedSeconds
) {
    public static TestSheetUploadResponse of(Long id, AnalysisStatus status) {
        return new TestSheetUploadResponse(id, status, 2000, 20);
    }
}
