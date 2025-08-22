package com.project.savingbee.filtering.dto;

import java.util.*;
import lombok.*;

/**
 * 예금 필터링 검색 요청 - 프론트엔드에서 파라미터로 받은 조건을 정리하여 FilterService에서 사용할 수 있게함
 */
@Data
@EqualsAndHashCode(callSuper=true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositFilterRequest extends BaseFilterRequest {

  private Filters filters;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Filters  {

    private List<String> finCoNo;           // 금융회사 번호들
    private List<String> joinWay;           // 우대조건들
    private List<String> joinDeny;          // 가입제한
    private List<Integer> saveTrm;          // 저축기간
    private List<String> intrRateType;     // 이자율 유형
    private RangeFilter intrRate;          // 기본금리 범위
    private RangeFilter intrRate2;         // 최고금리 범위
    private RangeFilter maxLimit;          // 한도 범위
  }

  // 편의 메서드들
  public boolean hasFilters() {
    return filters != null;
  }

  // TODO: 삭제 예정 - 사용하지 않음?
  // 각 필터 조건 존재 여부 확인 메서드들
  public boolean hasFinCoFilter() {
    return hasFilters() && filters.getFinCoNo() != null && !filters.getFinCoNo().isEmpty();
  }

  public boolean hasJoinWayFilter() {
    return hasFilters() && filters.getJoinWay() != null && !filters.getJoinWay().isEmpty();
  }

  public boolean hasJoinDenyFilter() {
    return hasFilters() && filters.getJoinDeny() != null && !filters.getJoinDeny().isEmpty();
  }

  public boolean hasSaveTrmFilter() {
    return hasFilters() && filters.getSaveTrm() != null && !filters.getSaveTrm().isEmpty();
  }

  public boolean hasIntrRateTypeFilter() {
    return hasFilters() && filters.getIntrRateType() != null && !filters.getIntrRateType()
        .isEmpty();
  }

  public boolean hasIntrRateFilter() {
    return hasFilters() && filters.getIntrRate() != null && filters.getIntrRate().hasAnyValue();
  }

  public boolean hasIntrRate2Filter() {
    return hasFilters() && filters.getIntrRate2() != null && filters.getIntrRate2().hasAnyValue();
  }

  public boolean hasMaxLimitFilter() {
    return hasFilters() && filters.getMaxLimit() != null && filters.getMaxLimit().hasAnyValue();
  }



}
