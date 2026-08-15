package com.shine.backend.domain.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

/** 둘 중 하나만 보낸다. 주차만으로는 최대 6일 오차가 생기므로 최종월경일 쪽이 정확하다. */
public record PregnancyUpdateRequest(

        @Min(value = 1, message = "임신 주차는 1~42 사이여야 해요.")
        @Max(value = 42, message = "임신 주차는 1~42 사이여야 해요.")
        Integer pregnancyWeek,

        LocalDate lastPeriodDate
) {}
