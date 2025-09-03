package com.project.savingbee.productCompare.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CompareResponseDto {

  private final List<ProductCompareInfosDto> info;  // 비교하는 두 상품 정보
  private final String winnerId;  // 만기시 실수령액이 더 높은 쪽의 상품코드, 같을 경우 둘 다 null
}
