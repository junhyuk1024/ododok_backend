package com.ododok.server.domain.transit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ODsaySearchPathResponseDto {
    private Result result;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private List<Path> path;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Path {
        private Info info;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        private int totalTime; // 총 소요 시간 (분)
    }
}