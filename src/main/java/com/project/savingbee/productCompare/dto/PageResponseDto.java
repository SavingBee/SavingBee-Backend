package com.project.savingbee.productCompare.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

@Getter
@AllArgsConstructor
public class PageResponseDto<T> {
  private final List<T> content;
  private final int page;
  private final int size; // page size
  private final long totalElements; // 전체 건수

  public static <T> PageResponseDto<T> fromList(List<T> all, Pageable pageable) {
    int page = Math.max(0, pageable.getPageNumber());
    int size = pageable.getPageSize();              // @PageableDefault로 기본값 20
    int from = Math.min(page * size, all.size());
    int to   = Math.min(from + size, all.size());
    List<T> slice = all.subList(from, to);

    return new PageResponseDto<>(slice, page, size, all.size());
  }
}
