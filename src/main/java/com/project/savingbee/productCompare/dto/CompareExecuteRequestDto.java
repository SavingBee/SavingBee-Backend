package com.project.savingbee.productCompare.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.UniqueElements;

@Getter
@Setter
public class CompareExecuteRequestDto {

  @NotNull
  @Size(min = 2, max = 2)
  @UniqueElements // 중복 방지
  private List<String> productIds; // 선택한 두 상품의 상품코드

  @NotNull
  private String type;  // 예금(D) / 적금(S)

  @NotNull
  private BigDecimal amount;  // 예치금 / 월 납입금

  @NotNull
  private Integer termMonth;  // 예치 기간

  @NotNull
  private String intrRateType;  // 단리(S) / 복리(M)
}
