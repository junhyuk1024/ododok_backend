package com.ododok.server.infra.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoGeoResponseDto {
    private List<Document> documents;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        private String x; // 경도
        private String y; // 위도
    }
}