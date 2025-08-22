package com.project.savingbee.filtering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 필터에서 사용되는 정렬
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SortFilter {

  private String field;   // 정렬 필드 (intr_rate2, intr_rate 등)
  private String order;   // 정렬 순서 (desc, asc)

  public boolean isDescending() {
    return "desc".equalsIgnoreCase(order);
  }
}