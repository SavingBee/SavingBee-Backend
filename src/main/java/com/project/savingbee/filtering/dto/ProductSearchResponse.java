package com.project.savingbee.filtering.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 상품명 검색용 응답 DTO
 */
@Data
@Builder
public class ProductSearchResponse {
  private List<ProductSummaryResponse> products;
  private List<ProductSummaryResponse> popularProducts;
  private Integer totalCount;
  private String searchTerm;
  private String message;
}
