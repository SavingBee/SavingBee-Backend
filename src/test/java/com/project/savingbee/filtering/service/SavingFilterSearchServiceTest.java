package com.project.savingbee.filtering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.savingbee.filtering.dto.ProductSearchResponse;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.dto.SavingFilterRequest;
import com.project.savingbee.filtering.util.KoreanParsing;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("적금 필터링+검색 서비스 테스트")
class SavingFilterSearchServiceTest {

  @Mock
  private SavingFilterService savingFilterService;

  @Mock
  private KoreanParsing koreanParsing;

  @Mock
  private SearchService searchService;

  @InjectMocks
  private SavingFilterSearchService savingFilterSearchService;

  private SavingFilterRequest testRequest;
  private Page<ProductSummaryResponse> mockFilteredProducts;
  private List<ProductSummaryResponse> mockProductList;

  @BeforeEach
  void setUp() {
    setupMockProducts();
    setupTestRequest();
  }

  /**
   * 검색어 없는 경우 - 필터링 결과만 반환
   */
  @Test
  @DisplayName("검색어가 없을 때는 필터링 결과만 반환한다")
  void testFilterOnly() {
    // Given
    testRequest.setQ(null);
    when(savingFilterService.savingFilter(any(SavingFilterRequest.class)))
        .thenReturn(mockFilteredProducts);

    // When
    Page<ProductSummaryResponse> result = savingFilterSearchService.savingFilterWithSearch(
        testRequest);

    // Then
    assertThat(result).isEqualTo(mockFilteredProducts);
    verify(savingFilterService, times(1)).savingFilter(testRequest);
    verify(koreanParsing, never()).processKoreanText(any());
  }

  /**
   * 검색어 있고 검색 결과 있는 경우
   */
  @Test
  @DisplayName("필터와 검색어가 함께 있고 매칭되는 상품이 있을 때 검색 결과를 반환한다")
  void testFilterWithSearchResultsFound() {
    // Given
    testRequest.setQ("우리은행");
    // 필터 조건 추가 (필터+검색 케이스로 만들기)
    testRequest.setFilters(SavingFilterRequest.Filters.builder()
        .saveTrm(List.of(12))  // 저축기간 필터 추가
        .build());

    when(savingFilterService.savingFilter(any(SavingFilterRequest.class)))
        .thenReturn(mockFilteredProducts);
    when(koreanParsing.processKoreanText("우리은행"))
        .thenReturn("우리은행");

    // When
    Page<ProductSummaryResponse> result = savingFilterSearchService.savingFilterWithSearch(
        testRequest);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getFinPrdtNm()).contains("우리은행");
    verify(koreanParsing, times(1)).processKoreanText("우리은행");
    verify(savingFilterService, times(1)).savingFilter(any()); // 필터링 서비스 호출됨
  }

  /**
   * 검색어 있고 검색 결과 없는 경우
   */
  @Test
  @DisplayName("필터와 검색어가 함께 있지만 매칭되는 상품이 없을 때 빈 결과 반환")
  void testFilterWithSearchResultsNotFound() {
    // Given
    testRequest.setQ("존재하지않는은행");
    // 필터 조건 추가 (필터+검색 케이스로 만들기)
    testRequest.setFilters(SavingFilterRequest.Filters.builder()
        .saveTrm(List.of(12))  // 저축기간 필터 추가
        .build());

    when(savingFilterService.savingFilter(any(SavingFilterRequest.class)))
        .thenReturn(mockFilteredProducts);
    when(koreanParsing.processKoreanText("존재하지않는은행"))
        .thenReturn("존재하지않는은행");

    // When
    Page<ProductSummaryResponse> result = savingFilterSearchService.savingFilterWithSearch(
        testRequest);

    // Then
    assertThat(result.getContent()).isEmpty(); // 빈 결과
    assertThat(result.getTotalElements()).isEqualTo(0); // 총 개수 0
    assertThat(result.getPageable()).isEqualTo(mockFilteredProducts.getPageable()); // 페이징 정보는 유지
    verify(koreanParsing, times(1)).processKoreanText("존재하지않는은행");
    verify(savingFilterService, times(1)).savingFilter(any()); // 필터링 서비스 호출됨
  }

  /**
   * 검색어만 있는 경우 - SearchService 호출
   */
  @Test
  @DisplayName("검색어만 있을 때는 SearchService를 호출하여 결과를 반환한다")
  void testSearchOnlyWithResults() {
    // Given
    testRequest.setQ("우리은행");
    // 모든 필터 조건을 null로 설정 (검색어만 있는 상황)
    testRequest.setFilters(null);

    ProductSearchResponse mockSearchResponse = ProductSearchResponse.builder()
        .products(mockProductList)
        .totalCount(mockProductList.size())
        .searchTerm("우리은행")
        .build();

    when(searchService.searchProduct("우리은행"))
        .thenReturn(ResponseEntity.ok(mockSearchResponse));

    // When
    Page<ProductSummaryResponse> result = savingFilterSearchService.savingFilterWithSearch(
        testRequest);

    // Then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getTotalElements()).isEqualTo(2);
    verify(searchService, times(1)).searchProduct("우리은행");
    verify(savingFilterService, never()).savingFilter(any()); // 필터링 서비스는 호출되지 않음
    verify(koreanParsing, never()).processKoreanText(any()); // 한국어 전처리도 호출되지 않음
  }

  @Test
  @DisplayName("검색어만 있지만 검색 결과가 없을 때는 빈 결과를 반환한다")
  void testSearchOnlyWithNoResults() {
    // Given
    testRequest.setQ("존재하지않는은행");
    testRequest.setFilters(null);

    ProductSearchResponse mockSearchResponse = ProductSearchResponse.builder()
        .products(List.of()) // 빈 결과
        .totalCount(0)
        .searchTerm("존재하지않는은행")
        .build();

    when(searchService.searchProduct("존재하지않는은행"))
        .thenReturn(ResponseEntity.ok(mockSearchResponse));

    // When
    Page<ProductSummaryResponse> result = savingFilterSearchService.savingFilterWithSearch(
        testRequest);

    // Then
    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isEqualTo(0);
    verify(searchService, times(1)).searchProduct("존재하지않는은행");
  }

  /**
   * Mock 데이터 설정
   */
  private void setupMockProducts() {
    mockProductList = List.of(
        ProductSummaryResponse.builder()
            .finPrdtCd("SAVING001")
            .finPrdtNm("우리은행 적금")
            .korCoNm("우리은행")
            .productType("saving")
            .maxIntrRate(new BigDecimal("3.80"))
            .baseIntrRate(new BigDecimal("2.50"))
            .build(),
        ProductSummaryResponse.builder()
            .finPrdtCd("SAVING002")
            .finPrdtNm("신한은행 적금")
            .korCoNm("신한은행")
            .productType("saving")
            .maxIntrRate(new BigDecimal("3.60"))
            .baseIntrRate(new BigDecimal("2.30"))
            .build()
    );

    mockFilteredProducts = new PageImpl<>(mockProductList,
        PageRequest.of(0, 10), mockProductList.size());
  }

  private void setupTestRequest() {
    testRequest = SavingFilterRequest.builder()
        .filters(SavingFilterRequest.Filters.builder().build())
        .build();

    testRequest.setPage(1);
    testRequest.setSize(10);
  }
}