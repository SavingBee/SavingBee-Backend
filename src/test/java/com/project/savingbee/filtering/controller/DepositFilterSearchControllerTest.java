package com.project.savingbee.filtering.controller;

import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.service.DepositFilterSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepositFilterSearchController.class)
@DisplayName("예금 필터링+검색 컨트롤러 테스트")
class DepositFilterSearchControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DepositFilterSearchService depositFilterSearchService;

  @Test
  @WithMockUser
  @DisplayName("검색어 없이 필터링만 수행")
  void testFilterOnlyWithoutSearch() throws Exception {
    // Given
    List<ProductSummaryResponse> mockProducts = createMockProducts();
    Page<ProductSummaryResponse> mockPage = new PageImpl<>(mockProducts,
        PageRequest.of(0, 10), mockProducts.size());

    when(depositFilterSearchService.depositFilterWithSearch(any())).thenReturn(mockPage);

    // When & Then
    mockMvc.perform(get("/products/filter/deposit/search")
            .param("finCoType", "은행"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.totalElements", is(3)));
  }

  @Test
  @WithMockUser
  @DisplayName("검색어 있고 매칭 상품 있는 경우")
  void testFilterWithSearchFound() throws Exception {
    // Given - "우리" 검색어로 우리은행 상품만 반환
    List<ProductSummaryResponse> searchResults = Arrays.asList(
        ProductSummaryResponse.builder()
            .finPrdtCd("PROD001")
            .finPrdtNm("고금리예금")
            .korCoNm("우리은행")
            .productType("deposit")
            .maxIntrRate(new BigDecimal("4.50"))
            .baseIntrRate(new BigDecimal("3.50"))
            .build()
    );
    Page<ProductSummaryResponse> mockPage = new PageImpl<>(searchResults,
        PageRequest.of(0, 10), searchResults.size());

    when(depositFilterSearchService.depositFilterWithSearch(any())).thenReturn(mockPage);

    // When & Then
    mockMvc.perform(get("/products/filter/deposit/search")
            .param("q", "우리")
            .param("finCoType", "은행"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].kor_co_nm", containsString("우리")));
  }

  @Test
  @WithMockUser
  @DisplayName("검색어 있고 매칭 상품 없는 경우 - 필터링 결과 반환")
  void testFilterWithSearchNotFound() throws Exception {
    // Given - 검색 결과 없어서 필터링 결과 반환
    List<ProductSummaryResponse> mockProducts = createMockProducts();
    Page<ProductSummaryResponse> mockPage = new PageImpl<>(mockProducts,
        PageRequest.of(0, 10), mockProducts.size());

    when(depositFilterSearchService.depositFilterWithSearch(any())).thenReturn(mockPage);

    // When & Then
    mockMvc.perform(get("/products/filter/deposit/search")
            .param("q", "존재하지않는은행")
            .param("finCoType", "은행"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3))); // 필터링 결과 반환
  }

  /**
   * 테스트용 Mock 상품 데이터 생성
   */
  private List<ProductSummaryResponse> createMockProducts() {
    return Arrays.asList(
        ProductSummaryResponse.builder()
            .finPrdtCd("PROD001")
            .finPrdtNm("고금리예금")
            .korCoNm("우리은행")
            .productType("deposit")
            .maxIntrRate(new BigDecimal("4.50"))
            .baseIntrRate(new BigDecimal("3.50"))
            .build(),
        ProductSummaryResponse.builder()
            .finPrdtCd("PROD002")
            .finPrdtNm("안정예금")
            .korCoNm("국민은행")
            .productType("deposit")
            .maxIntrRate(new BigDecimal("3.20"))
            .baseIntrRate(new BigDecimal("2.80"))
            .build(),
        ProductSummaryResponse.builder()
            .finPrdtCd("PROD003")
            .finPrdtNm("기본예금")
            .korCoNm("신한은행")
            .productType("deposit")
            .maxIntrRate(new BigDecimal("2.10"))
            .baseIntrRate(new BigDecimal("1.80"))
            .build()
    );
  }
}