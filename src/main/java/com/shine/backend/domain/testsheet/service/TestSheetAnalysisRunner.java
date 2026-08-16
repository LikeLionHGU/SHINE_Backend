package com.shine.backend.domain.testsheet.service;

import com.shine.backend.domain.testsheet.analyzer.AnalyzedRow;
import com.shine.backend.domain.testsheet.analyzer.TestSheetAnalyzer;
import com.shine.backend.domain.testsheet.entity.TestResult;
import com.shine.backend.domain.testsheet.entity.TestSheet;
import com.shine.backend.domain.testsheet.event.TestSheetUploadedEvent;
import com.shine.backend.domain.testsheet.ocr.OcrClient;
import com.shine.backend.domain.testsheet.ocr.OcrResult;
import com.shine.backend.domain.testsheet.repository.TestResultRepository;
import com.shine.backend.domain.testsheet.repository.TestSheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 검사지 분석을 백그라운드에서 돌린다.
 *
 * 순서: OCR → 파싱·매칭·판정 → 저장
 * 이 단계까지 AI는 개입하지 않는다. LLM 설명 생성은 나중에 뒤에 붙인다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestSheetAnalysisRunner {

    private final TestSheetRepository testSheetRepository;
    private final TestResultRepository testResultRepository;
    private final TestSheetAnalyzer analyzer;
    private final OcrClient ocrClient;

    /**
     * 업로드 트랜잭션이 커밋된 뒤에 실행된다.
     * AFTER_COMMIT이 아니면 아직 저장되지 않은 검사지를 조회하게 된다.
     */
    @Async("analysisExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUploaded(TestSheetUploadedEvent event) {
        Long testSheetId = event.testSheetId();
        TestSheet sheet = testSheetRepository.findById(testSheetId).orElse(null);
        if (sheet == null) {
            log.warn("분석 대상 검사지가 없습니다 id={}", testSheetId);
            return;
        }

        try {
            sheet.markAnalyzing();
            testSheetRepository.saveAndFlush(sheet);

            OcrResult ocr = ocrClient.analyze(sheet.getImageKeys());

            // 설계결정③ — 파서를 고친 뒤 재파싱할 수 있도록 원본을 통째로 남긴다
            sheet.saveOcrRaw(ocr.rawJson(), ocr.engine(), true);
            sheet.applyHospitalName(ocr.hospitalName());

            // 사용자가 날짜를 주지 않았고 OCR이 읽어냈다면 그 값을 쓴다
            if (!sheet.isTestDateConfirmed() && ocr.testDate() != null) {
                sheet.applyOcrTestDate(ocr.testDate(),
                        sheet.getUser().getPregnancyWeek(ocr.testDate()));
            }

            List<AnalyzedRow> rows = analyzer.analyze(ocr);
            if (rows.isEmpty()) {
                sheet.markFailed("NO_ITEM_MATCHED");
                return;
            }

            testResultRepository.saveAll(rows.stream().map(row -> toEntity(sheet, row)).toList());

            // LLM 설명은 아직 붙이지 않았다. 판정과 수치만으로도 화면은 채워진다.
            sheet.markDone(null, null, null, null);

            log.info("검사지 분석 완료 id={} 항목={}", testSheetId, rows.size());

        } catch (Exception e) {
            log.error("검사지 분석 실패 id={}", testSheetId, e);
            sheet.markFailed("OCR_ENGINE_ERROR");
        }
    }

    private TestResult toEntity(TestSheet sheet, AnalyzedRow row) {
        return TestResult.builder()
                .testSheet(sheet)
                .testItem(row.item())
                // 설계결정⑧ — 추이 조회를 조인 없이 끝내기 위한 의도적 복사
                .user(sheet.getUser())
                .testDate(sheet.getTestDate())
                .pregnancyWeek(sheet.getPregnancyWeek())
                .ocrLabel(row.ocrLabel())
                .ocrCategory(row.ocrCategory())
                .rawValue(row.rawValue())
                .resultType(row.resultType())
                .numberValue(row.numberValue())
                .textValue(row.textValue())
                .unit(row.unit())
                .unitRaw(row.unitRaw())
                .sheetNormalMin(row.sheetNormalMin())
                .sheetNormalMax(row.sheetNormalMax())
                .sheetNormalText(row.sheetNormalText())
                .normalRangeSource(row.normalRangeSource())
                .resultStatus(row.resultStatus())
                .sheetVerdict(row.sheetVerdict())
                .verdictMismatch(row.verdictMismatch())
                .editedByUser(false)
                .build();
    }
}
