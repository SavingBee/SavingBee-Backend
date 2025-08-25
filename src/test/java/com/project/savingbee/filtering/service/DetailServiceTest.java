package com.project.savingbee.filtering.service;

import com.project.savingbee.common.entity.*;
import com.project.savingbee.common.repository.DepositInterestRatesRepository;
import com.project.savingbee.common.repository.DepositProductsRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.common.repository.FinancialCompaniesRepository;
import com.project.savingbee.filtering.dto.ProductDetailResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * DetailService 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional // 각 테스트 후 롤백
@DisplayName("DetailService 통합 테스트")
public class DetailServiceTest {

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;

  @Autowired
  private DetailService detailService;

  @Autowired
  private DepositProductsRepository depositProductsRepository;

  @Autowired
  private SavingsProductsRepository savingsProductsRepository;

  @Autowired
  private FinancialCompaniesRepository financialCompaniesRepository;

  @Autowired
  private DepositInterestRatesRepository depositInterestRatesRepository;

  @Autowired
  private SavingsInterestRatesRepository savingsInterestRatesRepository;

  @MockitoBean
  private SearchService searchService;

  private String testDepositId = "DEPOSIT001";
  private String testSavingsId = "SAVINGS001";
  private String testFinCoNo = "0010001";

  @BeforeEach
  void setUp() {
    // 금융회사 데이터
    FinancialCompanies financialCompany = FinancialCompanies.builder()
        .finCoNo(testFinCoNo)
        .korCoNm("테스트은행")
        .orgTypeCode("1")
        .build();
    financialCompaniesRepository.save(financialCompany);

    // 예금 상품 데이터
    DepositProducts deposit = DepositProducts.builder()
        .finPrdtCd(testDepositId)
        .finPrdtNm("테스트정기예금")
        .finCoNo(testFinCoNo)
        .financialCompany(financialCompany)
        .joinWay("인터넷")
        .joinDeny("1")
        .joinMember("누구나")
        .maxLimit(BigDecimal.valueOf(100000000))
        .spclCnd("우대조건없음")
        .mtrtInt("만기후자동해지")
        .etcNote("기타사항")
        .dclsStrtDay(LocalDate.of(2024, 1, 1))
        .dclsEndDay(LocalDate.of(2024, 12, 31))
        .isActive(true)
        .minAmount(BigDecimal.valueOf(1000000))
        .maxAmount(BigDecimal.valueOf(500000000))
        .build();
    depositProductsRepository.save(deposit);

    // 예금 금리 정보
    DepositInterestRates depositRate = DepositInterestRates.builder()
        .finPrdtCd(testDepositId)
        .intrRateType("S")
        .saveTrm(12)
        .intrRate(BigDecimal.valueOf(3.0))
        .intrRate2(BigDecimal.valueOf(3.5))
        .build();

    deposit.setInterestRates(List.of(depositRate));
    depositProductsRepository.save(deposit);

    // 적금 상품 데이터
    SavingsProducts savings = SavingsProducts.builder()
        .finPrdtCd(testSavingsId)
        .finPrdtNm("테스트정기적금")
        .finCoNo(testFinCoNo)
        .financialCompany(financialCompany)
        .joinWay("인터넷")
        .joinDeny("1")
        .joinMember("누구나")
        .maxLimit(BigDecimal.valueOf(1000000))
        .spclCnd("우대조건없음")
        .mtrtInt("만기후자동해지")
        .etcNote("기타사항")
        .dclsStrtDay(LocalDate.of(2024, 1, 1))
        .dclsEndDay(LocalDate.of(2024, 12, 31))
        .isActive(true)
        .build();
    savingsProductsRepository.save(savings);

    // 적금 금리 정보
    SavingsInterestRates savingsRate = SavingsInterestRates.builder()
        .finPrdtCd(testSavingsId)
        .intrRateType("S")
        .rsrvType("S")
        .saveTrm(12)
        .intrRate(BigDecimal.valueOf(3.2))
        .intrRate2(BigDecimal.valueOf(3.7))
        .monthlyLimitMin(BigDecimal.valueOf(100000))
        .monthlyLimitMax(BigDecimal.valueOf(1000000))
        .build();

    savings.setInterestRates(List.of(savingsRate));
    savingsProductsRepository.save(savings);
  }

  @Test
  @DisplayName("예금 상품 정상 조회")
  void getDepositProductDetail_Success() {
    // when
    ProductDetailResponse result = detailService.getProductDetail(testDepositId);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getFinPrdtCd()).isEqualTo(testDepositId);
    assertThat(result.getFinPrdtNm()).isEqualTo("테스트정기예금");
    assertThat(result.getProductType()).isEqualTo("deposit");
    assertThat(result.getKorCoNm()).isEqualTo("테스트은행");
    assertThat(result.getJoinDenyNm()).isEqualTo("제한없음"); // "1" → "제한없음"
    assertThat(result.getInterestRates()).hasSize(1);

    ProductDetailResponse.InterestRateOption rateOption = result.getInterestRates().get(0);
    assertThat(rateOption.getSaveTrm()).isEqualTo(12);
    assertThat(rateOption.getIntrRate()).isEqualTo(BigDecimal.valueOf(3.0));
    assertThat(rateOption.getIntrRate2()).isEqualTo(BigDecimal.valueOf(3.5));
    assertThat(rateOption.getIntrRateTypeNm()).isEqualTo("단리"); // "S" → "단리"

    // 조회수 증가 로직 검증
    then(searchService).should().addToViewedProductsCache(testDepositId);
  }

  @Test
  @DisplayName("적금 상품 정상 조회")
  void getSavingsProductDetail_Success() {
    // when
    ProductDetailResponse result = detailService.getProductDetail(testSavingsId);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getFinPrdtCd()).isEqualTo(testSavingsId);
    assertThat(result.getFinPrdtNm()).isEqualTo("테스트정기적금");
    assertThat(result.getProductType()).isEqualTo("saving");
    assertThat(result.getKorCoNm()).isEqualTo("테스트은행");
    assertThat(result.getInterestRates()).hasSize(1);

    ProductDetailResponse.InterestRateOption rateOption = result.getInterestRates().get(0);
    assertThat(rateOption.getSaveTrm()).isEqualTo(12);
    assertThat(rateOption.getIntrRate()).isEqualTo(BigDecimal.valueOf(3.2));
    assertThat(rateOption.getIntrRate2()).isEqualTo(BigDecimal.valueOf(3.7));
    assertThat(rateOption.getRsrvTypeNm()).isEqualTo("정액적립식"); // 코드 "S" → "정액적립식"

    // totalMaxLimit 계산 검증 (12개월 * 100만원 = 1200만원)
    BigDecimal expectedTotal = BigDecimal.valueOf(1000000).multiply(BigDecimal.valueOf(12));
    assertThat(rateOption.getTotalMaxLimit()).isEqualTo(expectedTotal);

    // 조회수 증가 로직 검증
    then(searchService).should().addToViewedProductsCache(testSavingsId);
  }

  @Test
  @DisplayName("비활성화된 예금 상품도 조회 가능하지만 조회수 증가는 안 함")
  void getDepositProductDetail_InactiveProduct_Success_NoViewCount() {
    // given - 상품 비활성화 처리
    DepositProducts deposit = depositProductsRepository.findById(testDepositId).orElseThrow();
    deposit.setIsActive(false);
    depositProductsRepository.save(deposit);

    // when
    ProductDetailResponse result = detailService.getProductDetail(testDepositId);

    // then - 조회는 성공
    assertThat(result).isNotNull();
    assertThat(result.getFinPrdtCd()).isEqualTo(testDepositId);
    assertThat(result.getProductType()).isEqualTo("deposit");
    assertThat(result.getKorCoNm()).isEqualTo("테스트은행");

    // 조회수 증가 호출되지 않아야 함
    then(searchService).should(never()).addToViewedProductsCache(any());
  }

  @Test
  @DisplayName("비활성화된 적금 상품도 조회 가능하지만 조회수 증가는 안 함")
  void getSavingsProductDetail_InactiveProduct_Success_NoViewCount() {
    // given - 상품 비활성화 처리
    SavingsProducts savings = savingsProductsRepository.findById(testSavingsId).orElseThrow();
    savings.setIsActive(false);
    savingsProductsRepository.save(savings);

    // when
    ProductDetailResponse result = detailService.getProductDetail(testSavingsId);

    // then - 조회는 성공
    assertThat(result).isNotNull();
    assertThat(result.getFinPrdtCd()).isEqualTo(testSavingsId);
    assertThat(result.getProductType()).isEqualTo("saving");
    assertThat(result.getKorCoNm()).isEqualTo("테스트은행");

    // 조회수 증가 호출되지 않아야 함
    then(searchService).should(never()).addToViewedProductsCache(any());
  }

  @Test
  @DisplayName("존재하지 않는 상품 조회 시 예외 발생")
  void getProductDetail_NotFound_ThrowsException() {
    // given
    String nonExistentId = "NONEXISTENT001";

    // when & then
    assertThatThrownBy(() -> detailService.getProductDetail(nonExistentId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("존재하지 않는 상품입니다: " + nonExistentId);

    // 조회수 증가 호출되지 않아야 함
    then(searchService).should(never()).addToViewedProductsCache(any());
  }

  @Test
  @DisplayName("null 상품코드 입력 시 예외 발생")
  void getProductDetail_NullProductId_ThrowsException() {
    // when & then
    assertThatThrownBy(() -> detailService.getProductDetail(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("상품 코드가 유효하지 않습니다.");
  }

  @Test
  @DisplayName("빈 문자열 상품코드 입력 시 예외 발생")
  void getProductDetail_EmptyProductId_ThrowsException() {
    // when & then
    assertThatThrownBy(() -> detailService.getProductDetail("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("상품 코드가 유효하지 않습니다.");
  }

  @Test
  @DisplayName("조회수 증가 실패 시에도 상품 조회는 정상 진행")
  void getProductDetail_ViewCountIncreaseFails_StillReturnsProduct() {
    // given - 조회수 증가에서 예외 설정
    willThrow(new RuntimeException("Cache error"))
        .given(searchService).addToViewedProductsCache(testDepositId);

    // when
    ProductDetailResponse result = detailService.getProductDetail(testDepositId);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getFinPrdtCd()).isEqualTo(testDepositId);

    // 조회수 증가는 시도되었지만 실패
    then(searchService).should().addToViewedProductsCache(testDepositId);
  }
}