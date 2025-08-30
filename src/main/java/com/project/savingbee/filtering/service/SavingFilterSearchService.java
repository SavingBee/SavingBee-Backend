package com.project.savingbee.filtering.service;

import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.dto.SavingFilterRequest;
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
public class SavingFilterSearchService {

  private final SavingFilterService savingFilterService;
  private final KoreanParsing koreanParsing;

  /**
   * 적금 필터링에 검색 기능 추가
   */
  public Page<ProductSummaryResponse> savingFilterWithSearch(SavingFilterRequest request){
    log.info("적금 필터링에 검색어 조건 추가 - 검색어 {}",request.getQ());
    // 1. 검색어 체크
    if (!request.hasSearchTerm()) {
      return savingFilterService.savingFilter(request);
    }
    // 2. 필터링 수행
    SavingFilterRequest filterOnlyRequest = createFilterOnlyRequest(request);
    Page<ProductSummaryResponse> filteredProducts = savingFilterService.savingFilter(
        filterOnlyRequest);
    // 3. 검색어 전처리
    String processedSearchTerm = preprocessSearchTerm(request.getQ());
    // 4. 필터링 결과에서 검색어로 상품명 검색
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
   * 1. 검색어 체크
   * 검색어 존재 여부 체크 - 요청에서 q파라미터가 있는 지 확인
   * 검색어가 없으면 바로 savingFilter 시전
   * 검색어가 있으면 검색어 제외하고 savingFilter 수행할 수 있게 filteringRequest 정리
   */
  private SavingFilterRequest createFilterOnlyRequest(SavingFilterRequest original) {
    SavingFilterRequest request = SavingFilterRequest.builder()
        .filters(original.getFilters())
        .build();

    request.setPage(original.getPageNumber());
    request.setSize(original.getPageSize());
    request.setSort(original.getSort());
    request.setQ(null); // 검색어 제외

    return request;
  }
  /**
   * 3. 검색어 전처리
   */
  private String preprocessSearchTerm(String searchTerm) {
    return koreanParsing.processKoreanText(searchTerm);
  }
  /**
   * 4. 필터링 결과에서 검색어로 상품명 검색
   * 2에서 수행한 필터링 결과에서 검색어가 상품면에 포함되어 있는지 검색 진행
   * 검색 결과가 있을 경우: 해당 결과 반환
   * 검색 결과가 없을 경우: 빈 결과 반환
   */
  private List<ProductSummaryResponse> findProductsContainingSearchTerm(
      List<ProductSummaryResponse> products, String searchTerm) {

    return products.stream()
        .filter(product -> product.getFinPrdtNm() != null &&
            product.getFinPrdtNm().toLowerCase().contains(searchTerm.toLowerCase()))
        .collect(Collectors.toList());
  }


}
