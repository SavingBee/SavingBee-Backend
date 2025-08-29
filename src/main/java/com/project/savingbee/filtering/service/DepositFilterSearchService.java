package com.project.savingbee.filtering.service;

import com.project.savingbee.filtering.dto.DepositFilterRequest;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.util.KoreanParsing;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositFilterSearchService {

  private final DepositFilterService depositFilterService;
  private final KoreanParsing koreanParsing;

  /**
   * 필터링 -> 검색어 포함되었는지 확인
   */
  public Page<ProductSummaryResponse> depositFilterWithSearch(DepositFilterRequest request) {
    log.info("예금 필터링에 검색어 조건 추가 - 검색어 {}", request.getQ());

    // 1. 검색어 체크 - 검색어가 없으면 필터링 결과만 반환
    if (!request.hasSearchTerm()) {
      return depositFilterService.depositFilter(request);
    }

    // 2. 기존 필터링 수행 (검색어 제외)
    DepositFilterRequest filterOnlyRequest = createFilterOnlyRequest(request);
    Page<ProductSummaryResponse> filteredProducts = depositFilterService.depositFilter(
        filterOnlyRequest);

    // 3. 검색어 전처리
    String processedSearchTerm = preprocessSearchTerm(request.getQ());

    // 4. 필터링된 상품들에서 해당 검색어를 포함하는 상품찾기
    List<ProductSummaryResponse> searchResults = findProductsContainingSearchTerm(
        filteredProducts.getContent(), processedSearchTerm);

    // 검색 결과가 있으면 검색 결과, 없으면 빈 결과 반환
    if (!searchResults.isEmpty()) {
      return new PageImpl<>(searchResults, filteredProducts.getPageable(), searchResults.size());
    } else {
      // 검색 결과 없음 - 빈 결과 반환
      return new PageImpl<>(Collections.emptyList(), filteredProducts.getPageable(), 0);
    }
  }

  /**
   * 1. 검색어 체크 q를 제외한 요청(filterRequest) 생성
   */
  private DepositFilterRequest createFilterOnlyRequest(DepositFilterRequest original) {
    return DepositFilterRequest.builder()
        .page(original.getPageNumber())
        .size(original.getPageSize())
        .sort(original.getSort())
        .filters(original.getFilters())
        .q(null) // 검색어 제외
        .build();
  }

  /**
   * 3. 검색어 전처리
   */
  private String preprocessSearchTerm(String searchTerm) {
    return koreanParsing.processKoreanText(searchTerm);
  }

  /**
   * 4. 필터링된 상품들에서 해당 검색어를 포함하는 상품찾기
   */
  private List<ProductSummaryResponse> findProductsContainingSearchTerm(
      List<ProductSummaryResponse> products, String searchTerm) {

    return products.stream()
        .filter(product -> product.getFinPrdtNm() != null &&
            product.getFinPrdtNm().toLowerCase().contains(searchTerm.toLowerCase()))
        .collect(Collectors.toList());
  }
}