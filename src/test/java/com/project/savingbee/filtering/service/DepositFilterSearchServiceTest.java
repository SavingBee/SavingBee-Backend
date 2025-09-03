package com.project.savingbee.filtering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.savingbee.filtering.dto.DepositFilterRequest;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.util.KoreanParsing;
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

@ExtendWith(MockitoExtension.class)
class DepositFilterSearchServiceTest {

  @Mock
  private DepositFilterService depositFilterService;

  @Mock
  private KoreanParsing koreanParsing;

  @InjectMocks
  private DepositFilterSearchService depositFilterSearchService;

  private DepositFilterRequest testRequest;
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
    when(depositFilterService.depositFilter(any(DepositFilterRequest.class)))
        .thenReturn(mockFilteredProducts);

    // When
    Page<ProductSummaryResponse> result = depositFilterSearchService.depositFilterWithSearch(
        testRequest);

    // Then
    assertThat(result).isEqualTo(mockFilteredProducts);
    verify(depositFilterService, times(1)).depositFilter(testRequest);
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
    testRequest.setFilters(DepositFilterRequest.Filters.builder()
        .saveTrm(List.of(12))  // 저축기간 필터 추가
        .build());

    when(depositFilterService.depositFilter(any(DepositFilterRequest.class)))
        .thenReturn(mockFilteredProducts);
    when(koreanParsing.processKoreanText("우리은행"))
        .thenReturn("우리은행");

    // When
    Page<ProductSummaryResponse> result = depositFilterSearchService.depositFilterWithSearch(
        testRequest);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getFinPrdtNm()).contains("우리은행");
    verify(koreanParsing, times(1)).processKoreanText("우리은행");
    verify(depositFilterService, times(1)).depositFilter(any()); // 필터링 서비스 호출됨
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
    testRequest.setFilters(DepositFilterRequest.Filters.builder()
        .saveTrm(List.of(12))  // 저축기간 필터 추가
        .build());

    when(depositFilterService.depositFilter(any(DepositFilterRequest.class)))
        .thenReturn(mockFilteredProducts);
    when(koreanParsing.processKoreanText("존재하지않는은행"))
        .thenReturn("존재하지않는은행");

    // When
    Page<ProductSummaryResponse> result = depositFilterSearchService.depositFilterWithSearch(
        testRequest);

    // Then
    assertThat(result.getContent()).isEmpty(); // 빈 결과
    assertThat(result.getTotalElements()).isEqualTo(0); // 총 개수 0
    assertThat(result.getPageable()).isEqualTo(mockFilteredProducts.getPageable()); // 페이징 정보는 유지
    verify(koreanParsing, times(1)).processKoreanText("존재하지않는은행");
    verify(depositFilterService, times(1)).depositFilter(any()); // 필터링 서비스 호출됨
  }

  /**
   * Mock 데이터 설정
   */
  private void setupMockProducts() {
    mockProductList = List.of(
        ProductSummaryResponse.builder()
            .finPrdtCd("DEPOSIT001")
            .finPrdtNm("우리은행 적금")
            .korCoNm("우리은행")
            .productType("deposit")
            .build(),
        ProductSummaryResponse.builder()
            .finPrdtCd("DEPOSIT002")
            .finPrdtNm("신한은행 예금")
            .korCoNm("신한은행")
            .productType("deposit")
            .build()
    );

    mockFilteredProducts = new PageImpl<>(mockProductList,
        PageRequest.of(0, 10), mockProductList.size());
  }

  private void setupTestRequest() {
    testRequest = DepositFilterRequest.builder()
        .page(1)
        .size(10)
        .filters(DepositFilterRequest.Filters.builder().build())
        .build();
  }
}