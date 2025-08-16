package com.project.savingbee.connectApi.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.project.savingbee.common.entity.SavingsInterestRates;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.repository.FinancialCompaniesRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.connectApi.dto.SavingApiResponse;
import com.project.savingbee.connectApi.util.ApiParsing;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList; // 추가
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils; // 추가된 import

/**
 * SavingConnectApi 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Slf4j
@DisplayName("적금 API 연결 테스트")
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
public class SavingConnectApiTest {

  @Autowired
  private SavingConnectApi savingConnectApi;

  @Autowired
  private SavingsProductsRepository savingsProductsRepository;

  @Autowired
  private SavingsInterestRatesRepository savingsInterestRatesRepository;

  @Autowired
  private FinancialCompaniesRepository financialCompaniesRepository;

  @Value("${api.money.key}")
  private String apiKey;

  @BeforeEach
  void setUp() {
    cleanDatabase();
    log.debug("테스트 시작 전 DB 초기화 완료");
  }

  @AfterEach
  void tearDown() {
    cleanDatabase();
    log.debug("테스트 완료 후 DB 초기화 완료");
  }

  /**
   * Mock 데이터를 사용한 단위 테스트
   */
  @Test
  @DisplayName("Mock 데이터로 적금 상품 저장 테스트")
  @Rollback
  void mockDataSaveTest() {

    SavingApiResponse mockResponse = createMockResponse();

    invokeProcessSavingApiResponse(mockResponse);

    assertThat(financialCompaniesRepository.count()).isEqualTo(2); // 우리은행, SC은행
    assertThat(savingsProductsRepository.count()).isEqualTo(2); // 2개 상품
    assertThat(savingsInterestRatesRepository.count()).isEqualTo(3); // 3개 금리 옵션

    // 첫 번째 상품 검증
    SavingsProducts savedProduct1 = savingsProductsRepository.findById("WR0001T").orElse(null);
    assertThat(savedProduct1).isNotNull();
    assertThat(savedProduct1.getFinPrdtNm()).isEqualTo("테스트꿈적금");
    assertThat(savedProduct1.getMaxLimit()).isEqualTo(new BigDecimal("3000000"));
    assertThat(savedProduct1.getDclsEndDay()).isNull(); // 실제 데이터처럼 null

    // 두 번째 상품 검증
    SavingsProducts savedProduct2 = savingsProductsRepository.findById("SC0001T").orElse(null);
    assertThat(savedProduct2).isNotNull();
    assertThat(savedProduct2.getFinPrdtNm()).isEqualTo("테스트희망적금");
    assertThat(savedProduct2.getMaxLimit()).isEqualTo(new BigDecimal("300000"));

    // 월 적립액 파싱 검증
    List<SavingsInterestRates> sc_rates = savingsInterestRatesRepository.findByFinPrdtCd("SC0001T");
    SavingsInterestRates scRate = sc_rates.get(0);
    assertThat(scRate.getMonthlyLimitMin()).isEqualTo(new BigDecimal("50000")); // 최소 5만원
    assertThat(scRate.getMonthlyLimitMax()).isEqualTo(new BigDecimal("300000")); // 최대 30만원

    // 월 적립액 정보 없음
    List<SavingsInterestRates> wr_rates = savingsInterestRatesRepository.findByFinPrdtCd("WR0001T");
    for (SavingsInterestRates rate : wr_rates) {
      assertThat(rate.getMonthlyLimitMin()).isNull();
      assertThat(rate.getMonthlyLimitMax()).isNull();
    }

    log.info("Mock 데이터 저장 테스트 성공");
  }

  /**
   * 중복 데이터 처리 테스트
   */
  @Test
  @DisplayName("중복 데이터 처리 테스트")
  @Rollback
  void duplicateDataHandlingTest() {
    SavingApiResponse mockResponse = createMockResponse();

    // 첫 번째 저장
    invokeProcessSavingApiResponse(mockResponse);
    long firstCompaniesCount = financialCompaniesRepository.count();
    long firstProductsCount = savingsProductsRepository.count();
    long firstRatesCount = savingsInterestRatesRepository.count();

    // 두 번째 저장
    invokeProcessSavingApiResponse(mockResponse);
    long secondCompaniesCount = financialCompaniesRepository.count();
    long secondProductsCount = savingsProductsRepository.count();
    long secondRatesCount = savingsInterestRatesRepository.count();

    // 중복 저장되지 않음을 확인
    assertThat(firstCompaniesCount).isEqualTo(secondCompaniesCount);
    assertThat(firstProductsCount).isEqualTo(secondProductsCount);
    assertThat(firstRatesCount).isEqualTo(secondRatesCount);

    assertThat(financialCompaniesRepository.count()).isEqualTo(2);
    assertThat(savingsProductsRepository.count()).isEqualTo(2);
    assertThat(savingsInterestRatesRepository.count()).isEqualTo(3);

    log.info("중복 데이터 처리 테스트 성공");
  }

  /**
   * ApiParsing 클래스 적금 특화 기능 테스트
   */
  @Test
  @DisplayName("ApiParsing 클래스 적금 특화 파싱 테스트")
  void apiParsingForSavingsTest() {
    // 패턴 테스트
    String etcNote1 = "1. 1인당 가입한도 : 월 30만원\n2. 월 적립액은 예금을 가입하는 때에 정하며, 계약기간 중에 변경할 수 없음\n3. 월 적립액 최소 5만원 이상 최대 30만원 이하";
    BigDecimal minLimit1 = ApiParsing.parseMonthlyLimitMin(etcNote1);
    BigDecimal maxLimit1 = ApiParsing.parseMonthlyLimitMax(etcNote1);

    assertThat(minLimit1).isEqualTo(new BigDecimal("50000")); // 5만원
    assertThat(maxLimit1).isEqualTo(new BigDecimal("300000")); // 30만원

    // 일반적인 월 적립 한도 패턴 테스트
    String etcNote2 = "월 적립액 5만원이상 100만원이하";
    BigDecimal minLimit2 = ApiParsing.parseMonthlyLimitMin(etcNote2);
    BigDecimal maxLimit2 = ApiParsing.parseMonthlyLimitMax(etcNote2);

    assertThat(minLimit2).isEqualTo(new BigDecimal("50000"));
    assertThat(maxLimit2).isEqualTo(new BigDecimal("1000000"));

    // 다른 패턴 테스트
    String etcNote3 = "적립액은 월 최소 10만원 이상 최대 300만원 이하";
    BigDecimal minLimit3 = ApiParsing.parseMonthlyLimitMin(etcNote3);
    BigDecimal maxLimit3 = ApiParsing.parseMonthlyLimitMax(etcNote3);

    assertThat(minLimit3).isEqualTo(new BigDecimal("100000"));
    assertThat(maxLimit3).isEqualTo(new BigDecimal("3000000"));

    // "해당없음"인 경우
    String etcNote4 = "해당없음";
    BigDecimal minLimit4 = ApiParsing.parseMonthlyLimitMin(etcNote4);
    BigDecimal maxLimit4 = ApiParsing.parseMonthlyLimitMax(etcNote4);

    assertThat(minLimit4).isNull();
    assertThat(maxLimit4).isNull();

    // null인 경우
    BigDecimal minLimitNull = ApiParsing.parseMonthlyLimitMin(null);
    BigDecimal maxLimitNull = ApiParsing.parseMonthlyLimitMax(null);

    assertThat(minLimitNull).isNull();
    assertThat(maxLimitNull).isNull();

    log.info("적금 특화 파싱 테스트 성공");
  }

  /**
   * 데이터 무결성 검증 테스트
   */
  @Test
  @DisplayName("데이터 무결성 검증 테스트")
  @Rollback
  void dataIntegrityTest() {

    SavingApiResponse mockResponse = createMockResponse();

    invokeProcessSavingApiResponse(mockResponse);

    verifyDataIntegrity();

    log.info("데이터 무결성 검증 테스트 성공");
  }

  /**
   * 적금 특화 필드 저장 테스트
   */
  @Test
  @DisplayName("적금 특화 필드 저장 테스트")
  @Rollback
  void savingsSpecificFieldsTest() {

    SavingApiResponse mockResponse = createMockResponse();

    invokeProcessSavingApiResponse(mockResponse);

    List<SavingsInterestRates> rates = savingsInterestRatesRepository.findAll();

    for (SavingsInterestRates rate : rates) {
      // 적립유형 확인
      assertThat(rate.getRsrvType()).isIn("S", "F");

      // 기본 필드들 확인
      assertThat(rate.getIntrRateType()).isNotNull();
      assertThat(rate.getSaveTrm()).isNotNull();
      assertThat(rate.getIntrRate()).isNotNull();
      assertThat(rate.getFinPrdtCd()).isNotNull();
    }

    log.info("적금 특화 필드 저장 테스트 성공");
  }

  /**
   * 예외 상황 처리 테스트
   */
  @Test
  @DisplayName("예외 상황 처리 테스트")
  @Rollback
  void exceptionHandlingTest() {
    // null 응답 처리 테스트
    invokeProcessSavingApiResponse(null);
    assertThat(savingsProductsRepository.count()).isEqualTo(0);

    // 빈 baseList 처리 테스트
    SavingApiResponse emptyResponse = new SavingApiResponse();
    SavingApiResponse.SavingResult emptyResult = new SavingApiResponse.SavingResult();
    emptyResult.setErrorCode("000");
    emptyResult.setBaseList(Arrays.asList());
    emptyResult.setOptionList(Arrays.asList());
    emptyResponse.setResult(emptyResult);

    invokeProcessSavingApiResponse(emptyResponse);
    assertThat(savingsProductsRepository.count()).isEqualTo(0);

    log.info("예외 상황 처리 테스트 성공");
  }

  // 헬퍼 메서드

  /**
   * DB 정리
   */
  private void cleanDatabase() {
    savingsInterestRatesRepository.deleteAll();
    savingsProductsRepository.deleteAll();
    financialCompaniesRepository.deleteAll();
  }

  /**
   * 데이터 무결성 검증
   */
  private void verifyDataIntegrity() {
    List<SavingsProducts> products = savingsProductsRepository.findAll();
    List<SavingsInterestRates> rates = savingsInterestRatesRepository.findAll();

    // 상품의 금융회사 확인
    for (SavingsProducts product : products) {
      assertThat(financialCompaniesRepository.existsById(product.getFinCoNo()))
          .withFailMessage("상품 %s의 금융회사 %s가 존재하지 않습니다",
              product.getFinPrdtCd(), product.getFinCoNo())
          .isTrue();
    }

    // 금리의 상품 확인
    for (SavingsInterestRates rate : rates) {
      assertThat(savingsProductsRepository.existsById(rate.getFinPrdtCd()))
          .withFailMessage("금리의 상품 %s이 존재하지 않습니다", rate.getFinPrdtCd())
          .isTrue();
    }
  }

  /**
   * 저장된 데이터 샘플 로그 출력
   */
  private void logSavedDataSample() {
    log.info("=== 저장된 적금 데이터 확인 ===");

    List<SavingsProducts> products = savingsProductsRepository.findAll();
    if (products.isEmpty()) {
      log.warn("저장된 상품이 없습니다.");
      return;
    }

    SavingsProducts product = products.get(0);
    log.info("상품: {} ({})", product.getFinPrdtNm(), product.getFinPrdtCd());
    log.info("   - 가입방법: {}", product.getJoinWay());
    log.info("   - 가입제한: {}", product.getJoinDeny());
    log.info("   - 월 가입한도: {}", product.getMaxLimit());
    log.info("   - 공시기간: {} ~ {}", product.getDclsStrtDay(), product.getDclsEndDay());

    // 금융회사 정보 확인
    String finCoNo = product.getFinCoNo();
    financialCompaniesRepository.findById(finCoNo).ifPresentOrElse(
        company -> log.info("💼 금융회사: {} ({})", company.getKorCoNm(), company.getFinCoNo()),
        () -> log.warn("금융회사 정보 확인 불가: {}", finCoNo)
    );

    // 금리 옵션들 확인
    List<SavingsInterestRates> productRates = savingsInterestRatesRepository
        .findByFinPrdtCd(product.getFinPrdtCd());

    if (productRates.isEmpty()) {
      log.warn("금리옵션 없음");
    } else {
      log.info("금리 옵션 {}개:", productRates.size());
      for (int i = 0; i < productRates.size(); i++) {
        SavingsInterestRates rate = productRates.get(i);
        String rsrvTypeName = "S".equals(rate.getRsrvType()) ? "정액적립식" : "자유적립식";
        String intrTypeName = "S".equals(rate.getIntrRateType()) ? "단리" : "복리";

        log.info("   {}. {}개월 {} {}: 기본 {}% → 최고 {}%",
            i + 1,
            rate.getSaveTrm(),
            rsrvTypeName,
            intrTypeName,
            rate.getIntrRate(),
            rate.getIntrRate2());

        if (rate.getMonthlyLimitMin() != null || rate.getMonthlyLimitMax() != null) {
          log.info("      월 적립한도: {}원 ~ {}원",
              rate.getMonthlyLimitMin(), rate.getMonthlyLimitMax());
        }
      }
    }
  }

  /**
   * Reflection을 이용한 private 메서드 호출
   */
  private void invokeProcessSavingApiResponse(SavingApiResponse response) {
    ReflectionTestUtils.invokeMethod(savingConnectApi, "processSavingApiResponse", response);
  }

  /**
   * Mock 데이터 생성
   */
  private SavingApiResponse createMockResponse() {
    // Mock 회사 데이터 1
    SavingApiResponse.SavingBaseInfo baseInfo1 = SavingApiResponse.SavingBaseInfo.builder()
        .disclosureMonth("202401")
        .finCoNo("0010001")
        .finPrdtCd("WR0001T")
        .korCoNm("우리은행")
        .finPrdtNm("테스트꿈적금")
        .joinWay("영업점,인터넷,스마트폰")
        .mtrtInt(
            "만기 후\n- 1개월이내 : 만기시점약정이율×50%\n- 1개월초과 6개월이내: 만기시점약정이율×30%\n- 6개월초과 : 만기시점약정이율×20%")
        .spclCnd("- 최고 연 0.6%p\n1. 스마트뱅킹가입 연 0.2%p\n2. 우대쿠폰 등록 연 0.1%p")
        .joinDeny("1")
        .joinMember("국내거주자인 개인")
        .etcNote("해당없음")
        .maxLimit(3000000L)
        .dclsStrtDay("20240101")
        .dclsEndDay(null)
        .finCoSubmDay("202401011200")
        .build();

    // Mock 회사 데이터 2
    SavingApiResponse.SavingBaseInfo baseInfo2 = SavingApiResponse.SavingBaseInfo.builder()
        .disclosureMonth("202401")
        .finCoNo("0010002")
        .finPrdtCd("SC0001T")
        .korCoNm("한국스탠다드차타드은행")
        .finPrdtNm("테스트희망적금")
        .joinWay("영업점")
        .mtrtInt("만기 후 1개월: 0.7%\n만기 후 1개월 초과 1년 이내: 0.3%")
        .spclCnd("만기해지 시 연 2%p 우대이율 적용")
        .joinDeny("2")
        .joinMember("기초생활수급자, 소년소녀가장")
        .etcNote(
            "1. 1인당 가입한도 : 월 30만원\n2. 월 적립액은 예금을 가입하는 때에 정하며, 계약기간 중에 변경할 수 없음\n3. 월 적립액 최소 5만원 이상 최대 30만원 이하")
        .maxLimit(300000L)
        .dclsStrtDay("20240101")
        .dclsEndDay(null)
        .finCoSubmDay("202401011300")
        .build();

    // Mock 금리 데이터 - 첫 번째 상품 (정액적립식)
    SavingApiResponse.SavingOptionInfo option1 = SavingApiResponse.SavingOptionInfo.builder()
        .disclosureMonth("202401")
        .finCoNo("0010001")
        .finPrdtCd("WR0001T")
        .intrRateType("S")
        .intrRateTypeNm("단리")
        .rsrvType("S")
        .rsrvTypeNm("정액적립식")
        .saveTrm(12)
        .intrRate(new BigDecimal("2.50"))
        .intrRate2(new BigDecimal("3.10"))
        .build();

    // Mock 금리 데이터 - 첫 번째 상품 (자유적립식)
    SavingApiResponse.SavingOptionInfo option2 = SavingApiResponse.SavingOptionInfo.builder()
        .disclosureMonth("202401")
        .finCoNo("0010001")
        .finPrdtCd("WR0001T")
        .intrRateType("S")
        .intrRateTypeNm("단리")
        .rsrvType("F")
        .rsrvTypeNm("자유적립식")
        .saveTrm(12)
        .intrRate(new BigDecimal("2.30"))
        .intrRate2(new BigDecimal("2.90"))
        .build();

    // Mock 금리 데이터 - 두 번째 상품 (정액적립식)
    SavingApiResponse.SavingOptionInfo option3 = SavingApiResponse.SavingOptionInfo.builder()
        .disclosureMonth("202401")
        .finCoNo("0010002")
        .finPrdtCd("SC0001T")
        .intrRateType("S")
        .intrRateTypeNm("단리")
        .rsrvType("S")
        .rsrvTypeNm("정액적립식")
        .saveTrm(24)
        .intrRate(new BigDecimal("3.00"))
        .intrRate2(new BigDecimal("5.00"))
        .build();

    // 결과 조립
    SavingApiResponse.SavingResult result = SavingApiResponse.SavingResult.builder()
        .productDivision("S")
        .totalCount("2")
        .maxPageNo("1")
        .nowPageNo("1")
        .errorCode("000")
        .errorMessage("정상")
        .baseList(Arrays.asList(baseInfo1, baseInfo2))
        .optionList(Arrays.asList(option1, option2, option3))
        .build();

    return SavingApiResponse.builder()
        .result(result)
        .build();
  }
}