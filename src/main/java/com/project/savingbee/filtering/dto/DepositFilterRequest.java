package com.project.savingbee.filtering.dto;

import java.util.*;
import lombok.*;

/**
 * 예금 필터링 검색 요청 - 프론트엔드에서 파라미터로 받은 조건을 정리하여 FilterService에서 사용할 수 있게함
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositFilterRequest extends BaseFilterRequest {

  private Filters filters;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Filters {

    private List<String> orgTypeCode;           // 금융회사 번호들
    private List<String> joinWay;           // 우대조건들
    private List<String> joinDeny;          // 가입제한
    private List<Integer> saveTrm;          // 저축기간
    private List<String> intrRateType;     // 이자율 유형 (단리, 복리)
    private RangeFilter intrRate;          // 기본금리 범위
    private RangeFilter intrRate2;         // 최고금리 범위
    private RangeFilter maxLimit;          // 한도 범위
  }

  // 편의 메서드들
  public boolean hasFilters() {
    return filters != null;
  }
}
