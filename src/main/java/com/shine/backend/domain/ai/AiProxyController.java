package com.shine.backend.domain.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OpenAI 프록시 (전달사항 7번).
 *
 * 프론트는 지금 https://api.openai.com/v1/chat/completions 를 직접 부르고 있다.
 * 그 URL을 이 엔드포인트로 바꾸고 Authorization 헤더를 앱 토큰으로만 두면 된다.
 * 본문·응답 모양은 OpenAI 그대로라 프롬프트나 파싱 코드는 손댈 게 없다.
 *
 *   before  fetch("https://api.openai.com/v1/chat/completions",
 *                 { headers: { Authorization: `Bearer ${OPENAI_API_KEY}` }, body })
 *   after   apiRequest("/ai/chat/completions", { method: "POST", body })
 *
 * 바꾼 뒤 EXPO_PUBLIC_OPENAI_API_KEY 를 .env 와 eas.json 에서 지우고 키를 폐기해야 한다.
 * 이미 배포된 번들이 있으면 그 키는 이미 유출된 것으로 봐야 한다.
 */
@Tag(name = "AI", description = "OpenAI 프록시 — 키를 서버에만 둔다")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiProxyController {

    private final OpenAiProxyService openAiProxyService;

    @Operation(summary = "검사지 OCR · 요약 생성",
            description = "OpenAI Chat Completions 를 그대로 통과시킨다. 요청·응답 모두 OpenAI 원문이며 "
                    + "ApiResponse 로 감싸지 않는다. model 은 gpt-4o / gpt-4o-mini 만 허용하고, "
                    + "max_tokens 는 서버 상한으로 잘리며, 사용자당 하루 호출 수에 제한이 있다.")
    @PostMapping(value = "/chat/completions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> chatCompletions(@AuthenticationPrincipal Long userId,
                                                  @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(openAiProxyService.chatCompletions(userId, body));
    }

    @Operation(summary = "오늘 남은 호출 수")
    @GetMapping("/quota")
    public ResponseEntity<Map<String, Integer>> quota(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(Map.of("remaining", openAiProxyService.remainingQuota(userId)));
    }
}
