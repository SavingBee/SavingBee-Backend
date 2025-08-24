package com.project.savingbee.productCompare.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class ProductCompareInfosDto {
  private final String productId; // 상품코드
  private final String bankName;  // 금융회사명
  private final String productName; // 상품명

  private final BigDecimal intrRateBeforeTax; // 세전 이자율(%)
  private final BigDecimal intrRateAfterTax;  // 세후 이자율(%)
  private final BigDecimal highestPrefRate;  // 최고 우대금리(%)

  private final long intrAfterTax; // 세후 이자(원)
  private final long amountReceived; // 실수령액(원)
  private boolean winner; // 실수령액이 더 높은 쪽이 true, 같을 경우 둘 다 false

  private String intrRateType;  // 단리(S) / 복리(M)
}
