package com.project.savingbee.productCompare.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompareRequestDto {

  @NotNull
  private String type;  // 예금(D) / 적금(S)

  @NotNull
  private BigDecimal amount;  // 예치금 / 월 납입금

  @NotNull
  private Integer termMonth;  // 예치 기간 [6, 12, 24, 36]

  @NotNull
  private BigDecimal minRate; // 최소 이자율(%)

  @NotNull
  private String intrRateType;  // 단리(S) / 복리(M)

  private String bankKeyword; // optional : 사용자가 입력한 키워드(금융회사명 필터링용)
}
