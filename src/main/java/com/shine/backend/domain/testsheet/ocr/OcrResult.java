package com.shine.backend.domain.testsheet.ocr;

import java.time.LocalDate;
import java.util.List;

/**
 * @param testDate 검사지에서 읽어낸 검사일. 못 읽으면 null이며,
 *                 이 경우 사용자에게 날짜를 직접 확인받아야 한다.
 *                 검사일이 틀리면 추이 그래프의 X축이 통째로 어긋난다.
 * @param rawJson  OCR 응답 원본(설계결정③). 파서를 고친 뒤 재파싱하는 근거가 된다.
 */
public record OcrResult(
        LocalDate testDate,
        String hospitalName,
        List<OcrRow> rows,
        String rawJson,
        String engine
) {}
