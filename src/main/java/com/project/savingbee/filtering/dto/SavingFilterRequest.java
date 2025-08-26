package com.project.savingbee.filtering.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 적금 필터링 검색 요청
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingFilterRequest extends BaseFilterRequest {

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
    private List<String> intrRateType;     // 이자율 유형 (단리,복리)
    private List<String> rsrvType; // 적립유형(S:정액적립식, F:자유적립식)
    private Integer monthlyMaxLimit;       // 월 저축금
    private Integer totalMaxLimit;    // 총 저축금
    private RangeFilter intrRate;          // 기본금리 범위
    private RangeFilter intrRate2;         // 최고금리 범위

  }

  // 편의 메서드들
  public boolean hasFilters() {
    return filters != null;
  }
}
