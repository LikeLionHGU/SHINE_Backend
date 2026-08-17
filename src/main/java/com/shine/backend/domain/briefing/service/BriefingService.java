package com.shine.backend.domain.briefing.service;

import com.shine.backend.domain.appointment.entity.Appointment;
import com.shine.backend.domain.appointment.repository.AppointmentRepository;
import com.shine.backend.domain.briefing.dto.BriefingResponse;
import com.shine.backend.domain.question.dto.QuestionResponse;
import com.shine.backend.domain.question.entity.Question;
import com.shine.backend.domain.question.repository.QuestionRepository;
import com.shine.backend.domain.testsheet.entity.AnalysisStatus;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import com.shine.backend.domain.testsheet.entity.TestResult;
import com.shine.backend.domain.testsheet.entity.TestSheet;
import com.shine.backend.domain.testsheet.repository.TestResultRepository;
import com.shine.backend.domain.testsheet.repository.TestSheetRepository;
import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BriefingService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy. MM. dd");
    private static final DateTimeFormatter SHEET_FMT = DateTimeFormatter.ofPattern("yyyy. MM. dd");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);

    private static final String EMPTY_MESSAGE = "검사지 업로드 후 관련 질문을 확인하실 수 있습니다";

    private final AppointmentRepository appointmentRepository;
    private final TestSheetRepository testSheetRepository;
    private final TestResultRepository testResultRepository;
    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public BriefingResponse getBriefing(Long userId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));

        if (!appointment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        LocalDate visitDate = appointment.getVisitAt().toLocalDate();

        // 당일 검사지 — 진료를 아직 안 받았으면 없다
        TestSheet today = testSheetRepository
                .findFirstByUserIdAndAnalysisStatusAndTestDateOrderByIdDesc(
                        userId, AnalysisStatus.DONE, visitDate)
                .orElse(null);

        // 이전 검사지 — 수치를 비교할 기준
        TestSheet previous = testSheetRepository
                .findFirstByUserIdAndAnalysisStatusAndTestDateLessThanOrderByTestDateDescIdDesc(
                        userId, AnalysisStatus.DONE, visitDate)
                .orElse(null);

        List<QuestionResponse> questions = collectQuestions(userId, appointmentId, today, previous);

        return new BriefingResponse(
                appointment.getId(),
                appointment.getTitle(),
                appointment.getLocation(),
                appointment.getVisitAt(),
                appointment.getVisitAt().format(DATE_FMT),
                appointment.getVisitAt().format(TIME_FMT),
                appointment.isObgyn(),
                appointment.getVisitAt().isBefore(LocalDateTime.now()),
                appointment.getPregnancyWeek(),
                summarize(today),
                summarize(previous),
                questions,
                questions.isEmpty() && today == null && previous == null ? EMPTY_MESSAGE : null);
    }

    /**
     * 이 진료에 직접 붙은 질문과, 관련 검사지를 보고 생긴 질문을 함께 보여준다.
     * 진료 전이라 당일 검사지가 없어도 이전 검사지 질문은 물어볼 수 있어야 한다.
     */
    private List<QuestionResponse> collectQuestions(Long userId, Long appointmentId,
                                                    TestSheet today, TestSheet previous) {
        // 중복을 없애면서 순서를 유지한다
        LinkedHashMap<Long, Question> merged = new LinkedHashMap<>();

        questionRepository.findByAppointmentIdOrderByIdDesc(appointmentId)
                .forEach(q -> merged.put(q.getId(), q));

        for (TestSheet sheet : sheetsOf(today, previous)) {
            questionRepository.findByTestSheetIdOrderByIdDesc(sheet.getId())
                    .forEach(q -> merged.putIfAbsent(q.getId(), q));
        }

        return merged.values().stream()
                .filter(q -> q.getUser().getId().equals(userId))
                .map(QuestionResponse::from)
                .toList();
    }

    private List<TestSheet> sheetsOf(TestSheet today, TestSheet previous) {
        List<TestSheet> sheets = new ArrayList<>();
        if (today != null) sheets.add(today);
        if (previous != null) sheets.add(previous);
        return sheets;
    }

    private BriefingResponse.SheetSummary summarize(TestSheet sheet) {
        if (sheet == null) return null;

        List<TestResult> results = testResultRepository.findByTestSheetId(sheet.getId());
        long danger = count(results, ResultStatus.DANGER);
        long caution = count(results, ResultStatus.CAUTION);

        return new BriefingResponse.SheetSummary(
                sheet.getId(),
                sheet.getTestDate(),
                sheet.getTestDate().format(SHEET_FMT),
                sheet.getPregnancyWeek(),
                preview(sheet, danger, caution),
                danger,
                caution,
                results.size());
    }

    private String preview(TestSheet sheet, long danger, long caution) {
        if (sheet.getSummaryForMom() != null && !sheet.getSummaryForMom().isBlank()) {
            return sheet.getSummaryForMom();
        }
        if (danger > 0) return "확인이 필요한 항목이 %d개 있어요.".formatted(danger);
        if (caution > 0) return "주의해서 볼 항목이 %d개 있어요.".formatted(caution);
        return "모든 항목이 정상 범위 안에 있어요.";
    }

    private long count(List<TestResult> results, ResultStatus status) {
        return results.stream().filter(r -> r.getResultStatus() == status).count();
    }
}
