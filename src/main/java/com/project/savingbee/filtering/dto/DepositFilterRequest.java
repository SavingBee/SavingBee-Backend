package com.project.savingbee.filtering.dto;

import java.math.BigDecimal;
import java.util.*;
import lombok.*;

/**
 * 예금 필터링 검색 요청 프론트엔드에서 shortUrl을 통해 파라미터로 받은 조건을 정리하여 FilterService에서 사용할 수 있게함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositFilterRequest {

  /**
   * 조건 선택 확인 - 금융권역 - 가입 대상 - 저축 기간 - 이자계산 방식
   */

  private Filters filters;
  private Sort sort;
  private Integer page;
  private Integer size;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Filters {

    private List<String> finCoNo;           // 금융회사 번호들
    private List<String> joinWay;           // 우대조건들 (비대면 가입, 재예치, 첫 거래)
    private List<String> joinDeny;          // 가입제한 (1, 3 등)
    private List<Integer> saveTrm;          // 저축기간 (6, 12, 24, 36)
    private List<String> intrRateType;     // 이자율 유형 (S, M)
    private RangeFilter intrRate;          // 기본금리 범위
    private RangeFilter intrRate2;         // 최고금리 범위
    private RangeFilter maxLimit;          // 한도 범위
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RangeFilter {

    private BigDecimal min;
    private BigDecimal max;

    public boolean hasMinValue() {
      return min != null;
    }

    public boolean hasMaxValue() {
      return max != null;
    }

    public boolean hasAnyValue() {
      return hasMinValue() || hasMaxValue();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Sort {

    private String field;                   // 정렬 필드 (intr_rate2, intr_rate 등)
    private String order;                   // 정렬 순서 (desc, asc)

    public boolean isDescending() {
      return "desc".equalsIgnoreCase(order);
    }
  }

  // 편의 메서드들
  public boolean hasFilters() {
    return filters != null;
  }

  public boolean hasSort() {
    return sort != null && sort.getField() != null;
  }

  public int getPageNumber() {
    return page != null ? page : 1;
  }

  public int getPageSize() {
    return size != null ? size : 10;
  }

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

  // 기본 정렬 설정 메서드
  public void setDefaultValues() {
    if (page == null) {
      page = 1;
    }
    if (size == null) {
      size = 10;
    }
    if (sort == null) {
      sort = Sort.builder()
          .field("intr_rate2")
          .order("desc")
          .build();
    }
  }

}
