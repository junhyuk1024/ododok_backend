package com.ododok.server.infra.ai;

import com.ododok.server.domain.recommend.dto.AiRecommendResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "aiRecommendClient", url = "http://localhost:5000")
public interface AiRecommendClient {

    // 🌟 메서드 이름을 getRecommendBooks로 지정하거나, recommendBooks로 맞춰줍니다.
    @PostMapping("/api/v1/ai/recommend")
    AiRecommendResponseDto getRecommendBooks(@RequestBody Map<String, Object> requestBody);
}