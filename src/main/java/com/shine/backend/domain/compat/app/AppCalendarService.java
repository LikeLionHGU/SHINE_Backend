package com.shine.backend.domain.compat.app;

import com.shine.backend.domain.appointment.entity.Appointment;
import com.shine.backend.domain.appointment.entity.ScheduleSource;
import com.shine.backend.domain.appointment.entity.VisitStatus;
import com.shine.backend.domain.appointment.repository.AppointmentRepository;
import com.shine.backend.domain.question.entity.Question;
import com.shine.backend.domain.question.entity.QuestionSource;
import com.shine.backend.domain.question.entity.QuestionStatus;
import com.shine.backend.domain.question.repository.QuestionRepository;
import com.shine.backend.domain.testsheet.entity.AnalysisStatus;
import com.shine.backend.domain.testsheet.entity.TestSheet;
import com.shine.backend.domain.testsheet.repository.TestSheetRepository;
import com.shine.backend.domain.user.entity.User;
import com.shine.backend.domain.user.repository.UserRepository;
import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 프론트 client.ts 의 캘린더 함수들을 대체한다.
 * 날짜는 "YY.MM.DD", 시간은 meridiem/hour/minute 로 프론트 형식에 맞춘다.
 */
@Service
@RequiredArgsConstructor
public class AppCalendarService {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("yy.MM.dd");
    private static final DateTimeFormatter REPORT_DATE = DateTimeFormatter.ofPattern("yyyy. MM. dd");

    private final AppointmentRepository appointmentRepository;
    private final QuestionRepository questionRepository;
    private final TestSheetRepository testSheetRepository;
    private final UserRepository userRepository;

    // ---------- 일정 목록 ----------

    @Transactional(readOnly = true)
    public List<AppDtos.CalendarVisit> getVisits(Long userId) {
        return appointmentRepository
                .findByUserIdAndVisitAtBetweenOrderByVisitAt(
                        userId,
                        LocalDateTime.now().minusYears(2),
                        LocalDateTime.now().plusYears(2))
                .stream()
                .map(this::toVisit)
                .toList();
    }

    @Transactional
    public AppDtos.CalendarVisit saveVisit(Long userId, AppDtos.CalendarVisit request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime visitAt = toDateTime(request.date(), request.meridiem(),
                request.hour(), request.minute());

        Appointment appointment = findExisting(userId, request.id());

        if (appointment == null) {
            appointment = Appointment.builder()
                    .user(user)
                    .title(nonBlank(request.title(), "병원 방문"))
                    .location(request.place())
                    .visitAt(visitAt)
                    .pregnancyWeek(user.getPregnancyWeek(visitAt.toLocalDate()))
                    .obgyn(request.isHospital())
                    .visitStatus(VisitStatus.SCHEDULED)
                    .createdBy(ScheduleSource.USER)
                    .clientId(request.id() == null || request.id().isBlank() ? null : request.id().trim())
                    .build();
            appointmentRepository.save(appointment);
        } else {
            appointment.update(nonBlank(request.title(), null), request.place(), visitAt,
                    request.isHospital(), null, user.getPregnancyWeek(visitAt.toLocalDate()));
        }

        replaceQuestions(user, appointment, request.questions());
        return toVisit(appointment);
    }

    @Transactional
    public void deleteVisit(Long userId, String id) {
        Appointment appointment = findExisting(userId, id);
        if (appointment != null) appointmentRepository.delete(appointment);
    }

    // ---------- 캘린더 마커 ----------

    /**
     * 프론트의 DayMark 는 두 종류다.
     *   uploaded  검사지를 올린 날
     *   scheduled 일정만 잡혀 있는 날
     * 같은 날에 둘 다 있으면 검사지 쪽을 우선한다.
     */
    @Transactional(readOnly = true)
    public AppDtos.CalendarMonthMarks getMonthMarks(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        Map<Integer, String> marks = new HashMap<>();
        Map<Integer, String> labels = new HashMap<>();

        appointmentRepository.findByUserIdAndVisitAtBetweenOrderByVisitAt(
                        userId, ym.atDay(1).atStartOfDay(), ym.atEndOfMonth().atTime(23, 59, 59))
                .forEach(a -> {
                    int day = a.getVisitAt().getDayOfMonth();
                    marks.put(day, "scheduled");
                    if (!a.isObgyn()) labels.put(day, a.getTitle());
                });

        testSheetRepository.findByUserIdOrderByIdDesc(userId,
                        org.springframework.data.domain.PageRequest.of(0, 200))
                .stream()
                .filter(s -> s.getAnalysisStatus() == AnalysisStatus.DONE)
                .filter(s -> YearMonth.from(s.getTestDate()).equals(ym))
                .forEach(s -> marks.put(s.getTestDate().getDayOfMonth(), "uploaded"));

        return new AppDtos.CalendarMonthMarks(marks, labels);
    }

    // ---------- 진료 상세 ----------

    @Transactional(readOnly = true)
    public AppDtos.VisitDetail getVisitDetail(Long userId, String date) {
        LocalDate visitDate = parseShortDate(date);

        TestSheet today = testSheetRepository
                .findFirstByUserIdAndAnalysisStatusAndTestDateOrderByIdDesc(
                        userId, AnalysisStatus.DONE, visitDate)
                .orElse(null);

        TestSheet previous = testSheetRepository
                .findFirstByUserIdAndAnalysisStatusAndTestDateLessThanOrderByTestDateDescIdDesc(
                        userId, AnalysisStatus.DONE, visitDate)
                .orElse(null);

        List<String> suggested = new ArrayList<>();
        List<String> written = new ArrayList<>();

        for (TestSheet sheet : List.of(today, previous).stream().filter(Objects::nonNull).toList()) {
            for (Question q : questionRepository.findByTestSheetIdOrderByIdDesc(sheet.getId())) {
                if (!q.getUser().getId().equals(userId)) continue;
                // AI는 "물어볼 질문"을 추천한 것이고, USER는 직접 적은 것이다
                if (q.getCreatedBy() == QuestionSource.AI) suggested.add(q.getContent());
                else written.add(q.getContent());
            }
        }

        return new AppDtos.VisitDetail(
                toReport(today), toReport(previous), suggested, written);
    }

    // ---------- 변환 ----------

    private AppDtos.CalendarVisit toVisit(Appointment a) {
        LocalDateTime at = a.getVisitAt();
        int hour24 = at.getHour();
        int hour12 = hour24 % 12 == 0 ? 12 : hour24 % 12;

        List<String> questions = questionRepository
                .findByAppointmentIdOrderByIdDesc(a.getId()).stream()
                .map(Question::getContent).toList();

        return new AppDtos.CalendarVisit(
                // 프론트가 만든 id가 있으면 그걸 돌려준다. 프론트가 id를 갈아끼울 필요가 없다.
                a.getClientId() != null ? a.getClientId() : String.valueOf(a.getId()),
                at.toLocalDate().format(SHORT),
                a.getTitle(),
                a.getLocation() == null ? "" : a.getLocation(),
                hour24 < 12 ? "AM" : "PM",
                hour12,
                at.getMinute(),
                a.isObgyn(),
                questions);
    }

    private AppDtos.Report toReport(TestSheet sheet) {
        if (sheet == null) return null;
        String url = sheet.getImageKeys() == null || sheet.getImageKeys().isEmpty()
                ? null
                : "/api/v1/test-sheets/%d/images/1".formatted(sheet.getId());
        return new AppDtos.Report(sheet.getTestDate().format(REPORT_DATE), url);
    }

    private Appointment findExisting(Long userId, String id) {
        if (id == null || id.isBlank()) return null;
        String key = id.trim();

        // 프론트가 만든 id("visit-2026-08-16")로 먼저 찾는다
        Appointment byClientId = appointmentRepository.findByUserIdAndClientId(userId, key).orElse(null);
        if (byClientId != null) return byClientId;

        // 서버 id(숫자)로도 찾을 수 있게 둔다
        try {
            return appointmentRepository.findById(Long.parseLong(key))
                    .filter(a -> a.getUser().getId().equals(userId))
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 일정에 붙은 질문을 통째로 갈아끼운다. 프론트가 배열로 통으로 보내기 때문이다. */
    private void replaceQuestions(User user, Appointment appointment, List<String> contents) {
        if (contents == null) return;

        questionRepository.deleteAll(
                questionRepository.findByAppointmentIdOrderByIdDesc(appointment.getId()));

        questionRepository.saveAll(contents.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(c -> Question.builder()
                        .user(user)
                        .appointment(appointment)
                        .content(c.length() > 500 ? c.substring(0, 500) : c)
                        .createdBy(QuestionSource.USER)
                        .questionStatus(QuestionStatus.PENDING)
                        .includeInBriefing(true)
                        .build())
                .toList());
    }

    private LocalDateTime toDateTime(String date, String meridiem, int hour, int minute) {
        int hour24 = "PM".equalsIgnoreCase(meridiem)
                ? (hour % 12) + 12
                : (hour % 12);
        return parseShortDate(date).atTime(hour24, minute);
    }

    /** "26.08.24" 와 "2026-08-24" 를 모두 받는다 */
    private LocalDate parseShortDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "날짜가 필요합니다.");
        }
        String value = raw.trim();
        try {
            return value.matches("\\d{2}\\.\\d{2}\\.\\d{2}")
                    ? LocalDate.parse(value, SHORT)
                    : LocalDate.parse(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "날짜 형식이 올바르지 않습니다.");
        }
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
