package com.project.savingbee.filtering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.FinancialCompanies;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.repository.DepositProductsRepository;
import com.project.savingbee.common.repository.FinancialCompaniesRepository;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.filtering.dto.ProductSearchResponse;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.util.KoreanParsing;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Slf4j
public class SearchServiceTest {

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

  /**
   * Mock 데이터
   */
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
        .interestRates(Collections.emptyList())
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
        .interestRates(Collections.emptyList())
        .build();
    savingsProductsRepository.save(testSavings);


    // 금리 정보는 별도로 저장하지 않고 빈 리스트로 처리
  }

  @Test
  @DisplayName("상품 검색 성공")
  void searchProductSuccess() {
    String searchTerm = "테스트";
    log.info("Testing search with term: {}", searchTerm);

    ResponseEntity<ProductSearchResponse> response = searchService.searchProduct(searchTerm);

    log.info("Response status: {}", response.getStatusCode());
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

//    @SuppressWarnings("unchecked")
//    ProductSearchResponse responseBody = (ProductSearchResponse) response.getBody();

    assertThat(response).isNotNull();

    // 테스트 코드 수정 필요
//    @SuppressWarnings("unchecked")
    List<ProductSummaryResponse> products = response.getBody().getProducts();
    assertThat(products).hasSize(2);

    // 예금 상품 확인
    boolean hasDeposit = products.stream()
        .anyMatch(p -> "deposit".equals(p.getProductType()) &&
            "테스트예금상품".equals(p.getFinPrdtNm()));
    assertThat(hasDeposit).isTrue();

    // 적금 상품 확인
    boolean hasSavings = products.stream()
        .anyMatch(p -> "savings".equals(p.getProductType()) &&
            "테스트적금상품".equals(p.getFinPrdtNm()));
    assertThat(hasSavings).isTrue();

    log.info("Found {} products", products.size());
  }

  @Test
  @DisplayName("검색 결과 없음, 인기 상품 반환")
  void searchProductNoResults() {

    String searchTerm = "존재하지않는상품";
    log.info("Testing no results scenario with term: {}", searchTerm);

    ResponseEntity<ProductSearchResponse> response = searchService.searchProduct(searchTerm);

//    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

//    @SuppressWarnings("unchecked")
//    Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

    assertThat(response).isNotNull();

//    @SuppressWarnings("unchecked")
    List<ProductSummaryResponse> products = response.getBody().getProducts();
    assertThat(products).isEmpty();

//    @SuppressWarnings("unchecked")
    List<ProductSummaryResponse> popularProducts = response.getBody().getPopularProducts();
    // null 체크 추가
    if (popularProducts == null) {
      log.error("PopularProducts is null!");
      assertThat(popularProducts).isNotNull();
    } else {
      log.info("PopularProducts size: {}", popularProducts.size());
      assertThat(popularProducts).isNotEmpty();
      assertThat(popularProducts).hasSizeLessThanOrEqualTo(3);
    }

    log.info("Returned {} popular products", popularProducts != null ? popularProducts.size() : 0);
  }

  @Test
  @DisplayName("검색어 유효성 검사")
  void searchProductBadRequest() {

    String shortTerm = "a";
    log.info("Testing invalid query: {}", shortTerm);

    ResponseEntity<ProductSearchResponse> response = searchService.searchProduct(shortTerm);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

//    @SuppressWarnings("unchecked")
//    Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

    assertThat(response).isNotNull();
//    assertThat(response.get("error")).isEqualTo("검색어는 2자 이상 입력해주세요");

    log.info("Invalid query properly rejected");
  }
}