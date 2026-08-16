package com.shine.backend.domain.testsheet.service;

import com.shine.backend.domain.testsheet.dto.*;
import com.shine.backend.domain.testsheet.entity.AnalysisStatus;
import com.shine.backend.domain.testsheet.entity.TestResult;
import com.shine.backend.domain.testsheet.entity.TestSheet;
import com.shine.backend.domain.testsheet.repository.TestResultRepository;
import com.shine.backend.domain.testsheet.repository.TestSheetRepository;
import com.shine.backend.domain.user.entity.User;
import com.shine.backend.domain.user.repository.UserRepository;
import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import com.shine.backend.global.storage.FileStorage;
import com.shine.backend.domain.testsheet.event.TestSheetUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestSheetService {

    private static final int MAX_FILES = 5;

    private final TestSheetRepository testSheetRepository;
    private final TestResultRepository testResultRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 이미지를 저장하고 분석을 예약한다. 분석을 기다리지 않고 바로 응답한다.
     * 10~30초를 요청 스레드에서 붙잡고 있으면 안 된다.
     */
    @Transactional
    public TestSheetUploadResponse upload(Long userId, List<MultipartFile> files,
                                          LocalDate testDate, String hospitalName) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "검사지 이미지를 올려주세요.");
        }
        if (files.size() > MAX_FILES) {
            throw new BusinessException(ErrorCode.FILE_COUNT_EXCEEDED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<String> imageKeys = fileStorage.storeAll(files, userId);

        // 날짜를 안 주면 일단 오늘로 두고 '확인 안 됨'으로 표시한다.
        // OCR이 읽어내면 덮어쓰고, 못 읽으면 사용자에게 물어본다.
        LocalDate effectiveDate = testDate != null ? testDate : LocalDate.now();

        TestSheet sheet = TestSheet.builder()
                .user(user)
                .testDate(effectiveDate)
                .testDateConfirmed(testDate != null)
                .pregnancyWeek(user.getPregnancyWeek(effectiveDate))
                .hospitalName(hospitalName)
                .imageKeys(imageKeys)
                .analysisStatus(AnalysisStatus.WAITING)
                .piiMasked(false)
                .build();

        testSheetRepository.save(sheet);

        // 직접 호출하면 아직 커밋되지 않은 상태에서 다른 스레드가 조회하게 된다.
        // 이벤트로 넘겨 커밋 이후에 분석이 시작되도록 한다.
        eventPublisher.publishEvent(new TestSheetUploadedEvent(sheet.getId()));

        return TestSheetUploadResponse.of(sheet.getId(), sheet.getAnalysisStatus());
    }

    @Transactional(readOnly = true)
    public AnalysisStatusResponse getStatus(Long userId, Long testSheetId) {
        return AnalysisStatusResponse.from(findOwned(userId, testSheetId));
    }

    @Transactional(readOnly = true)
    public TestSheetDetailResponse getDetail(Long userId, Long testSheetId) {
        TestSheet sheet = findOwned(userId, testSheetId);

        if (sheet.getAnalysisStatus() == AnalysisStatus.WAITING
                || sheet.getAnalysisStatus() == AnalysisStatus.ANALYZING) {
            throw new BusinessException(ErrorCode.ANALYSIS_NOT_DONE);
        }

        return TestSheetDetailResponse.of(sheet, testResultRepository.findBySheetWithItem(testSheetId));
    }

    /**
     * OCR이 날짜를 못 읽었을 때 사용자가 직접 확정한다.
     * test_results의 test_date는 의도적 복사본이라 함께 갱신해야 한다.
     */
    @Transactional
    public TestSheetDetailResponse confirmTestDate(Long userId, Long testSheetId, LocalDate testDate) {
        TestSheet sheet = findOwned(userId, testSheetId);

        int week = sheet.getUser().getPregnancyWeek(testDate);
        sheet.confirmTestDate(testDate, week);

        List<TestResult> results = testResultRepository.findByTestSheetId(testSheetId);
        results.forEach(r -> r.applySheetDate(testDate, week));

        return TestSheetDetailResponse.of(sheet, results);
    }

    @Transactional
    public void delete(Long userId, Long testSheetId) {
        TestSheet sheet = findOwned(userId, testSheetId);
        List<String> keys = sheet.getImageKeys();
        testSheetRepository.delete(sheet);
        if (keys != null) keys.forEach(fileStorage::delete);
    }

    @Transactional(readOnly = true)
    public InputStream readImage(Long userId, Long testSheetId, int page) {
        TestSheet sheet = findOwned(userId, testSheetId);
        List<String> keys = sheet.getImageKeys();

        if (keys == null || page < 1 || page > keys.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "해당 페이지가 없습니다.");
        }
        return fileStorage.read(keys.get(page - 1));
    }

    private TestSheet findOwned(Long userId, Long testSheetId) {
        TestSheet sheet = testSheetRepository.findById(testSheetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHEET_NOT_FOUND));
        if (!sheet.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.SHEET_NOT_OWNED);
        }
        return sheet;
    }
}
