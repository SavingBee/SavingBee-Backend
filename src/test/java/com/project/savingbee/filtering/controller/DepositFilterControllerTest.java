package com.project.savingbee.filtering.controller;

import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.service.DepositFilterService;
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

@WebMvcTest(DepositFilterController.class)
@DisplayName("예금 필터링 컨트롤러 테스트")
class DepositFilterControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DepositFilterService depositFilterService;

  @Test
  @WithMockUser
  @DisplayName("기본 필터링 요청")
  void testBasicFilteringRequest() throws Exception {
    // Given
    List<ProductSummaryResponse> mockProducts = createMockProducts();
    Page<ProductSummaryResponse> mockPage = new PageImpl<>(mockProducts,
        PageRequest.of(0, 10), mockProducts.size());

    when(depositFilterService.depositFilter(any())).thenReturn(mockPage);

    // When & Then
    mockMvc.perform(get("/products/filter/deposite"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[0].fin_prdt_cd", is("PROD001")))
        .andExpect(jsonPath("$.totalElements", is(3)))
        .andExpect(jsonPath("$.size", is(10)));
  }

  @Test
  @WithMockUser
  @DisplayName("필터 파라미터 테스트")
  void testFilterParameters() throws Exception {
    // Given
    List<ProductSummaryResponse> mockProducts = createMockProducts();
    Page<ProductSummaryResponse> mockPage = new PageImpl<>(mockProducts,
        PageRequest.of(0, 10), mockProducts.size());

    when(depositFilterService.depositFilter(any())).thenReturn(mockPage);

    // When & Then
    mockMvc.perform(get("/products/filter/deposite")
            .param("finCoNo", "0010001,0010002")
            .param("saveTrm", "12,24")
            .param("intrRateMin", "2.0")
            .param("intrRateMax", "5.0")
            .param("sortField", "intr_rate2")
            .param("sortOrder", "desc"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)));
  }

  @Test
  @WithMockUser
  @DisplayName("페이징 테스트")
  void testPagination() throws Exception {
    // Given
    List<ProductSummaryResponse> mockProducts = createMockProducts();
    Page<ProductSummaryResponse> mockPage = new PageImpl<>(mockProducts,
        PageRequest.of(1, 10), 25);

    when(depositFilterService.depositFilter(any())).thenReturn(mockPage);

    // When & Then
    mockMvc.perform(get("/products/filter/deposite")
            .param("page", "2"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number", is(1)))
        .andExpect(jsonPath("$.size", is(10)))
        .andExpect(jsonPath("$.totalElements", is(25)));
  }

  @Test
  @WithMockUser
  @DisplayName("잘못된 저축기간 파라미터")
  void testInvalidSavingTermParameter() throws Exception {
    // When & Then
    mockMvc.perform(get("/products/filter/deposite")
            .param("saveTrm", "invalid,12"))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  @DisplayName("서비스 예외 처리")
  void testServiceException() throws Exception {
    // Given
    when(depositFilterService.depositFilter(any()))
        .thenThrow(new RuntimeException("데이터베이스 연결 오류"));

    // When & Then
    mockMvc.perform(get("/products/filter/deposite"))
        .andDo(print())
        .andExpect(status().isInternalServerError());
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