package com.shine.backend.domain.appointment.repository;

import com.shine.backend.domain.appointment.entity.Appointment;
import com.shine.backend.domain.appointment.entity.ScheduleSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /** 캘린더 월 조회 */
    List<Appointment> findByUserIdAndVisitAtBetweenOrderByVisitAt(
            Long userId, LocalDateTime from, LocalDateTime to);

    /** 예정된 방문 — 오늘 이후만 */
    List<Appointment> findByUserIdAndVisitAtAfterOrderByVisitAt(Long userId, LocalDateTime now);

    /**
     * 주차 변경 시 재생성 대상.
     * SYSTEM이 만든 미래 일정만 지운다 — 사용자가 직접 만들거나 수정한 건 건드리지 않는다.
     */
    List<Appointment> findByUserIdAndCreatedByAndVisitAtAfter(
            Long userId, ScheduleSource createdBy, LocalDateTime now);

    /** 프론트가 만든 id로 찾는다 */
    Optional<Appointment> findByUserIdAndClientId(Long userId, String clientId);

    /** 소유자 무관 조회. 삭제 시 404와 403을 구분하기 위함 */
    Optional<Appointment> findByClientId(String clientId);
}
