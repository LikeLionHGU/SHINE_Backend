package com.shine.backend.domain.question.repository;

import com.shine.backend.domain.question.entity.Question;
import com.shine.backend.domain.question.entity.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByUserIdOrderByIdDesc(Long userId);

    List<Question> findByUserIdAndQuestionStatusOrderByIdDesc(Long userId, QuestionStatus status);

    List<Question> findByTestSheetIdOrderByIdDesc(Long testSheetId);

    /** 캘린더에서 일정 토글을 내렸을 때 보여줄 질문들 */
    List<Question> findByAppointmentIdOrderByIdDesc(Long appointmentId);

    long countByAppointmentId(Long appointmentId);
}
