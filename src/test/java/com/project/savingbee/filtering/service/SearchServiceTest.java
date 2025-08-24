package com.project.savingbee.filtering.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.FinancialCompanies;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.repository.DepositProductsRepository;
import com.project.savingbee.common.repository.FinancialCompaniesRepository;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.filtering.dto.ProductSearchResponse;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Slf4j
public class SearchServiceTest {

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;

  @Autowired
  private SearchService searchService;

  @Autowired
  private DepositProductsRepository depositProductsRepository;

  @Autowired
  private SavingsProductsRepository savingsProductsRepository;

  @Autowired
  private FinancialCompaniesRepository financialCompaniesRepository;

  private FinancialCompanies testBank;
  private DepositProducts testDeposit;
  private SavingsProducts testSavings;

  @BeforeEach
  void setUp() {
    // 금융회사 저장
    testBank = FinancialCompanies.builder()
        .finCoNo("TEST001")
        .korCoNm("테스트은행")
        .trnsOrgCode("TEST")
        .orgTypeCode("BANK")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    financialCompaniesRepository.save(testBank);

    // 예금 상품 저장
    testDeposit = DepositProducts.builder()
        .finPrdtCd("DEP_TEST001")
        .finPrdtNm("테스트예금상품")
        .joinWay("영업점,인터넷")
        .mtrtInt("만기시 이자지급")
        .spclCnd("우대조건")
        .joinDeny("제한없음")
        .joinMember("개인")
        .etcNote("기타사항")
        .maxLimit(new BigDecimal("100000000"))
        .dclsStrtDay(LocalDate.now().minusMonths(1))
        .dclsEndDay(LocalDate.now().plusMonths(6))
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .finCoNo("TEST001")
        .minAmount(new BigDecimal("10000"))
        .maxAmount(new BigDecimal("100000000"))
        .financialCompany(testBank)
        .build();
    depositProductsRepository.save(testDeposit);

    // 적금 상품 저장
    testSavings = SavingsProducts.builder()
        .finPrdtCd("SAV_TEST001")
        .finPrdtNm("테스트적금상품")
        .joinWay("영업점,인터넷")
        .mtrtInt("만기시 이자지급")
        .spclCnd("우대조건")
        .joinDeny("제한없음")
        .joinMember("개인")
        .etcNote("기타사항")
        .maxLimit(new BigDecimal("5000000"))
        .dclsStrtDay(LocalDate.now().minusMonths(1))
        .dclsEndDay(LocalDate.now().plusMonths(6))
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .finCoNo("TEST001")
        .financialCompany(testBank)
        .build();
    savingsProductsRepository.save(testSavings);
  }

  @AfterEach
  void cleanup() {
    depositProductsRepository.deleteAll();
    savingsProductsRepository.deleteAll();
    financialCompaniesRepository.deleteAll();
  }

  @Test
  @DisplayName("상품 검색 성공")
  void searchProductSuccess() {
    // Given
    String searchTerm = "테스트";
    log.info("Testing search with term: {}", searchTerm);

    // When
    ResponseEntity<ProductSearchResponse> response = searchService.searchProduct(searchTerm);

    // Then
    log.info("Response status: {}", response.getStatusCode());
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    ProductSearchResponse responseBody = response.getBody();
    List<ProductSummaryResponse> products = responseBody.getProducts();

    log.info("실제 반환된 products 개수: {}", products.size());
    products.forEach(p -> {
      log.info("Product: type={}, name={}, company={}",
          p.getProductType(), p.getFinPrdtNm(), p.getKorCoNm());
    });

    assertThat(products).hasSize(2);
    assertThat(responseBody.getTotalCount()).isEqualTo(2);
    assertThat(responseBody.getSearchTerm()).isEqualTo(searchTerm);

    // 예금 상품 확인
    boolean hasDeposit = products.stream()
        .anyMatch(p -> "deposit".equals(p.getProductType()) &&
            "테스트예금상품".equals(p.getFinPrdtNm()));
    assertThat(hasDeposit).isTrue();

    // 적금 상품 확인 (saving으로 수정)
    boolean hasSavings = products.stream()
        .anyMatch(p -> "saving".equals(p.getProductType()) &&
            "테스트적금상품".equals(p.getFinPrdtNm()));
    assertThat(hasSavings).isTrue();

    log.info("Found {} products", products.size());
  }

  @Test
  @DisplayName("검색 결과 없음, 인기 상품 반환")
  void searchProductNoResults() {
    // Given
    String searchTerm = "존재하지않는상품";
    log.info("Testing no results scenario with term: {}", searchTerm);

    // When
    ResponseEntity<ProductSearchResponse> response = searchService.searchProduct(searchTerm);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    ProductSearchResponse responseBody = response.getBody();
    List<ProductSummaryResponse> products = responseBody.getProducts();
    List<ProductSummaryResponse> popularProducts = responseBody.getPopularProducts();

    assertThat(products).isEmpty();
    assertThat(responseBody.getTotalCount()).isEqualTo(0);
    assertThat(responseBody.getMessage()).isEqualTo("검색 결과가 없어 인기 상품을 추천합니다");

    // 인기 상품 확인
    assertThat(popularProducts).isNotNull();
    assertThat(popularProducts).isNotEmpty();
    assertThat(popularProducts).hasSizeLessThanOrEqualTo(3);

    log.info("Returned {} popular products", popularProducts.size());
  }

  @Test
  @DisplayName("검색어 유효성 검사")
  void searchProductBadRequest() {
    // Given
    String shortTerm = "a";
    log.info("Testing invalid query: {}", shortTerm);

    // When
    ResponseEntity<ProductSearchResponse> response = searchService.searchProduct(shortTerm);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();

    ProductSearchResponse responseBody = response.getBody();
    assertThat(responseBody.getMessage()).isEqualTo("검색어가 유효하지 않습니다.");

    log.info("Invalid query properly rejected");
  }

  @Test
  @DisplayName("null 검색어 처리")
  void searchProductNullQuery() {
    // When
    ResponseEntity<ProductSearchResponse> response = searchService.searchProduct(null);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("검색어가 유효하지 않습니다.");
  }

  @Test
  @DisplayName("빈 문자열 검색어 처리")
  void searchProductEmptyQuery() {
    // When
    ResponseEntity<ProductSearchResponse> response = searchService.searchProduct("");

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("검색어가 유효하지 않습니다.");
  }
}