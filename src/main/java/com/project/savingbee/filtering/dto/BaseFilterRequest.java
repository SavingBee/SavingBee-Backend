package com.project.savingbee.filtering.dto;


import lombok.Data;

@Data
public abstract class BaseFilterRequest {

  private SortFilter sort;
  private Integer page;
  private Integer size;
  private String q;

  // 공통 편의 메서드들
  // 기본 정렬 설정 메서드
  public void setDefaultValues() {
    if (page == null) {
      page = 1;
    }
    if (size == null) {
      size = 10;
    }
    if (sort == null) {
      sort = SortFilter.builder()
          .field("intr_rate2")
          .order("desc")
          .build();
    }
  }

  // 정렬 여부 확인
  public boolean hasSort() {
    return sort != null && sort.getField() != null;
  }

  // 1페이지에 10개의 상품
  public int getPageNumber() {
    return page != null ? page : 1;
  }

  public int getPageSize() {
    return size != null ? size : 10;
  }

  // 검색어 추가
  public boolean hasSearchTerm() {
    return q != null && !q.trim().isEmpty();
  }
}