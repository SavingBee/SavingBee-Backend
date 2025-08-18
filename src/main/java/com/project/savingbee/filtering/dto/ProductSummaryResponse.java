package com.project.savingbee.filtering.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {

  // 금융 상품 코드
  @JsonProperty("fin_prdt_cd")
  private String finPrdtCd;

  // 금융 상품명
  @JsonProperty("fin_prdt_nm")
  private String finPrdtNm;

  // 금융 회사명
  @JsonProperty("kor_co_nm")
  private String korCoNm;

  // 상품 타입 (deposit: 예금, savings: 적금)
  @JsonProperty("product_type")
  private String productType;

  // 최고 우대 금리
  @JsonProperty("max_intr_rate")
  private BigDecimal maxIntrRate;

  // 기본 금리
  @JsonProperty("base_intr_rate")
  private BigDecimal baseIntrRate;
}
