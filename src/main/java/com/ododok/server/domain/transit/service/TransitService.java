package com.ododok.server.domain.transit.service;

import com.ododok.server.domain.recommend.dto.AiBookResponseDto;
import com.ododok.server.domain.recommend.dto.AiRecommendResponseDto;
import com.ododok.server.domain.transit.dto.ODsaySearchPathResponseDto;
import com.ododok.server.domain.transit.dto.PointDto;
import com.ododok.server.domain.transit.dto.TransitRouteResponseDto;
import com.ododok.server.infra.ai.AiRecommendClient;
import com.ododok.server.infra.kakao.KakaoGeoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransitService {

    private final RestTemplate restTemplate;
    private final KakaoGeoClient kakaoGeoClient; // 카카오 지오코딩 클라이언트
    private final AiRecommendClient aiRecommendClient; // 파이썬 AI 추천 클라이언트

    @Value("${ODSAY_API_KEY}")
    private String apiKey;

    /**
     * 🌟 [API 1 전용] 출발역/도착역 이름을 받아 지하철 소요 시간(분)만 반환합니다.
     */
    public int getTransitDurationMinutesByName(String startStationName, String endStationName) {
        log.info("🚀 [Service] 이름 기반 지하철 경로 검색 시작: {} -> {}", startStationName, endStationName);

        // 1. 카카오 API를 이용해 출발지/목적지 좌표 가져오기 (지오코딩)
        PointDto startPoint = kakaoGeoClient.getCoordinatesByKeyword(startStationName);
        PointDto endPoint = kakaoGeoClient.getCoordinatesByKeyword(endStationName);

        // 좌표를 하나라도 찾지 못한 경우 처리
        if (startPoint == null || endPoint == null) {
            log.warn("⚠️ 출발지 또는 목적지의 좌표를 찾을 수 없습니다. 더미값(42)을 반환합니다.");
            return 42;
        }

        // 2. 확보한 좌표로 ODsay API 호출하여 지하철 전용 소요 시간 계산
        return callODsaySubwayApi(startPoint.getLongitude(), startPoint.getLatitude(),
                endPoint.getLongitude(), endPoint.getLatitude());
    }

    /**
     * 🌟 [API 2 전용] 이동 시간(분)과 유저 설정값을 받아 파이썬 AI 서버만 단독 호출하여 추천 도서 목록을 반환합니다.
     */
    public List<AiBookResponseDto> getBookRecommendations(int duration, Integer userCpm, List<String> preferredGenres) {
        log.info("🚀 [Service] 도서 추천 단독 요청 (이동시간: {}분, CPM: {})", duration, userCpm);

        Map<String, Object> aiRequestBody = new HashMap<>();
        aiRequestBody.put("user_cpm", userCpm != null ? userCpm : 950); // 기본 CPM 950
        aiRequestBody.put("one_way_minutes", duration);
        aiRequestBody.put("preferred_genres", preferredGenres != null && !preferredGenres.isEmpty() ? preferredGenres : List.of("소설"));

        try {
            // FeignClient를 통해 파이썬 AI 서버 호출
            AiRecommendResponseDto aiResponse = aiRecommendClient.getRecommendBooks(aiRequestBody);
            if (aiResponse != null && aiResponse.getBooks() != null) {
                log.info("✅ 파이썬 AI 도서 추천 성공: {}권 발견", aiResponse.getBooks().size());
                return aiResponse.getBooks();
            }
        } catch (Exception e) {
            log.error("❌ 파이썬 AI 추천 서버 호출 중 예외 발생: {}", e.getMessage(), e);
        }

        return List.of(); // 예외 발생 시 빈 리스트 반환
    }

    /**
     * 🌟 [기존 결합형 메서드] 소요 시간 + 도서 추천을 한 번에 가져오는 기존 메서드 (필요시 유지)
     */
    public TransitRouteResponseDto getRouteDurationAndRecommendByName(String startStationName, String endStationName, List<String> preferredGenres) {
        int duration = getTransitDurationMinutesByName(startStationName, endStationName);
        List<AiBookResponseDto> recommendedBooks = getBookRecommendations(duration, 950, preferredGenres);

        return new TransitRouteResponseDto(startStationName, endStationName, duration, recommendedBooks);
    }

    /**
     * 🌟 [서브 비즈니스 로직] 좌표를 받아 ODsay API를 호출하여 지하철 직통 소요 시간만 추출합니다.
     */
    private int callODsaySubwayApi(double sx, double sy, double ex, double ey) {
        log.info("📌 현재 적용된 ODsay API Key: {}", apiKey);

        if ("DUMMY_KEY".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ ODsay API Key가 DUMMY_KEY이거나 비어있습니다. 가상 소요시간(42분)을 반환합니다.");
            return 42;
        }

        try {
            String cleanKey = apiKey.trim();

            // 🌟 1. 이미 %2F, %2B 등으로 들어왔을 경우를 대비해 원본문자열로 강제 복원(디코딩)
            if (cleanKey.contains("%")) {
                cleanKey = java.net.URLDecoder.decode(cleanKey, StandardCharsets.UTF_8.name());
            }

            // 🌟 2. 원본 키 상태에서 URLEncoder 진행
            String encodedApiKey = URLEncoder.encode(cleanKey, StandardCharsets.UTF_8.name());

            // 🌟 3. RestTemplate의 자동 이중 인코딩 방지
            DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
            factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
            restTemplate.setUriTemplateHandler(factory);

            String urlInfo = String.format(
                    "https://api.odsay.com/v1/api/searchPubTransPath?SX=%s&SY=%s&EX=%s&EY=%s&apiKey=%s&SearchPathType=1",
                    String.valueOf(sx), String.valueOf(sy), String.valueOf(ex), String.valueOf(ey), encodedApiKey
            );

            URI uri = URI.create(urlInfo);

            log.info("📌 ODsay 지하철 전용 요청 URI: {}", uri);

            // RAW JSON 응답 확인
            String rawJson = restTemplate.getForObject(uri, String.class);
            log.info("📌 ODsay RAW 응답 결과: {}", rawJson);

            // 4. DTO 파싱
            ODsaySearchPathResponseDto response = restTemplate.getForObject(uri, ODsaySearchPathResponseDto.class);

            if (response != null && response.getResult() != null && response.getResult().getPath() != null && !response.getResult().getPath().isEmpty()) {
                int totalTime = response.getResult().getPath().get(0).getInfo().getTotalTime();
                log.info("✅ 지하철 소요시간 계산 성공: {}분", totalTime);
                return totalTime;
            } else {
                log.warn("⚠️ 해당 경로에는 지하철 직통편이 존재하지 않습니다. (좌표간 지하철 이동 불가)");
                return 0;
            }
        } catch (Exception e) {
            log.error("❌ ODsay API 호출 중 예외 발생: {}", e.getMessage(), e);
        }

        return 42;
    }
}