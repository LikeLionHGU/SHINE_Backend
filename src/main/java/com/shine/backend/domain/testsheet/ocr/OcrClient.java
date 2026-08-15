package com.shine.backend.domain.testsheet.ocr;

import java.util.List;

/**
 * OCR 엔진 추상화.
 *
 * 구현체를 갈아끼우면 엔진을 바꿀 수 있다.
 * 개발 중에는 픽스처를 쓰고, 나중에 실제 API 구현체를 붙인다.
 */
public interface OcrClient {

    OcrResult analyze(List<String> imageKeys);

    String engineName();
}
