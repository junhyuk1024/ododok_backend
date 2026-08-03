package com.ododok.server.infra.kakao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class KakaoGeoClient {

    private final RestTemplate restTemplate;
    private final String kakaoApiKey;
    private static final String KAKAO_GEO_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    public KakaoGeoClient(RestTemplate restTemplate, @Value("${KAKAO_REST_API_KEY}") String kakaoApiKey) {
        this.restTemplate = restTemplate;
        this.kakaoApiKey = kakaoApiKey;
    }

    public com.ododok.server.domain.transit.dto.PointDto getCoordinatesByKeyword(String keyword) {
        log.info("🚀 [Kakao] 지오코딩 요청: {}", keyword);

        // 카카오 API는 Header에 API Key를 넣어야 합니다.
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String uri = UriComponentsBuilder.fromHttpUrl(KAKAO_GEO_URL)
                .queryParam("query", keyword)
                .build()
                .toUriString();

        try {
            ResponseEntity<KakaoGeoResponseDto> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, KakaoGeoResponseDto.class);

            if (response.getBody() != null && !response.getBody().getDocuments().isEmpty()) {
                KakaoGeoResponseDto.Document doc = response.getBody().getDocuments().get(0);
                double lon = Double.parseDouble(doc.getX());
                double lat = Double.parseDouble(doc.getY());
                log.info("✅ 지오코딩 성공: {} -> ({}, {})", keyword, lon, lat);
                return new com.ododok.server.domain.transit.dto.PointDto(lon, lat);
            }
        } catch (Exception e) {
            log.error("❌ 카카오 지오코딩 에러: {}", e.getMessage());
        }
        return null; // 검색 결과 없거나 에러 시 null 반환
    }
}