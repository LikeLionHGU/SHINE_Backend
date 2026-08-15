package com.shine.backend.domain.appointment.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 부분 수정. 보낸 필드만 반영된다. */
public record AppointmentUpdateRequest(
        @Size(max = 50) String title,
        @Size(max = 100) String location,
        LocalDateTime visitAt,
        Boolean isObgyn
) {}
