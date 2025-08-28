package com.project.savingbee.filtering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterSearchSummaryResponse {

  // 검색어 정보
  private String searchTerm;

  // 검색 결과 존재 여부
  private boolean hasSearchResults;

  // 검색 결과가 없을 때 메시지
  private String noDataMessage;

  // 실제 상품 목록 (검색 결과 또는 필터링 결과)
  private Page<ProductSummaryResponse> products;

}