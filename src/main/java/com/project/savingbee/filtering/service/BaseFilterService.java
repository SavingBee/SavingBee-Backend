package com.project.savingbee.filtering.service;

import com.project.savingbee.filtering.dto.BaseFilterRequest;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public abstract class BaseFilterService<T, R extends BaseFilterRequest> {

  protected abstract String getProductCode(T product);

  /**
   * 공통 메서드
   */

  // 금리 정렬 여부 확인
  protected boolean isInterestRateSort(R request) { if (!request.hasSort()) {
    return true; // 기본값이 금리 정렬
  }

    String sortField = request.getSort().getField();
    return "intr_rate2".equals(sortField) ||
        "max_intr_rate".equals(sortField) ||
        "intr_rate".equals(sortField) ||
        "base_intr_rate".equals(sortField);
  }

  // JOIN으로 중복 제거 - List
  protected List<T> removeDuplicates(List<T> products) {
    return products.stream()
        .collect(Collectors.toMap(
            this::getProductCode,  // 상품코드로 중복 제거
            product -> product,
            (existing, replacement) -> existing,  // 중복 시 기존 것 유지
            java.util.LinkedHashMap::new  // 순서 유지
        ))
        .values()
        .stream()
        .collect(Collectors.toList());
  }

  // JOIN으로 인한 중복 제거
  protected Page<T> removeDuplicatesFromPage(Page<T> products) {
    List<T> distinctList = removeDuplicates(products.getContent());

    return new PageImpl<>(
        distinctList,
        products.getPageable(),
        products.getTotalElements()  // 전체 개수는 원래 값 유지
    );
  }

  // 상품명 정렬 Pageable 생성
  protected Pageable createPageableForDbSort(R request) {
    Sort.Direction direction = Sort.Direction.ASC;
    String sortField = "finPrdtNm"; // 기본값: 상품명

    if (request.hasSort()) {
      direction = request.getSort().isDescending() ? Sort.Direction.DESC : Sort.Direction.ASC;
      String requestedField = request.getSort().getField();

      // 상품명 정렬만 지원
      if ("fin_prdt_nm".equals(requestedField)) {
        sortField = "finPrdtNm";
      } else {
        log.debug("DB 정렬 지원하지 않는 필드 '{}', 상품명으로 대체", requestedField);
        sortField = "finPrdtNm";
      }
    }

    Sort sort = Sort.by(direction, sortField);
    int pageNumber = Math.max(0, request.getPageNumber() - 1);
    return PageRequest.of(pageNumber, request.getPageSize(), sort);
  }

}
