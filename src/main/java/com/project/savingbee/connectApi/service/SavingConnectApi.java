package com.project.savingbee.connectApi.service;

import com.project.savingbee.connectApi.dto.SavingApiResponse;
import com.project.savingbee.connectApi.util.ApiParsing;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.entity.SavingsInterestRates;
import com.project.savingbee.common.entity.FinancialCompanies;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.common.repository.FinancialCompaniesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 금융감독원 적금 API 연결
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SavingConnectApi {

  private final SavingsProductsRepository savingsProductsRepository;
  private final SavingsInterestRatesRepository savingsInterestRatesRepository;
  private final FinancialCompaniesRepository financialCompaniesRepository;

  // 금융권별 처리 순서: 은행 -> 저축은행 -> 신협
  private static final List<String> TOP_FIN_GRP_NOS = List.of(
      "020000",  // 은행
      "030300",  // 저축은행
      "030200",  // 여신전문 (신협으로 분류)
      "050000",  // 보험 (신협으로 분류)
      "060000"   // 금융투자 (신협으로 분류)
  );

  // API key
  @Value("${api.money.key}")
  private String moneyKey;

  // Date Format
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

  /**
   * 금융감독원 적금 API 연결 메인 메서드
   */
  public void connectSavingApi() {
    try {
      log.info("적금 상품 API 연결 시작");

      // WebClient 기본 설정 및 생성
      WebClient webClient = WebClient.builder()
          .baseUrl("https://finlife.fss.or.kr/finlifeapi")
          .clientConnector(new ReactorClientHttpConnector(
              HttpClient.create()
                  .responseTimeout(Duration.ofSeconds(30))
          ))
          .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
          .build();

      // 각 금융권별로 순차 처리
      for (String topFinGrpNo : TOP_FIN_GRP_NOS) {
        log.info("{}번 금융권 처리 시작", topFinGrpNo);

        // 첫 번째 페이지를 호출하여 전체 페이지 수 확인
        SavingApiResponse firstResponse = callSavingApi(webClient, topFinGrpNo, 1);
        if (firstResponse == null) {
          log.error("{}번 금융권 첫 번째 페이지 호출 실패", topFinGrpNo);
          continue;
        }

        // 전체 페이지 수 확인
        int maxPageNo = Integer.parseInt(firstResponse.getResult().getMaxPageNo());
        log.info("{}번 금융권 총 {}페이지 데이터 처리 시작", topFinGrpNo, maxPageNo);

        for (int pageNo = 1; pageNo <= maxPageNo; pageNo++) {
          SavingApiResponse response;
          if (pageNo == 1) {
            response = firstResponse; // 첫 페이지는 이미 호출했으므로 재사용
          } else {
            response = callSavingApi(webClient, topFinGrpNo, pageNo);
          }

          if (response != null) {
            processSavingApiResponse(response, topFinGrpNo);
            log.info("{}번 금융권 {}페이지 처리 완료", topFinGrpNo, pageNo);
          }
        }

        log.info("{}번 금융권 처리 완료", topFinGrpNo);
      }

      log.info("적금 상품 API 연결 완료");

    } catch (Exception e) {
      log.error("적금 API 연결 중 오류 발생", e);
      throw new RuntimeException("적금 API 연결 실패", e);
    }
  }

  /**
   * 실제 API 호출 메서드
   */
  private SavingApiResponse callSavingApi(WebClient webClient, String topFinGrpNo, int pageNo) {
    try {
      log.debug("{}번 금융권 {}페이지 API 호출 시작", topFinGrpNo, pageNo);

      // API 호출
      SavingApiResponse response = webClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/savingProductsSearch.json")  // 전체 경로
              .queryParam("auth", moneyKey) // API키
              .queryParam("topFinGrpNo", topFinGrpNo) // 금융권별로 동적 처리
              .queryParam("pageNo", pageNo) // 페이지 번호
              .build())
          .retrieve()
          .onStatus(HttpStatusCode::isError, clientResponse -> {
            log.error("API 호출 실패: {} - {}", clientResponse.statusCode(), clientResponse.headers());
            return clientResponse.bodyToMono(String.class)
                .doOnNext(errorBody -> log.error("에러 응답 내용: {}", errorBody))
                .then(Mono.error(
                    new RuntimeException("적금 상품 API 호출 실패: " + clientResponse.statusCode())));
          })
          .bodyToMono(SavingApiResponse.class)  // SavingApiResponse로 수정
          .doOnSuccess(res -> log.debug("API 응답 수신 성공"))
          .doOnError(error -> log.error("API 호출 중 오류: ", error))
          .block();

      // 응답 확인하기 - null이거나 빈 값
      if (response == null) {
        log.error("API 응답이 null입니다.");
        return null;
      }
      if (response.getResult() == null) {
        log.error("API 응답에 result가 없습니다.");
        return null;
      }

      log.debug("{}번 금융권 {}페이지 API 응답 수신 완료", topFinGrpNo, pageNo);
      return response;

    } catch (Exception e) {
      log.error("{}번 금융권 {}페이지 API 호출 중 오류", topFinGrpNo, pageNo, e);
      return null;
    }
  }

  /**
   * topFinGrpNo를 orgTypeCode로 매핑
   */
  private String mapTopFinGrpNoToOrgTypeCode(String topFinGrpNo) {
    switch (topFinGrpNo) {
      case "020000": // 은행
        return "020000";
      case "030300": // 저축은행
        return "030300";
      case "030200": // 여신전문 → 신협으로 분류
      case "050000": // 보험 → 신협으로 분류
      case "060000": // 금융투자 → 신협으로 분류
        return "050000";
      default:
        log.warn("알 수 없는 topFinGrpNo: {}, 기본값으로 050000 사용", topFinGrpNo);
        return "050000";
    }
  }

  /**
   * 저장 메서드
   */

  /* API 데이터 DB에 저장하기*/
  private void processSavingApiResponse(SavingApiResponse response, String topFinGrpNo) {
    // null 체크 추가
    if (response == null || response.getResult() == null) {
      log.warn("API 응답이 null이거나 result가 없습니다.");
      return;
    }
    // 금융회사 정보 FinancialCompanies 먼저 저장
    if (response.getResult().getBaseList() != null) {
      saveSavingFinancialCompanies(response.getResult().getBaseList(), topFinGrpNo);
      // 상품정보 SavingsProducts에 저장
      saveSavingProducts(response.getResult().getBaseList());
    }
    // 상품의 금리옵션은 SavingsInterestRates에 저장
    if (response.getResult().getOptionList() != null) {
      saveSavingInterestRates(response.getResult().getOptionList());
    }
    log.info("적금 상품 정보 저장 완료");
  }

  /* 적금 금융회사 정보 저장 */
  private void saveSavingFinancialCompanies(List<SavingApiResponse.SavingBaseInfo> baseList,
      String topFinGrpNo) {
    String orgTypeCode = mapTopFinGrpNoToOrgTypeCode(topFinGrpNo);
    Set<String> processedFinCoNos = new HashSet<>();

    for (SavingApiResponse.SavingBaseInfo item : baseList) {
      // 금융회사 고유번호가 있는 경우에만 저장
      if (item.getFinCoNo() != null && !processedFinCoNos.contains(item.getFinCoNo())) {
        // 이미 존재하는지 확인 존재하지 않으면 저장
        if (!financialCompaniesRepository.existsById(item.getFinCoNo())) {
          FinancialCompanies company = FinancialCompanies.builder()
              .finCoNo(item.getFinCoNo())
              .korCoNm(item.getKorCoNm())
              .orgTypeCode(orgTypeCode) // 매핑된 orgTypeCode로 저장
              .build();

          financialCompaniesRepository.save(company);
        }
        processedFinCoNos.add(item.getFinCoNo());
      }
    }
  }

  /* 적금 상품 저장 */
  private void saveSavingProducts(List<SavingApiResponse.SavingBaseInfo> baseList) {
    for (SavingApiResponse.SavingBaseInfo item : baseList) {
      try {
        // 이미 존재하는 상품인지 확인
        if (savingsProductsRepository.existsById(item.getFinPrdtCd())) {
          continue;
        }

        // 공시종료일 확인하여 isActive 설정
        LocalDate dclsEndDay = ApiParsing.parseDate(item.getDclsEndDay());
        boolean isActive = true;
        if (dclsEndDay != null && dclsEndDay.isBefore(LocalDate.now())) {
          isActive = false;
          log.debug("상품 {} - 공시종료로 비활성화 (종료일: {})", item.getFinPrdtCd(), dclsEndDay);
        }

        // 존재하지 않을 경우 저장
        SavingsProducts product = SavingsProducts.builder()
            .finPrdtCd(item.getFinPrdtCd())
            .finPrdtNm(item.getFinPrdtNm())
            .joinWay(item.getJoinWay())
            .mtrtInt(item.getMtrtInt())
            .spclCnd(item.getSpclCnd())
            .joinDeny(item.getJoinDeny())
            .joinMember(item.getJoinMember())
            .etcNote(item.getEtcNote())
            .maxLimit(ApiParsing.parseBigDecimal(item.getMaxLimit()))
            .dclsStrtDay(ApiParsing.parseDate(item.getDclsStrtDay()))
            .dclsEndDay(dclsEndDay)
            .isActive(isActive) // 공시종료일 기준으로 설정
            .finCoNo(item.getFinCoNo())
            .build();

        savingsProductsRepository.save(product);

      } catch (Exception e) {
        log.error("적금 상품 저장 실패 - 상품코드: {}, 오류: {}",
            item.getFinPrdtCd(), e.getMessage());
      }
    }
  }

  /* 적금 상품 금리 옵션 저장 */
  private void saveSavingInterestRates(List<SavingApiResponse.SavingOptionInfo> optionList) {
    for (SavingApiResponse.SavingOptionInfo option : optionList) {
      try {
        // 상품이 존재하는지 확인
        if (!savingsProductsRepository.existsById(option.getFinPrdtCd())) {
          continue;
        }

        // 해당 상품의 동일한 금리 옵션이 이미 저장되어있는 지 확인
        boolean exists = savingsInterestRatesRepository
            .existsByFinPrdtCdAndIntrRateTypeAndRsrvTypeAndSaveTrm(
                option.getFinPrdtCd(),
                option.getIntrRateType(),
                option.getRsrvType(),
                option.getSaveTrm());

        if (exists) {
          continue; // 이미 존재하면 건너뛰기
        }

        // 새로운 금리 옵션 저장
        SavingsInterestRates rate = SavingsInterestRates.builder()
            .finPrdtCd(option.getFinPrdtCd())
            .intrRateType(option.getIntrRateType())
            .rsrvType(option.getRsrvType())
            .saveTrm(option.getSaveTrm())
            .intrRate(ApiParsing.parseBigDecimal(option.getIntrRate()))
            .intrRate2(ApiParsing.parseBigDecimal(option.getIntrRate2()))
            .build();

        savingsInterestRatesRepository.save(rate);

      } catch (Exception e) {
        log.error("적금 금리 저장 실패 - 상품코드: {}, 저축기간: {}, 오류: {}",
            option.getFinPrdtCd(), option.getSaveTrm(), e.getMessage());
      }
    }
  }
}