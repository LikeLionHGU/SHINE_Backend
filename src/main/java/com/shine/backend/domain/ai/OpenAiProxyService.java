package com.shine.backend.domain.ai;

import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 검사지 OCR·요약 생성을 서버가 대신 호출한다.
 *
 * 지금까지는 앱이 OpenAI를 직접 불렀고, 키가 EXPO_PUBLIC_OPENAI_API_KEY 로 번들에 박혀
 * 나갔다. 배포하면 누구나 꺼내 쓸 수 있다(전달사항 7번).
 *
 * 프론트 프롬프트·응답 스키마를 그대로 통과시키는 얇은 프록시다. 요청 본문을 해석하지 않고
 * 넘기므로 프롬프트가 바뀌어도 서버를 고칠 일이 없다. 대신 세 가지만 강제한다.
 *   ① 모델 화이트리스트  — 아무 모델이나 부르지 못하게
 *   ② max_tokens 상한   — 한 번에 나갈 수 있는 요금을 묶어두려고
 *   ③ 사용자별 일일 한도 — 키가 서버에 있어도 로그인만 하면 쓸 수 있으니 막아야 한다
 *
 * 응답은 OpenAI 원문 그대로 돌려준다. ApiResponse 로 감싸면 프론트가 파싱 코드를
 * 통째로 고쳐야 한다.
 */
@Slf4j
@Service
public class OpenAiProxyService {

    private static final String QUOTA_PREFIX = "openai:quota:";

    private final OpenAiProperties properties;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    /**
     * RestClient.Builder 는 자동 설정 빈이 아니라서 주입받을 수 없다.
     * (RestTemplateBuilder 와 다르다.) 여기서 직접 만든다.
     */
    public OpenAiProxyService(OpenAiProperties properties,
                              StringRedisTemplate redis,
                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.redis = redis;
        this.objectMapper = objectMapper;

        // 비전 모델은 검사지 한 장에 30초 넘게 걸리기도 한다. 기본 타임아웃으로는 끊긴다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    public String chatCompletions(Long userId, Map<String, Object> body) {
        if (!properties.isConfigured()) {
            log.error("OPENAI_API_KEY 가 설정되지 않았다");
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }

        Map<String, Object> sanitized = sanitize(body);
        String payload = serialize(sanitized);

        if (payload.getBytes().length > properties.maxRequestBytes()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "이미지가 너무 커요. 사진을 줄여서 다시 시도해주세요.");
        }

        consumeQuota(userId);

        try {
            return restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            // OpenAI 응답에는 우리 키가 섞여 나올 수 있으므로 사용자에게 그대로 노출하지 않는다
            log.error("OpenAI 호출 실패 userId={} model={}", userId, sanitized.get("model"), e);
            throw new BusinessException(ErrorCode.AI_REQUEST_FAILED);
        }
    }

    /**
     * 프론트가 보낸 본문에서 바꿀 것만 바꾸고 나머지는 그대로 둔다.
     * messages·response_format·temperature 는 손대지 않는다 — 프롬프트는 프론트 것이다.
     */
    private Map<String, Object> sanitize(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "요청 본문이 비어 있습니다.");
        }

        Map<String, Object> result = new LinkedHashMap<>(body);

        String model = String.valueOf(result.get("model"));
        boolean allowed = properties.allowedModels().stream()
                .anyMatch(m -> m.equalsIgnoreCase(model));
        if (!allowed) {
            log.warn("허용되지 않은 모델 요청 '{}'", model);
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_ALLOWED,
                    "지원하지 않는 모델입니다: " + model);
        }

        // 스트리밍은 지원하지 않는다. true 로 오면 프론트가 응답을 파싱하지 못한다.
        result.remove("stream");
        // 클라이언트가 실어 보낸 인증 정보는 버린다. 키는 서버 것만 쓴다.
        result.remove("api_key");

        Object maxTokens = result.get("max_tokens");
        int limit = properties.maxTokensLimit();
        if (!(maxTokens instanceof Number n) || n.intValue() <= 0 || n.intValue() > limit) {
            result.put("max_tokens", limit);
        }

        return result;
    }

    private String serialize(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "요청 형식을 확인해주세요.");
        }
    }

    /**
     * 사용자당 하루 호출 수를 센다.
     *
     * Redis가 죽어 있으면 막지 않고 통과시킨다. 한도 계산이 안 된다고 검사지 업로드가
     * 통째로 멈추는 것보다는, 로그를 남기고 넘어가는 편이 낫다.
     */
    private void consumeQuota(Long userId) {
        String key = QUOTA_PREFIX + userId + ":" + LocalDate.now();
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofDays(2));
            }
            if (count != null && count > properties.dailyLimitPerUser()) {
                throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("호출 한도 확인 실패 — 이번 요청은 통과시킨다 userId={}", userId, e);
        }
    }

    /** 남은 호출 수. 화면에 안내를 띄우고 싶을 때 쓴다. */
    public int remainingQuota(Long userId) {
        try {
            String raw = redis.opsForValue().get(QUOTA_PREFIX + userId + ":" + LocalDate.now());
            int used = raw == null ? 0 : Integer.parseInt(raw);
            return Math.max(0, properties.dailyLimitPerUser() - used);
        } catch (Exception e) {
            return properties.dailyLimitPerUser();
        }
    }
}
