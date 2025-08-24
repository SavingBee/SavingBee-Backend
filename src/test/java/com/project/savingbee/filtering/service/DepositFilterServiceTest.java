package com.project.savingbee.filtering.service;

import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.DepositInterestRates;
import com.project.savingbee.common.entity.FinancialCompanies;
import com.project.savingbee.common.repository.DepositProductsRepository;
import com.project.savingbee.common.repository.DepositInterestRatesRepository;
import com.project.savingbee.common.repository.FinancialCompaniesRepository;
import com.project.savingbee.filtering.dto.DepositFilterRequest;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.dto.RangeFilter;
import com.project.savingbee.filtering.dto.SortFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("예금 필터 서비스 통합 테스트")
class DepositFilterServiceTest {

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;

  @Autowired
  private DepositFilterService depositFilterService;

  @Autowired
  private DepositProductsRepository depositProductsRepository;

  @Autowired
  private DepositInterestRatesRepository depositInterestRatesRepository;

  @Autowired
  private FinancialCompaniesRepository financialCompaniesRepository;

  @BeforeEach
  void setUp() {
    // 테스트 데이터 초기화
    depositInterestRatesRepository.deleteAll();
    depositProductsRepository.deleteAll();
    financialCompaniesRepository.deleteAll();

    // 금융회사 생성
    FinancialCompanies wooriBank = financialCompaniesRepository.save(
        FinancialCompanies.builder()
            .finCoNo("0010001")
            .korCoNm("우리은행")
            .build()
    );

    FinancialCompanies kbBank = financialCompaniesRepository.save(
        FinancialCompanies.builder()
            .finCoNo("0010002")
            .korCoNm("국민은행")
            .build()
    );

    FinancialCompanies shinhanBank = financialCompaniesRepository.save(
        FinancialCompanies.builder()
            .finCoNo("0010003")
            .korCoNm("신한은행")
            .build()
    );

    // 예금 상품 생성
    DepositProducts highRateProduct = depositProductsRepository.save(
        DepositProducts.builder()
            .finPrdtCd("HIGH_RATE_001")
            .finPrdtNm("고금리특별예금")
            .finCoNo("0010001")
            .joinWay("영업점,인터넷,스마트폰")
            .spclCnd("신규고객 우대")
            .joinDeny("1")
            .joinMember("개인")
            .maxLimit(new BigDecimal("100000000"))
            .isActive(true)
            .dclsStrtDay(LocalDate.now())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build()
    );

    DepositProducts mediumRateProduct = depositProductsRepository.save(
        DepositProducts.builder()
            .finPrdtCd("MEDIUM_RATE_001")
            .finPrdtNm("안정금리예금")
            .finCoNo("0010002")
            .joinWay("영업점")
            .spclCnd("일반고객 대상")
            .joinDeny("1")
            .joinMember("개인")
            .maxLimit(new BigDecimal("50000000"))
            .isActive(true)
            .dclsStrtDay(LocalDate.now())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build()
    );

    DepositProducts lowRateProduct = depositProductsRepository.save(
        DepositProducts.builder()
            .finPrdtCd("LOW_RATE_001")
            .finPrdtNm("기본예금")
            .finCoNo("0010003")
            .joinWay("영업점,인터넷")
            .spclCnd("제한없음")
            .joinDeny("1")
            .joinMember("개인")
            .maxLimit(new BigDecimal("30000000"))
            .isActive(true)
            .dclsStrtDay(LocalDate.now())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build()
    );

    // 고금리 상품 금리
    List<DepositInterestRates> highRateInterestRates = depositInterestRatesRepository.saveAll(
        Arrays.asList(
            DepositInterestRates.builder()
                .finPrdtCd("HIGH_RATE_001")
                .intrRateType("S")
                .saveTrm(12)
                .intrRate(new BigDecimal("3.50"))
                .intrRate2(new BigDecimal("4.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
            DepositInterestRates.builder()
                .finPrdtCd("HIGH_RATE_001")
                .intrRateType("S")
                .saveTrm(24)
                .intrRate(new BigDecimal("3.70"))
                .intrRate2(new BigDecimal("4.20"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
            DepositInterestRates.builder()
                .finPrdtCd("HIGH_RATE_001")
                .intrRateType("M")
                .saveTrm(12)
                .intrRate(new BigDecimal("3.45"))
                .intrRate2(new BigDecimal("3.95"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        ));

    // 중간금리 상품 금리
    List<DepositInterestRates> mediumRateInterestRates = depositInterestRatesRepository.saveAll(
        Arrays.asList(
            DepositInterestRates.builder()
                .finPrdtCd("MEDIUM_RATE_001")
                .intrRateType("S")
                .saveTrm(12)
                .intrRate(new BigDecimal("2.50"))
                .intrRate2(new BigDecimal("2.80"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
            DepositInterestRates.builder()
                .finPrdtCd("MEDIUM_RATE_001")
                .intrRateType("S")
                .saveTrm(24)
                .intrRate(new BigDecimal("2.60"))
                .intrRate2(new BigDecimal("2.90"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        ));

    // 낮은금리 상품 금리
    List<DepositInterestRates> lowRateInterestRates = Arrays.asList(
        depositInterestRatesRepository.save(
            DepositInterestRates.builder()
                .finPrdtCd("LOW_RATE_001")
                .intrRateType("S")
                .saveTrm(12)
                .intrRate(new BigDecimal("1.50"))
                .intrRate2(new BigDecimal("1.80"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        )
    );

    // 양방향 연관관계 설정
    highRateProduct.setFinancialCompany(wooriBank);
    highRateProduct.setInterestRates(highRateInterestRates);

    mediumRateProduct.setFinancialCompany(kbBank);
    mediumRateProduct.setInterestRates(mediumRateInterestRates);

    lowRateProduct.setFinancialCompany(shinhanBank);
    lowRateProduct.setInterestRates(lowRateInterestRates);

    // 금리 엔티티에도 상품 연관관계 설정
    highRateInterestRates.forEach(rate -> rate.setDepositProduct(highRateProduct));
    mediumRateInterestRates.forEach(rate -> rate.setDepositProduct(mediumRateProduct));
    lowRateInterestRates.forEach(rate -> rate.setDepositProduct(lowRateProduct));
  }

  @Test
  @DisplayName("기본 필터링 (금리 내림차순)")
  void testBasicIntegrationFiltering() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(null)  // 필터 없음
        .build();
    request.setPage(1);
    request.setSize(10);

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(3);
    assertThat(result.getTotalElements()).isEqualTo(3);

    // 최고 금리 내림차순 기본 정렬
    List<ProductSummaryResponse> products = result.getContent();
    assertThat(products.get(0).getFinPrdtCd()).isEqualTo("HIGH_RATE_001");
    assertThat(products.get(0).getMaxIntrRate()).isEqualTo(new BigDecimal("4.20"));

    assertThat(products.get(1).getFinPrdtCd()).isEqualTo("MEDIUM_RATE_001");
    assertThat(products.get(1).getMaxIntrRate()).isEqualTo(new BigDecimal("2.90"));

    assertThat(products.get(2).getFinPrdtCd()).isEqualTo("LOW_RATE_001");
    assertThat(products.get(2).getMaxIntrRate()).isEqualTo(new BigDecimal("1.80"));
  }

  @Test
  @DisplayName("금융회사 필터링")
  void testFinancialCompanyFiltering() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(DepositFilterRequest.Filters.builder()
            .finCoNo(Arrays.asList("0010001", "0010002"))
            .build())
        .build();
    request.setPage(1);
    request.setSize(10);

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(2);

    List<String> companyNames = result.getContent().stream()
        .map(ProductSummaryResponse::getKorCoNm)
        .toList();
    assertThat(companyNames).containsExactlyInAnyOrder("우리은행", "국민은행");
  }

  @Test
  @DisplayName("저축기간 필터링")
  void testSavingTermFiltering() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(DepositFilterRequest.Filters.builder()
            .saveTrm(Arrays.asList(24))
            .build())
        .build();
    request.setPage(1);
    request.setSize(10);

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    // 24개월 옵션이 있는 상품들만
    assertThat(result.getContent()).hasSize(2);

    List<String> productCodes = result.getContent().stream()
        .map(ProductSummaryResponse::getFinPrdtCd)
        .toList();
    assertThat(productCodes).containsExactlyInAnyOrder("HIGH_RATE_001", "MEDIUM_RATE_001");
  }

  @Test
  @DisplayName("이자계산방식 필터링")
  void testInterestRateTypeFiltering() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(DepositFilterRequest.Filters.builder()
            .intrRateType(Arrays.asList("M"))
            .build())
        .build();
    request.setPage(1);
    request.setSize(10);

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    // 복리 옵션이 있는 상품만
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getFinPrdtCd()).isEqualTo("HIGH_RATE_001");
  }

  @Test
  @DisplayName("기본금리 범위 필터링")
  void testBaseInterestRateRangeFiltering() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(DepositFilterRequest.Filters.builder()
            .intrRate(RangeFilter.builder()
                .min(new BigDecimal("2.00"))
                .max(new BigDecimal("3.00"))
                .build())
            .build())
        .build();
    request.setPage(1);
    request.setSize(10);

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    // 기본금리가 해당 범위에 속하는 상품만
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getFinPrdtCd()).isEqualTo("MEDIUM_RATE_001");
  }

  @Test
  @DisplayName("우대금리 범위 필터링")
  void testPreferentialInterestRateRangeFiltering() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(DepositFilterRequest.Filters.builder()
            .intrRate2(RangeFilter.builder()
                .min(new BigDecimal("2.00"))
                .max(new BigDecimal("3.50"))
                .build())
            .build())
        .build();
    request.setPage(1);
    request.setSize(10);

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    // 우대 금리가 해당 범위에 속하는 상품만
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getFinPrdtCd()).isEqualTo("MEDIUM_RATE_001");
  }

  @Test
  @DisplayName("가입한도 필터링")
  void testMaxLimitFiltering() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(DepositFilterRequest.Filters.builder()
            .maxLimit(RangeFilter.builder()
                .min(new BigDecimal("40000000"))
                .build())
            .build())
        .build();
    request.setPage(1);
    request.setSize(10);

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(2);

    List<String> productCodes = result.getContent().stream()
        .map(ProductSummaryResponse::getFinPrdtCd)
        .toList();
    assertThat(productCodes).containsExactlyInAnyOrder("HIGH_RATE_001", "MEDIUM_RATE_001");
  }

//  @Test
//  @DisplayName("우대조건 텍스트 검색")
//  void testPreferentialConditionTextSearch() {
//    // Given
//    DepositFilterRequest request = DepositFilterRequest.builder()
//        .page(1)
//        .size(10)
//        .filters(DepositFilterRequest.Filters.builder()
//            .joinWay(Arrays.asList("신규"))
//            .build())
//        .build();
//
//    // When
//    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);
//
//    // Then
//    assertThat(result).isNotNull();
//    // "신규고객 우대" 조건이 있는 상품만
//    assertThat(result.getContent()).hasSize(1);
//    assertThat(result.getContent().get(0).getFinPrdtCd()).isEqualTo("HIGH_RATE_001");
//  }

  @Test
  @DisplayName("우대조건 텍스트 검색 - 코드 분리로 변경")
  void testPreferentialConditionTextSearch() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(DepositFilterRequest.Filters.builder()
            .joinWay(Arrays.asList("첫거래"))  // PreConMapping의 displayName
            .build())
        .build();
    request.setPage(1);
    request.setSize(10);

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getFinPrdtCd()).isEqualTo("HIGH_RATE_001");
  }

  @Test
  @DisplayName("복합 필터링")
  void testComplexFiltering() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(DepositFilterRequest.Filters.builder()
            .finCoNo(Arrays.asList("0010001", "0010002"))
            .saveTrm(Arrays.asList(12))
            .intrRateType(Arrays.asList("S"))
            .intrRate2(RangeFilter.builder()
                .min(new BigDecimal("3.00"))
                .build())
            .build())
        .build();
    request.setPage(1);
    request.setSize(10);

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    // 모든 조건을 만족하는 상품만
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getFinPrdtCd()).isEqualTo("HIGH_RATE_001");
    assertThat(result.getContent().get(0).getKorCoNm()).isEqualTo("우리은행");
  }

  @Test
  @DisplayName("페이징 기능")
  void testPagination() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(null)
        .build();
    request.setPage(1);
    request.setSize(2);

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getTotalElements()).isEqualTo(3);
    assertThat(result.getTotalPages()).isEqualTo(2);
    assertThat(result.hasNext()).isTrue();

    // 두 번째 페이지 요청
    DepositFilterRequest secondPageRequest = DepositFilterRequest.builder()
        .filters(null)
        .build();
    secondPageRequest.setPage(2);
    secondPageRequest.setSize(2);

    Page<ProductSummaryResponse> secondPage = depositFilterService.depositFilter(secondPageRequest);

    assertThat(secondPage.getContent()).hasSize(1);
    assertThat(secondPage.hasNext()).isFalse();
  }

  @Test
  @DisplayName("최고금리 내림차순 정렬")
  void testMaxInterestRateDescendingSortAtServiceLevel() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(null)
        .build();
    request.setPage(1);
    request.setSize(10);
    request.setSort(SortFilter.builder()
        .field("intr_rate2")
        .order("desc")
        .build());

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(3);

    // 최고금리 내림차순
    List<String> productCodes = result.getContent().stream()
        .map(ProductSummaryResponse::getFinPrdtCd)
        .toList();

    assertThat(productCodes).containsExactly("HIGH_RATE_001", "MEDIUM_RATE_001", "LOW_RATE_001");

    List<BigDecimal> maxRates = result.getContent().stream()
        .map(ProductSummaryResponse::getMaxIntrRate)
        .toList();

    assertThat(maxRates).containsExactly(
        new BigDecimal("4.20"),
        new BigDecimal("2.90"),
        new BigDecimal("1.80")
    );
  }

  @Test
  @DisplayName("기본금리 오름차순 정렬")
  void testBaseInterestRateAscendingSortAtServiceLevel() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(null)
        .build();
    request.setPage(1);
    request.setSize(10);
    request.setSort(SortFilter.builder()
        .field("intr_rate")
        .order("asc")
        .build());
    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(3);

    // 기본금리 오름차순
    List<String> productCodes = result.getContent().stream()
        .map(ProductSummaryResponse::getFinPrdtCd)
        .toList();

    assertThat(productCodes).containsExactly("LOW_RATE_001", "MEDIUM_RATE_001", "HIGH_RATE_001");
  }

  @Test
  @DisplayName("상품명 정렬")
  void testProductNameSortingAtDatabaseLevel() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(null)
        .build();
    request.setPage(1);
    request.setSize(10);
    request.setSort(SortFilter.builder()
        .field("fin_prdt_nm")
        .order("asc")
        .build());

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(3);

    // 상품명 오름차순 정렬 확인 - DB 한글 정렬
    List<String> productNames = result.getContent().stream()
        .map(ProductSummaryResponse::getFinPrdtNm)
        .toList();

    // 정렬 순서가 올바른지 확인
    assertThat(productNames.get(0)).isEqualTo("고금리특별예금");
    assertThat(productNames.get(2)).isEqualTo("안정금리예금");
  }

  @Test
  @DisplayName("빈 결과 처리")
  void testEmptyResult() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(DepositFilterRequest.Filters.builder()
            .finCoNo(Arrays.asList("9999999")) // 존재하지 않는 금융회사
            .build())
        .build();
    request.setPage(1);
    request.setSize(10);

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isEqualTo(0);
    assertThat(result.getTotalPages()).isEqualTo(0);
  }

  @Test
  @DisplayName("DTO 변환 정확성 검증")
  void testDtoConversionAccuracy() {
    // Given
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(null)
        .build();
    request.setPage(1);
    request.setSize(1);

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);

    ProductSummaryResponse response = result.getContent().get(0);
    assertThat(response.getFinPrdtCd()).isEqualTo("HIGH_RATE_001");
    assertThat(response.getFinPrdtNm()).isEqualTo("고금리특별예금");
    assertThat(response.getKorCoNm()).isEqualTo("우리은행");
    assertThat(response.getProductType()).isEqualTo("deposit");
    assertThat(response.getMaxIntrRate()).isEqualTo(new BigDecimal("4.20"));
    assertThat(response.getBaseIntrRate()).isEqualTo(new BigDecimal("3.70"));
  }

  @Test
  @DisplayName("예외 처리 (null 요청)")
  void testNullRequestHandling() {
    // When & Then
    assertThatThrownBy(() -> depositFilterService.depositFilter(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("필터링 요청이 null입니다.");
  }

  @Test
  @DisplayName("상품명 정렬 - JOIN 없이 페이징 테스트")
  void testProductNameSortingWithoutJoin() {
    // Given - 금리 필터 없음
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(null)  // JOIN 발생하지 않음
        .build();
    request.setPage(1);
    request.setSize(1);
    request.setSort(SortFilter.builder()
        .field("fin_prdt_nm")
        .order("asc")
        .build());

    // When
    Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

    // Then
    assertThat(result.getContent()).hasSize(1);  // 이제 성공할 것
  }
}