// AI 모델 응답 수신용 DTO (ModelResultDto.java)
package com.ododok.server.domain.book.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ModelResultDto {
    private String isbn;
    private Double score;     // 적합도 (예: 0.95 -> 95%)
    private Integer totalPage; // AI 모델이 연산하여 반환한 페이지 수
}