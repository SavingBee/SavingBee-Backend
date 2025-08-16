package com.project.savingbee.connectApi.service;

import com.project.savingbee.common.entity.FinancialCompanies;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.entity.SavingsInterestRates;
import com.project.savingbee.common.repository.FinancialCompaniesRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.connectApi.dto.SavingApiResponse;
import com.project.savingbee.connectApi.util.ApiParsing;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * 금융 감독원 적금 API 연결
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SavingConnectApi {

  private final SavingsProductsRepository savingsProductsRepository;
  private final SavingsInterestRatesRepository savingsInterestRatesRepository;
  private final FinancialCompaniesRepository financialCompaniesRepository;

  // API Key
  @Value("${api.money.key}")
  private String moneyKey;

  // Date Format
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

  /**
   * 금융 감독원 적금 API 연결 메인 메서드
   */
  @Transactional
  public void connectSavingProducts() {
    try {
      log.info("적금 상품 api 연결 시작");

      // WebClient 기본 설정 및 생성
      WebClient webClient = WebClient.builder()
          .baseUrl("https://finlife.fss.or.kr/finlifeapi")
          .clientConnector(new ReactorClientHttpConnector(
              HttpClient.create()
                  .followRedirect(true)
                  .responseTimeout(Duration.ofSeconds(30))
          ))
          .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
          .build();

      // 첫 번째 페이지를 호출하여 전체 페이지 수 확인
      SavingApiResponse firstResponse = callSavingApi(webClient, 1);
      if (firstResponse == null) {
        log.error("첫 번째 페이지 호출 실패");
        return;
      }

      // 전체 페이지 수 확인
      int maxPageNo = Integer.parseInt(firstResponse.getResult().getMaxPageNo());
      log.info("총 {}페이지 데이터 처리 시작", maxPageNo);

      for (int pageNo = 1; pageNo <= maxPageNo; pageNo++) {
        SavingApiResponse response;
        if (pageNo == 1) {
          response = firstResponse; // 첫 페이지는 이미 호출했으므로 재사용
        } else {
          response = callSavingApi(webClient, pageNo);
        }

        if (response != null) {
          processSavingApiResponse(response);
          log.info("{}페이지 처리 완료", pageNo);
        }
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
  private SavingApiResponse callSavingApi(WebClient webClient, int pageNo) {
    try {
      log.debug("{}페이지 API 호출 시작", pageNo);

      // API 호출
      SavingApiResponse response = webClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/savingProductsSearch.json")  // 전체 경로
              .queryParam("auth", moneyKey) // API키
              .queryParam("topFinGrpNo", "020000")
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

      // 응답 확인 하기 - null이거나 빈 값
      if (response == null) {
        log.error("API 응답이 null입니다.");
        return null;
      }
      if (response.getResult() == null) {
        log.error("API 응답에 result가 없습니다.");
        return null;
      }

      return response;

    } catch (Exception e) {
      log.error("API 호출 중 예외 발생: ", e);
      return null;
    }
  }

  /**
   * 저장 메서드
   */

  /* API 데이터 DB에 저장하기*/
  private void processSavingApiResponse(SavingApiResponse response) {
    // null 체크 추가
    if (response == null) {
      log.warn("응답 데이터가 null입니다. 처리를 건너뜁니다.");
      return;
    }

    // result null 체크 추가
    if (response.getResult() == null) {
      log.warn("응답의 result가 null입니다. 처리를 건너뜁니다.");
      return;
    }

    // 에러 코드 체크 추가
    if (!"000".equals(response.getResult().getErrorCode())) {
      log.warn("API 응답 에러: 코드={}, 메시지={}",
          response.getResult().getErrorCode(),
          response.getResult().getErrorMessage());
      return;
    }
    // 금융회사 정보 FinancialCompanies 먼저 저장
    if (response.getResult().getBaseList() != null) {
      saveFinancialCompanies(response.getResult().getBaseList());
      // 상품정보 SavingsProducts에 저장
      saveSavingsProducts(response.getResult().getBaseList());
    }
    // 상품의 금리옵션은 SavingsInterestRates에 저장
    if (response.getResult().getOptionList() != null
        && response.getResult().getBaseList() != null) {
      saveSavingsInterestRates(response.getResult().getOptionList(),
          response.getResult().getBaseList());
    }
    log.info("적금 상품 정보 저장 완료");
  }

  /* 금융 회사 정보 저장 */
  private void saveFinancialCompanies(List<SavingApiResponse.SavingBaseInfo> baseList) {
    Set<String> processedFinCoNos = new HashSet<>();

    for (SavingApiResponse.SavingBaseInfo item : baseList) {
      // 금융 화사 고유 번호가 있는 경우에만 저장
      if (item.getFinCoNo() != null && !processedFinCoNos.contains(item.getFinCoNo())) {
        // 이미 존재하는지 확인 존재하지 않으면 저장
        if (!financialCompaniesRepository.existsById(item.getFinCoNo())) {
          FinancialCompanies company = FinancialCompanies.builder()
              .finCoNo(item.getFinCoNo())
              .korCoNm(item.getKorCoNm())
              .build();

          financialCompaniesRepository.save(company);
        }
        processedFinCoNos.add(item.getFinCoNo());
      }
    }
  }

  /* 적금 상품 저장 */
  private void saveSavingsProducts(List<SavingApiResponse.SavingBaseInfo> baseList) {
    for (SavingApiResponse.SavingBaseInfo item : baseList) {
      try {
        // 이미 존재하는 상품인지 확인
        if (savingsProductsRepository.existsById(item.getFinPrdtCd())) {
          continue;
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
            .maxLimit(ApiParsing.parseBigDecimalFromLong(item.getMaxLimit()))
            .dclsStrtDay(ApiParsing.parseDate(item.getDclsStrtDay()))
            .dclsEndDay(ApiParsing.parseDate(item.getDclsEndDay()))
            .isActive(true)
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
  private void saveSavingsInterestRates(List<SavingApiResponse.SavingOptionInfo> optionList,
      List<SavingApiResponse.SavingBaseInfo> baseList) {

    // baseList를 Map으로 변환 (finPrdtCd -> SavingBaseInfo)
    Map<String, SavingApiResponse.SavingBaseInfo> baseInfoMap = baseList.stream()
        .collect(Collectors.toMap(
            SavingApiResponse.SavingBaseInfo::getFinPrdtCd,
            base -> base,
            (existing, replacement) -> existing // 중복 키가 있을 경우 기존 값 유지
        ));

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
                option.getSaveTrm()
            );

        // 존재하면 건너뛰기
        if (exists) {
          continue;
        }

        // baseInfo에서 etcNote 가져오기
        SavingApiResponse.SavingBaseInfo baseInfo = baseInfoMap.get(option.getFinPrdtCd());
        String etcNote = baseInfo != null ? baseInfo.getEtcNote() : null;

        // 존재하지 않으면 저장
        SavingsInterestRates rate = SavingsInterestRates.builder()
            .intrRateType(option.getIntrRateType())
            .rsrvType(option.getRsrvType()) // 적금 적립 유형
            .saveTrm(option.getSaveTrm())
            .intrRate(option.getIntrRate())
            .intrRate2(option.getIntrRate2())
            .finPrdtCd(option.getFinPrdtCd())
            .monthlyLimitMin(ApiParsing.parseMonthlyLimitMin(etcNote)) // 월 최소 금액
            .monthlyLimitMax(ApiParsing.parseMonthlyLimitMax(etcNote)) // 월 최대 금액
            .build();

        savingsInterestRatesRepository.save(rate);

      } catch (Exception e) {
        log.error("적금 금리 저장 실패 - 상품코드: {}, 오류: {}",
            option.getFinPrdtCd(), e.getMessage());
      }
    }
  }
}