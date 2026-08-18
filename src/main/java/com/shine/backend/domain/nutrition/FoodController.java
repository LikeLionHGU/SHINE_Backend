package com.shine.backend.domain.nutrition;

import com.shine.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Foods", description = "추천 가능한 음식 목록")
@RestController
@RequestMapping("/api/v1/foods")
@RequiredArgsConstructor
public class FoodController {

    private final AllowedFoods allowedFoods;

    @Operation(summary = "추천 가능한 음식 목록",
            description = """
                    AI 프롬프트에 넣을 목록. 하드코딩하지 말고 여기서 받아 쓰면
                    목록이 바뀌어도 앱을 다시 배포할 필요가 없다.

                    서버는 이 목록에 없는 음식을 걸러내므로, 프롬프트에 넣지 않으면
                    추천이 비어버릴 수 있다.
                    """)
    @GetMapping
    public ApiResponse<Map<String, Object>> getFoods() {
        return ApiResponse.success(Map.of(
                "count", allowedFoods.all().size(),
                "foods", allowedFoods.all(),
                "promptHint", "foods 의 name 은 반드시 아래 목록에서만 고를 것. 목록에 없는 음식은 쓰지 말 것: "
                        + String.join(", ", allowedFoods.all())));
    }
}
