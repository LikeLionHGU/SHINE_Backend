package com.shine.backend.domain.question.entity;

/** AI는 "물어볼 질문 문구"를 추천할 뿐, 의학적 답변을 생성하지 않는다. */
public enum QuestionSource {
    AI,
    USER
}
