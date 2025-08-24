package com.project.savingbee.filtering.controller;

import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.entity.SavingsInterestRates;
import com.project.savingbee.common.entity.FinancialCompanies;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.common.repository.FinancialCompaniesRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("적금 필터링 컨트롤러 통합 테스트")
class SavingFilterControllerTest {

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @Autowired
  private SavingsProductsRepository savingsProductsRepository;

  @Autowired
  private SavingsInterestRatesRepository savingsInterestRatesRepository;

  @Autowired
  private FinancialCompaniesRepository financialCompaniesRepository;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

    // 테스트 데이터 초기화
    savingsInterestRatesRepository.deleteAll();
    savingsProductsRepository.deleteAll();
    financialCompaniesRepository.deleteAll();

    setupTestData();
  }

  @Test
  @DisplayName("기본 필터링 요청 - 전체 상품")
  void testBasicFilteringRequest() throws Exception {
    // When & Then
    mockMvc.perform(get("/products/filter/saving"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[0].product_type", is("saving")))
        .andExpect(jsonPath("$.totalElements", is(3)))
        .andExpect(jsonPath("$.size", is(10)));
  }

  @Test
  @DisplayName("금융회사 유형 필터링 - 표시명 사용")
  void testFinancialCompanyTypeFilter() throws Exception {
    // When & Then
    mockMvc.perform(get("/products/filter/saving")
            .param("finCoType", "은행")
            .param("sortField", "intr_rate2")
            .param("sortOrder", "desc"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2))) // 우리은행, 국민은행
        .andExpect(jsonPath("$.content[0].kor_co_nm", anyOf(is("우리은행"), is("국민은행"))));
  }

  @Test
  @DisplayName("가입대상 필터링 - 표시명 사용")
  void testJoinDenyFilter() throws Exception {
    // When & Then
    mockMvc.perform(get("/products/filter/saving")
            .param("joinDeny", "제한없음")
            .param("sortField", "intr_rate2")
            .param("sortOrder", "desc"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3))); // 모든 상품이 제한없음
  }

  @Test
  @DisplayName("적립방식 필터링 - 적금 고유")
  void testReserveTypeFilter() throws Exception {
    // When & Then - 정액적립식만
    mockMvc.perform(get("/products/filter/saving")
            .param("rsrvType", "정액적립식"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2))); // 고금리, 중간금리 상품

    // When & Then - 자유적립식만
    mockMvc.perform(get("/products/filter/saving")
            .param("rsrvType", "자유적립식"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1))); // 자유적립 상품만
  }

  @Test
  @DisplayName("월 저축금 필터링 - 적금 고유")
  void testMonthlyMaxLimitFilter() throws Exception {
    // When & Then - 월 50만원 이상 저축 가능한 상품
    mockMvc.perform(get("/products/filter/saving")
            .param("monthlyMaxLimit", "500000"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2))); // 100만원, 50만원 한도 상품

    // When & Then - 월 100만원 저축 가능한 상품
    mockMvc.perform(get("/products/filter/saving")
            .param("monthlyMaxLimit", "1000000"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1))); // 100만원 한도 상품만
  }

  @Test
  @DisplayName("총 저축금 필터링 - 적금 고유")
  void testTotalMaxLimitFilter() throws Exception {
    // When & Then - 24개월 기간에 총 1,500만원 저축 가능한 상품
    mockMvc.perform(get("/products/filter/saving")
            .param("saveTrm", "24")
            .param("totalMaxLimit", "15000000"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1))); // 고금리상품만 (100만원×24개월=2,400만원)
  }

  @Test
  @DisplayName("저축기간 필터링")
  void testSavingTermFilter() throws Exception {
    // When & Then - 24개월 상품만
    mockMvc.perform(get("/products/filter/saving")
            .param("saveTrm", "24"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2))); // 고금리, 중간금리 상품

    // When & Then - 12개월 상품
    mockMvc.perform(get("/products/filter/saving")
            .param("saveTrm", "12"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3))); // 모든 상품에 12개월 옵션
  }

  @Test
  @DisplayName("우대조건 필터링")
  void testPreferentialConditionFilter() throws Exception {
    // When & Then
    mockMvc.perform(get("/products/filter/saving")
            .param("joinWay", "첫거래"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1))); // "신규고객 우대" 상품만
  }

  @Test
  @DisplayName("복합 필터링 - 적금 고유 필터 포함")
  void testComplexFilteringWithSavingsSpecificFilters() throws Exception {
    // When & Then
    mockMvc.perform(get("/products/filter/saving")
            .param("finCoType", "은행")
            .param("rsrvType", "정액적립식")
            .param("monthlyMaxLimit", "500000")
            .param("saveTrm", "12")
            .param("intrRateMin", "2.0")
            .param("sortField", "intr_rate2")
            .param("sortOrder", "desc"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2))); // 조건 만족하는 상품들
  }

  @Test
  @DisplayName("금리 범위 필터링")
  void testInterestRateRangeFilter() throws Exception {
    // When & Then
    mockMvc.perform(get("/products/filter/saving")
            .param("intrRateMin", "2.0")
            .param("intrRateMax", "4.0")
            .param("intrRate2Min", "2.5")
            .param("intrRate2Max", "5.0"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThan(0))));
  }

  @Test
  @DisplayName("정렬 테스트")
  void testSorting() throws Exception {
    // When & Then - 최고금리 내림차순 (기본)
    mockMvc.perform(get("/products/filter/saving"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[0].fin_prdt_cd", is("SAVING_HIGH_001"))); // 최고금리 상품 먼저

    // When & Then - 기본금리 오름차순
    mockMvc.perform(get("/products/filter/saving")
            .param("sortField", "intr_rate")
            .param("sortOrder", "asc"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[0].fin_prdt_cd", is("SAVING_FREE_001"))); // 최저금리 상품 먼저
  }

  @Test
  @DisplayName("페이징 테스트")
  void testPagination() throws Exception {
    // When & Then - 첫 번째 페이지
    mockMvc.perform(get("/products/filter/saving")
            .param("page", "1"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number", is(0))) // 0-based 페이징
        .andExpect(jsonPath("$.size", is(10)))
        .andExpect(jsonPath("$.totalElements", is(3)))
        .andExpect(jsonPath("$.totalPages", is(1)));
  }

  @Test
  @DisplayName("잘못된 저축기간 파라미터")
  void testInvalidSavingTermParameter() throws Exception {
    // When & Then
    mockMvc.perform(get("/products/filter/saving")
            .param("saveTrm", "invalid,12"))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("빈 결과 처리")
  void testEmptyResult() throws Exception {
    // When & Then - 존재하지 않는 금융회사로 필터링
    mockMvc.perform(get("/products/filter/saving")
            .param("finCoType", "존재하지않는은행"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)))
        .andExpect(jsonPath("$.totalElements", is(0)));
  }

  /**
   * 테스트용 데이터 설정
   */
  private void setupTestData() {
    // 금융회사 생성
    FinancialCompanies wooriBank = financialCompaniesRepository.save(
        FinancialCompanies.builder()
            .finCoNo("0010001")
            .korCoNm("우리은행")
            .orgTypeCode("020000") // 은행
            .build()
    );

    FinancialCompanies kbBank = financialCompaniesRepository.save(
        FinancialCompanies.builder()
            .finCoNo("0010002")
            .korCoNm("국민은행")
            .orgTypeCode("020000") // 은행
            .build()
    );

    FinancialCompanies shinhanSavingsBank = financialCompaniesRepository.save(
        FinancialCompanies.builder()
            .finCoNo("0010003")
            .korCoNm("신한저축은행")
            .orgTypeCode("030300") // 은행
            .build()
    );

    // 적금 상품 생성
    SavingsProducts highRateProduct = savingsProductsRepository.save(
        SavingsProducts.builder()
            .finPrdtCd("SAVING_HIGH_001")
            .finPrdtNm("고금리특별적금")
            .finCoNo("0010001")
            .joinWay("영업점,인터넷,스마트폰")
            .spclCnd("신규고객 우대")
            .joinDeny("1")
            .joinMember("개인")
            .maxLimit(new BigDecimal("1000000")) // 월 100만원
            .isActive(true)
            .dclsStrtDay(LocalDate.now())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build()
    );

    SavingsProducts mediumRateProduct = savingsProductsRepository.save(
        SavingsProducts.builder()
            .finPrdtCd("SAVING_MEDIUM_001")
            .finPrdtNm("안정금리적금")
            .finCoNo("0010002")
            .joinWay("영업점")
            .spclCnd("일반고객 대상")
            .joinDeny("1")
            .joinMember("개인")
            .maxLimit(new BigDecimal("500000")) // 월 50만원
            .isActive(true)
            .dclsStrtDay(LocalDate.now())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build()
    );

    SavingsProducts freeTypeProduct = savingsProductsRepository.save(
        SavingsProducts.builder()
            .finPrdtCd("SAVING_FREE_001")
            .finPrdtNm("자유적립식적금")
            .finCoNo("0010003")
            .joinWay("영업점,인터넷")
            .spclCnd("제한없음")
            .joinDeny("1")
            .joinMember("개인")
            .maxLimit(new BigDecimal("300000")) // 월 30만원
            .isActive(true)
            .dclsStrtDay(LocalDate.now())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build()
    );

    // 금리 정보 생성
    savingsInterestRatesRepository.saveAll(Arrays.asList(
        // 고금리 상품 금리 (정액적립식)
        SavingsInterestRates.builder()
            .finPrdtCd("SAVING_HIGH_001")
            .intrRateType("S").rsrvType("S").saveTrm(12)
            .intrRate(new BigDecimal("3.50")).intrRate2(new BigDecimal("4.00"))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build(),
        SavingsInterestRates.builder()
            .finPrdtCd("SAVING_HIGH_001")
            .intrRateType("S").rsrvType("S").saveTrm(24)
            .intrRate(new BigDecimal("3.70")).intrRate2(new BigDecimal("4.20"))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build(),

        // 중간금리 상품 금리 (정액적립식)
        SavingsInterestRates.builder()
            .finPrdtCd("SAVING_MEDIUM_001")
            .intrRateType("S").rsrvType("S").saveTrm(12)
            .intrRate(new BigDecimal("2.50")).intrRate2(new BigDecimal("2.80"))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build(),
        SavingsInterestRates.builder()
            .finPrdtCd("SAVING_MEDIUM_001")
            .intrRateType("S").rsrvType("S").saveTrm(24)
            .intrRate(new BigDecimal("2.60")).intrRate2(new BigDecimal("2.90"))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build(),

        // 자유적립식 상품 금리
        SavingsInterestRates.builder()
            .finPrdtCd("SAVING_FREE_001")
            .intrRateType("S").rsrvType("F").saveTrm(12) // 자유적립식
            .intrRate(new BigDecimal("1.50")).intrRate2(new BigDecimal("1.80"))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build()
    ));

    // 금리 정보 저장 후 연관관계 설정
    List<SavingsInterestRates> savedRates = savingsInterestRatesRepository.saveAll(Arrays.asList(
        // 고금리 상품 금리 (정액적립식)
        SavingsInterestRates.builder()
            .finPrdtCd("SAVING_HIGH_001")
            .intrRateType("S").rsrvType("S").saveTrm(12)
            .intrRate(new BigDecimal("3.50")).intrRate2(new BigDecimal("4.00"))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build(),
        SavingsInterestRates.builder()
            .finPrdtCd("SAVING_HIGH_001")
            .intrRateType("S").rsrvType("S").saveTrm(24)
            .intrRate(new BigDecimal("3.70")).intrRate2(new BigDecimal("4.20"))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build(),

        // 중간금리 상품 금리 (정액적립식)
        SavingsInterestRates.builder()
            .finPrdtCd("SAVING_MEDIUM_001")
            .intrRateType("S").rsrvType("S").saveTrm(12)
            .intrRate(new BigDecimal("2.50")).intrRate2(new BigDecimal("2.80"))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build(),
        SavingsInterestRates.builder()
            .finPrdtCd("SAVING_MEDIUM_001")
            .intrRateType("S").rsrvType("S").saveTrm(24)
            .intrRate(new BigDecimal("2.60")).intrRate2(new BigDecimal("2.90"))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build(),

        // 자유적립식 상품 금리
        SavingsInterestRates.builder()
            .finPrdtCd("SAVING_FREE_001")
            .intrRateType("S").rsrvType("F").saveTrm(12)
            .intrRate(new BigDecimal("1.50")).intrRate2(new BigDecimal("1.80"))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build()
    ));

    // 저장된 금리 정보를 상품별로 분리하여 연관관계 설정
    List<SavingsInterestRates> highRateInterestRates = savedRates.stream()
        .filter(rate -> "SAVING_HIGH_001".equals(rate.getFinPrdtCd()))
        .collect(Collectors.toList());

    List<SavingsInterestRates> mediumRateInterestRates = savedRates.stream()
        .filter(rate -> "SAVING_MEDIUM_001".equals(rate.getFinPrdtCd()))
        .collect(Collectors.toList());

    List<SavingsInterestRates> freeTypeInterestRates = savedRates.stream()
        .filter(rate -> "SAVING_FREE_001".equals(rate.getFinPrdtCd()))
        .collect(Collectors.toList());

    // 양방향 연관관계 설정
    highRateProduct.setFinancialCompany(wooriBank);
    highRateProduct.setInterestRates(highRateInterestRates);

    mediumRateProduct.setFinancialCompany(kbBank);
    mediumRateProduct.setInterestRates(mediumRateInterestRates);

    freeTypeProduct.setFinancialCompany(shinhanSavingsBank);
    freeTypeProduct.setInterestRates(freeTypeInterestRates);

    // 금리 엔티티에도 상품 연관관계 설정
    highRateInterestRates.forEach(rate -> rate.setSavingsProduct(highRateProduct));
    mediumRateInterestRates.forEach(rate -> rate.setSavingsProduct(mediumRateProduct));
    freeTypeInterestRates.forEach(rate -> rate.setSavingsProduct(freeTypeProduct));
  }
}