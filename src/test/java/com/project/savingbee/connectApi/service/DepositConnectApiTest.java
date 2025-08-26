package com.project.savingbee.connectApi.service;

import com.project.savingbee.common.entity.*;
import com.project.savingbee.common.repository.*;
import com.project.savingbee.connectApi.dto.DepositApiResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;
import java.util.*;

import static org.assertj.core.api.Assertions.*;


/**
 * DepositConnectApi 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Slf4j
@DisplayName("예금 API 연결 테스트")
class DepositConnectApiTest {

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;

  @Autowired
  private DepositConnectApi depositConnectApi;

  @Autowired
  private DepositProductsRepository depositProductsRepository;

  @Autowired
  private DepositInterestRatesRepository depositInterestRatesRepository;

  @Autowired
  private FinancialCompaniesRepository financialCompaniesRepository;

  @Value("${api.money.key}")
  private String apiKey;

  // API 키 설정 (테스트용)
  @BeforeEach
  void setUp() {
    // 실제 API 키가 설정되어 있는지 확인
    if (apiKey == null || apiKey.equals("test-api-key")) {
      log.warn("실제 API 키가 설정 안됨. 테스트 종료.");
    }
    cleanDatabase();  // 테스트 전 H2 정리
  }

  // 테스트 후 DB 정리 (다음 테스트를 위해)
  @AfterEach
  void tearDown() {
    cleanDatabase();
    log.debug(" 테스트 후 DB 초기화");
  }

//  /**
//   * 실제 API 1페이지 호출 및 확인
//   *
//   * @throws Exception
//   */
//  @Test
//  @Rollback
//  void callRealApiForTest() throws Exception {
//    log.info(" 금융감독원 API 호출 테스트 시작");
//
//    // API 키 체크 추가
//    String currentApiKey = ReflectionTestUtils.getField(depositConnectApi, "moneyKey").toString();
//    if (currentApiKey == null || currentApiKey.equals("test-api-key") || currentApiKey.trim()
//        .isEmpty()) {
//      log.warn("유효하지 않은 API 키: {}. 테스트를 건너뜁니다.", currentApiKey);
//      return; // 테스트 종료
//    }
//
//    long totalStartTime = System.currentTimeMillis();
//
//    // ConnectDepositProducts() 메서드 호출 후 한 페이지만 처리
////    depositConnectApi.testConnect();
//
//    long totalDuration = System.currentTimeMillis() - totalStartTime;
//    log.info("전체 처리 시간: {}ms", totalDuration);
//
//    // 결과 확인
//    long companiesCount = financialCompaniesRepository.count();
//    long productsCount = depositProductsRepository.count();
//    long ratesCount = depositInterestRatesRepository.count();
//
//    log.info("===처리 결과===");
//    log.info("  - 금융회사: {}개", companiesCount);
//    log.info("  - 예금상품: {}개", productsCount);
//    log.info("  - 금리옵션: {}개", ratesCount);
//
//    assertThat(companiesCount).isGreaterThan(0);
//    assertThat(productsCount).isGreaterThan(0);
//    assertThat(ratesCount).isGreaterThan(0);
//
//    // 데이터 무결성 확인
//    log.info("데이터 무결성 확인");
//    verifyDataIntegrity();
//    log.info("데이터 무결성 확인 완료");
//
//    // DB에 저장된 내용 확인해보기
//    logSavedDataSample();
//  }

  /**
   * 중복 처리 테스트
   */
  @Rollback
  void duplicateDataHandlingTest() throws Exception {
    log.info("중복 데이터 처리 테스트 시작 (orgTypeCode 검증 포함)");

    // mock 데이터 생성 - topFinGrpNo를 고려한 mock 생성
    DepositApiResponse mockResponse = createMockForOrgTypeCode();

    // 같은 데이터 중복 저장
    log.info("첫 번째 저장");
    invokeProcessDepositApiResponseWithTopFinGrpNo(mockResponse, "020000"); // 은행으로 처리

    long firstCompaniesCount = financialCompaniesRepository.count();
    long firstProductsCount = depositProductsRepository.count();
    long firstRatesCount = depositInterestRatesRepository.count();

    // orgTypeCode 검증 추가
    FinancialCompanies savedCompany = financialCompaniesRepository.findById("0010001").orElse(null);
    assertThat(savedCompany).isNotNull();
    assertThat(savedCompany.getOrgTypeCode()).isEqualTo("020000"); // 은행 코드 확인
    log.info("첫 번째 저장 후 orgTypeCode: {}", savedCompany.getOrgTypeCode());

    log.info("두 번째 저장");
    invokeProcessDepositApiResponseWithTopFinGrpNo(mockResponse, "020000");

    long secondCompaniesCount = financialCompaniesRepository.count();
    long secondProductsCount = depositProductsRepository.count();
    long secondRatesCount = depositInterestRatesRepository.count();

    // 중복 저장 확인
    assertThat(firstCompaniesCount).isEqualTo(secondCompaniesCount);
    assertThat(firstProductsCount).isEqualTo(secondProductsCount);
    assertThat(firstRatesCount).isEqualTo(secondRatesCount);

    // 저장된 데이터 개수 확인
    assertThat(financialCompaniesRepository.count()).isEqualTo(1);
    assertThat(depositProductsRepository.count()).isEqualTo(1);
    log.info("저장된 금융회사 수 {}", financialCompaniesRepository.count());
    log.info("저장된 금융상품 수 {}", depositProductsRepository.count());

    // orgTypeCode가 여전히 올바른지 재확인
    FinancialCompanies finalCompany = financialCompaniesRepository.findById("0010001").orElse(null);
    assertThat(finalCompany.getOrgTypeCode()).isEqualTo("020000");

    log.info("중복 저장 불가 확인 및 orgTypeCode 매핑 검증 완료");
  }

  /**
   * orgTypeCode 매핑 테스트 - 최소한의 새 테스트 추가
   */
  @Test
  @Rollback
  void orgTypeCodeMappingTest() throws Exception {
    log.info("orgTypeCode 매핑 테스트 시작");

    // 은행 데이터
    DepositApiResponse bankResponse = createMockForOrgTypeCode("0010001", "테스트은행");
    invokeProcessDepositApiResponseWithTopFinGrpNo(bankResponse, "020000");

    // 저축은행 데이터
    DepositApiResponse savingsBankResponse = createMockForOrgTypeCode("0030001", "테스트저축은행");
    invokeProcessDepositApiResponseWithTopFinGrpNo(savingsBankResponse, "030300");

    // 여신전문 데이터 (신협으로 분류됨)
    DepositApiResponse creditResponse = createMockForOrgTypeCode("0032001", "테스트여신전문");
    invokeProcessDepositApiResponseWithTopFinGrpNo(creditResponse, "030200");

    // 검증
    verifyOrgTypeCode("0010001", "020000"); // 은행 → 020000
    verifyOrgTypeCode("0030001", "030300"); // 저축은행 → 030300
    verifyOrgTypeCode("0032001", "050000"); // 여신전문 → 050000 (신협으로 분류)

    assertThat(financialCompaniesRepository.count()).isEqualTo(3);
    log.info("orgTypeCode 매핑 테스트 완료");
  }

  // ===================== 기존 헬퍼 메서드 수정 =====================

  /**
   * orgTypeCode 테스트를 위한 Mock 데이터 (기존 createMock 수정)
   */
  private DepositApiResponse createMockForOrgTypeCode() {
    return createMockForOrgTypeCode("0010001", "테스트은행");
  }

  /**
   * orgTypeCode 테스트를 위한 Mock 데이터 - 파라미터 추가
   */
  private DepositApiResponse createMockForOrgTypeCode(String finCoNo, String korCoNm) {
    DepositApiResponse response = new DepositApiResponse();
    DepositApiResponse.Result result = new DepositApiResponse.Result();

    result.setErrCd("000");
    result.setErrMsg("정상");

    // Mock 회사 데이터
    DepositApiResponse.BaseListItem item = new DepositApiResponse.BaseListItem();
    item.setFinCoNo(finCoNo);
    item.setKorCoNm(korCoNm);
    item.setFinPrdtCd("TEST" + finCoNo);
    item.setFinPrdtNm(korCoNm + "예금상품");
    item.setJoinWay("영업점,인터넷");
    item.setMtrtInt("만기 후 조건");
    item.setSpclCnd("우대조건");
    item.setJoinDeny("1");
    item.setJoinMember("개인");
    item.setEtcNote("테스트용");
    item.setMaxLimit("100000000");
    item.setDclsStrtDay("20240101");
    item.setDclsEndDay("20241231");

    // Mock 금리 데이터
    DepositApiResponse.OptionListItem option = new DepositApiResponse.OptionListItem();
    option.setFinPrdtCd("TEST" + finCoNo);
    option.setIntrRateType("S");
    option.setSaveTrm("12");
    option.setIntrRate("2.50");
    option.setIntrRate2("3.00");

    result.setBaseList(Arrays.asList(item));
    result.setOptionList(Arrays.asList(option));
    response.setResult(result);

    return response;
  }

  /**
   * topFinGrpNo와 함께 processDepositApiResponse 호출
   */
  private void invokeProcessDepositApiResponseWithTopFinGrpNo(DepositApiResponse response,
      String topFinGrpNo) {
    ReflectionTestUtils.invokeMethod(depositConnectApi, "processDepositApiResponse", response,
        topFinGrpNo);
  }

  /**
   * orgTypeCode 검증 헬퍼 메서드
   */
  private void verifyOrgTypeCode(String finCoNo, String expectedOrgTypeCode) {
    FinancialCompanies company = financialCompaniesRepository.findById(finCoNo).orElse(null);
    assertThat(company).isNotNull();
    assertThat(company.getOrgTypeCode()).isEqualTo(expectedOrgTypeCode);
    log.info("금융회사 {} orgTypeCode 검증 완료: {}", finCoNo, expectedOrgTypeCode);
  }


  /**
   * 추가적으로 필요한 메서드
   */

  // DB 정리
  private void cleanDatabase() {
    depositInterestRatesRepository.deleteAll();
    depositProductsRepository.deleteAll();
    financialCompaniesRepository.deleteAll();
  }

  // 데이터 검증
  private void verifyDataIntegrity() {
    List<DepositProducts> products = depositProductsRepository.findAll();
    List<DepositInterestRates> rates = depositInterestRatesRepository.findAll();

    // 상품의 금융회사 확인
    for (DepositProducts product : products) {
      assertThat(financialCompaniesRepository.existsById(product.getFinCoNo()))
          .withFailMessage("상품 %s의 금융회사 %s가 존재하지 않습니다",
              product.getFinPrdtCd(), product.getFinCoNo())
          .isTrue();
    }

    // 금리의 상품 확인
    for (DepositInterestRates rate : rates) {
      assertThat(depositProductsRepository.existsById(rate.getFinPrdtCd()))
          .withFailMessage("금리의 상품 %s이 존재하지 않습니다", rate.getFinPrdtCd())
          .isTrue();
    }
  }

  // DB 데이터 확인 - 상품 1개를 기준으로 연관 데이터 확인
  private void logSavedDataSample() {
    log.info("=== 저장된 데이터 확인 ===");

    // 상품 1개 조회
    List<DepositProducts> products = depositProductsRepository.findAll();
    if (products.isEmpty()) {
      log.warn("저장된 상품이 없습니다.");
      return;
    }

    DepositProducts product = products.get(0);
    log.info("상품: {} ({})", product.getFinPrdtNm(), product.getFinPrdtCd());
    log.info("   - 가입방법: {}", product.getJoinWay());
    log.info("   - 가입제한: {}", product.getJoinDeny());
    log.info("   - 최고한도: {}", product.getMaxLimit());
    log.info("   - 공시기간: {} ~ {}", product.getDclsStrtDay(), product.getDclsEndDay());
    log.info("   - 특별조건: {}", product.getSpclCnd());

    // 해당 상품의 금융회사 정보 확인
    String finCoNo = product.getFinCoNo();
    financialCompaniesRepository.findById(finCoNo).ifPresentOrElse(
        company -> {
          log.info("💼 금융회사: {} ({})", company.getKorCoNm(), company.getFinCoNo());
        },
        () -> log.warn("금융회사 정보 확인 불가 : 상품 {}", finCoNo)
    );

    // 해당 상품의 금리 옵션들 확인
    List<DepositInterestRates> productRates = depositInterestRatesRepository
        .findByFinPrdtCd(product.getFinPrdtCd());

    if (productRates.isEmpty()) {
      log.warn("금리옵션 없음");
    } else {
      log.info("금리 옵션 {}개:", productRates.size());
      for (int i = 0; i < Math.min(3, productRates.size()); i++) {  // 최대 3개만 출력
        DepositInterestRates rate = productRates.get(i);
        log.info("   {}. {}개월 {}금리: 기본 {}% → 최고 {}%",
            i + 1,
            rate.getSaveTrm(),
            "S".equals(rate.getIntrRateType()) ? "단리" : "복리",
            rate.getIntrRate(),
            rate.getIntrRate2());
      }
      if (productRates.size() > 3) {
        log.info("   ... 외 {}개 더", productRates.size() - 3);
      }
    }
  }


  /**
   * 리플렉션을 이용한 private 메서드 호출
   */
  private LocalDate invokeParseDate(String dateStr) throws Exception {
    return (LocalDate) ReflectionTestUtils.invokeMethod(depositConnectApi, "parseDate", dateStr);
  }

  private Integer invokeParseInteger(String str) throws Exception {
    return (Integer) ReflectionTestUtils.invokeMethod(depositConnectApi, "parseInteger", str);
  }

  private BigDecimal invokeParseBigDecimal(String str) throws Exception {
    return (BigDecimal) ReflectionTestUtils.invokeMethod(depositConnectApi, "parseBigDecimal", str);
  }

  private void invokeProcessDepositApiResponse(DepositApiResponse response) throws Exception {
    ReflectionTestUtils.invokeMethod(depositConnectApi, "processDepositApiResponse", response);
  }


  /**
   * Mock 데이터
   */
  private DepositApiResponse createMock() {
    DepositApiResponse response = new DepositApiResponse();
    DepositApiResponse.Result result = new DepositApiResponse.Result();

    result.setErrCd("000");
    result.setErrMsg("정상");

    // Mock 회사 데이터
    DepositApiResponse.BaseListItem item = new DepositApiResponse.BaseListItem();
    item.setFinCoNo("0010001");
    item.setKorCoNm("테스트은행");
    item.setFinPrdtCd("TEST001");
    item.setFinPrdtNm("테스트예금상품");
    item.setJoinWay("영업점,인터넷");
    item.setMtrtInt("만기 후 조건");
    item.setSpclCnd("우대조건");
    item.setJoinDeny("1");
    item.setJoinMember("개인");
    item.setEtcNote("테스트용");
    item.setMaxLimit("100000000");
    item.setDclsStrtDay("20240101");
    item.setDclsEndDay("20241231");

    // Mock 금리 데이터
    DepositApiResponse.OptionListItem option = new DepositApiResponse.OptionListItem();
    option.setFinPrdtCd("TEST001");
    option.setIntrRateType("S");
    option.setSaveTrm("12");
    option.setIntrRate("2.50");
    option.setIntrRate2("3.00");

    result.setBaseList(Arrays.asList(item));
    result.setOptionList(Arrays.asList(option));
    response.setResult(result);

    return response;
  }
}