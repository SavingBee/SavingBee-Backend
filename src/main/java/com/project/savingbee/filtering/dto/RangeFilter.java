package com.project.savingbee.filtering.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 범위 필터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RangeFilter {

  private BigDecimal min; // 범위 최소값
  private BigDecimal max; // 범위 최대값

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