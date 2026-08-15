package com.shine.backend.domain.appointment.service;

import com.shine.backend.domain.appointment.dto.*;
import com.shine.backend.domain.appointment.entity.Appointment;
import com.shine.backend.domain.appointment.entity.ScheduleSource;
import com.shine.backend.domain.appointment.entity.VisitStatus;
import com.shine.backend.domain.appointment.repository.AppointmentRepository;
import com.shine.backend.domain.question.repository.QuestionRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CalendarResponse getCalendar(Long userId, String yearMonth) {
        User user = findUser(userId);
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<Appointment> appointments = appointmentRepository
                .findByUserIdAndVisitAtBetweenOrderByVisitAt(
                        userId, ym.atDay(1).atStartOfDay(), ym.atEndOfMonth().atTime(23, 59, 59));

        // 날짜별로 묶어서 마커를 만든다
        Map<LocalDate, List<Appointment>> byDate = appointments.stream()
                .collect(Collectors.groupingBy(a -> a.getVisitAt().toLocalDate()));

        List<CalendarResponse.Day> days = new ArrayList<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            List<CalendarResponse.Marker> markers = byDate.getOrDefault(date, List.of()).stream()
                    .map(a -> new CalendarResponse.Marker(
                            a.getId(),
                            a.isObgyn(),
                            a.getVisitAt().isBefore(now),
                            // 산부인과는 라벨 없이 점만, 그 외는 제목을 함께 표시
                            a.isObgyn() ? null : a.getTitle()))
                    .toList();

            days.add(new CalendarResponse.Day(
                    date, user.getPregnancyWeek(date), date.equals(today), markers));
        }

        return new CalendarResponse(yearMonth, days, toResponses(appointments, now));
    }

    /** 하단 '예정된 방문' 목록 — 오늘 이후만 */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getUpcoming(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return toResponses(
                appointmentRepository.findByUserIdAndVisitAtAfterOrderByVisitAt(userId, now), now);
    }

    @Transactional
    public AppointmentResponse create(Long userId, AppointmentCreateRequest request) {
        User user = findUser(userId);

        Appointment appointment = Appointment.builder()
                .user(user)
                .title(request.title())
                .location(request.location())
                .visitAt(request.visitAt())
                // 그 시점의 주수를 스냅샷으로 남긴다
                .pregnancyWeek(user.getPregnancyWeek(request.visitAt().toLocalDate()))
                .obgyn(request.isObgyn())
                .visitStatus(VisitStatus.SCHEDULED)
                .createdBy(ScheduleSource.USER)
                .build();

        appointmentRepository.save(appointment);
        return AppointmentResponse.from(appointment, LocalDateTime.now(), 0);
    }

    @Transactional
    public AppointmentResponse update(Long userId, Long appointmentId, AppointmentUpdateRequest request) {
        Appointment appointment = findOwned(userId, appointmentId);
        User user = appointment.getUser();

        Integer newWeek = request.visitAt() == null
                ? null
                : user.getPregnancyWeek(request.visitAt().toLocalDate());

        appointment.update(request.title(), request.location(), request.visitAt(),
                request.isObgyn(), null, newWeek);

        return AppointmentResponse.from(appointment, LocalDateTime.now(),
                questionRepository.countByAppointmentId(appointmentId));
    }

    @Transactional
    public void delete(Long userId, Long appointmentId) {
        appointmentRepository.delete(findOwned(userId, appointmentId));
    }

    // ---------- 내부 ----------

    private List<AppointmentResponse> toResponses(List<Appointment> appointments, LocalDateTime now) {
        return appointments.stream()
                .map(a -> AppointmentResponse.from(
                        a, now, questionRepository.countByAppointmentId(a.getId())))
                .toList();
    }

    private Appointment findOwned(Long userId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
        // 남의 일정을 건드리지 못하게 막는다
        if (!appointment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return appointment;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
