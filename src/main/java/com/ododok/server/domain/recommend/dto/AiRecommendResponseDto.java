package com.ododok.server.domain.recommend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiRecommendResponseDto {
    private List<AiBookResponseDto> books; // 파이썬이 반환하는 추천 도서 리스트
}