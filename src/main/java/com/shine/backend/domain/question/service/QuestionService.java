package com.shine.backend.domain.question.service;

import com.shine.backend.domain.appointment.entity.Appointment;
import com.shine.backend.domain.appointment.repository.AppointmentRepository;
import com.shine.backend.domain.question.dto.*;
import com.shine.backend.domain.question.entity.Question;
import com.shine.backend.domain.question.entity.QuestionSource;
import com.shine.backend.domain.question.entity.QuestionStatus;
import com.shine.backend.domain.question.repository.QuestionRepository;
import com.shine.backend.domain.testsheet.entity.TestSheet;
import com.shine.backend.domain.testsheet.repository.TestSheetRepository;
import com.shine.backend.domain.user.entity.User;
import com.shine.backend.domain.user.repository.UserRepository;
import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final AppointmentRepository appointmentRepository;
    private final TestSheetRepository testSheetRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<QuestionResponse> getByAppointment(Long userId, Long appointmentId) {
        return questionRepository.findByAppointmentIdOrderByIdDesc(appointmentId).stream()
                .filter(q -> q.getUser().getId().equals(userId))
                .map(QuestionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getByTestSheet(Long userId, Long testSheetId) {
        return questionRepository.findByTestSheetIdOrderByIdDesc(testSheetId).stream()
                .filter(q -> q.getUser().getId().equals(userId))
                .map(QuestionResponse::from)
                .toList();
    }

    @Transactional
    public QuestionResponse create(Long userId, QuestionCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Appointment appointment = null;
        if (request.appointmentId() != null) {
            appointment = appointmentRepository.findById(request.appointmentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
            if (!appointment.getUser().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }

        TestSheet testSheet = null;
        if (request.testSheetId() != null) {
            testSheet = testSheetRepository.findById(request.testSheetId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SHEET_NOT_FOUND));
            if (!testSheet.getUser().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.SHEET_NOT_OWNED);
            }
        }

        Question question = Question.builder()
                .user(user)
                .testSheet(testSheet)
                .appointment(appointment)
                .content(request.content())
                .createdBy(QuestionSource.USER)
                .questionStatus(QuestionStatus.PENDING)
                .includeInBriefing(true)
                .build();

        questionRepository.save(question);
        return QuestionResponse.from(question);
    }

    @Transactional
    public QuestionResponse update(Long userId, Long questionId, QuestionUpdateRequest request) {
        Question question = findOwned(userId, questionId);
        question.update(request.doctorAnswer(), request.questionStatus(), null);
        return QuestionResponse.from(question);
    }

    @Transactional
    public void delete(Long userId, Long questionId) {
        questionRepository.delete(findOwned(userId, questionId));
    }

    private Question findOwned(Long userId, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        if (!question.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return question;
    }
}
