package com.project.savingbee.filtering.service;

import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.repository.DepositProductsRepository;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.filtering.dto.ProductDetailResponse;
import com.project.savingbee.filtering.dto.ProductDetailResponse.InterestRateOption;
import com.project.savingbee.filtering.util.FilterMappingUtil;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 상품 상세 정보 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DetailService {

  private final DepositProductsRepository depositProductsRepository;
  private final SavingsProductsRepository savingsProductsRepository;
  private final SearchService searchService;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  /**
   * 상품 상세 정보 조회
   */
  public ProductDetailResponse getProductDetail(String productId) {
    log.info("상품 상세 정보 조회 시작 - 상품코드:{}", productId);

    // 입력 유효성 검사
    if (productId == null || productId.trim().isEmpty()) {
      throw new IllegalArgumentException("상품 코드가 유효하지 않습니다.");
    }

    // 예금 상품에서 먼저 찾기
    Optional<DepositProducts> depositOpt = depositProductsRepository.findById(productId);
    if (depositOpt.isPresent()) {
      DepositProducts deposit = depositOpt.get();

      // 조회수 증가
//      addToViewedProductsCache(productId);

      if (Boolean.TRUE.equals(deposit.getIsActive())) {
        addToViewedProductsCache(productId);
      }

      log.info("예금 상품 조회 완료 - 상품코드:{}", deposit.getFinPrdtNm());
      return convertDepositToResponse(deposit);
    }

    // 적금 상품에서 찾기
    Optional<SavingsProducts> savingsOpt = savingsProductsRepository.findById(productId);
    if (savingsOpt.isPresent()) {
      SavingsProducts saving = savingsOpt.get();

      // 조회수 증가
//      addToViewedProductsCache(productId);

      if (Boolean.TRUE.equals(saving.getIsActive())) {
        addToViewedProductsCache(productId);
      }

      log.info("적금 상품 조회 완료 -상품코드:{}", saving.getFinPrdtNm());
      return convertSavingsToResponse(saving);
    }

    // 상품 바놘 실패
    throw new IllegalArgumentException("존재하지 않는 상품입니다: " + productId);

  }

  /**
   * 조회수 증가 - SearchService의 viewedProductsCache에 추가
   */
  private void addToViewedProductsCache(String productCode) {
    try {
      searchService.addToViewedProductsCache(productCode);
      log.debug("상품 조회수 증가 완료 - productCode: {}", productCode);
    } catch (Exception e) {
      log.warn("상품 조회수 증가 실패 - productCode: {}, error: {}", productCode, e.getMessage());
    }
  }

  /**
   * 예금 상품을 상세 응답 DTO로 변환
   */
  private ProductDetailResponse convertDepositToResponse(DepositProducts deposit) {
    // 금융회사명 null 체크
    String companyName = "정보없음";
    if (deposit.getFinancialCompany() != null && deposit.getFinancialCompany().getKorCoNm() != null) {
      companyName = deposit.getFinancialCompany().getKorCoNm();
    }

    // 금리 옵션 정보 변환
    List<ProductDetailResponse.InterestRateOption> interestRateOptions =
        (deposit.getInterestRates() != null) ?
            deposit.getInterestRates().stream()
            .map(rate -> ProductDetailResponse.InterestRateOption.builder()
                .saveTrm(rate.getSaveTrm())
                .intrRateTypeNm(
                    FilterMappingUtil.convertInterestRateCodeToDisplayName(rate.getIntrRateType()))
                .intrRate(rate.getIntrRate())
                .intrRate2(rate.getIntrRate2())
                .build())
            .collect(Collectors.toList())
        : List.of();

    // 기본 상품 정보 변환
    return ProductDetailResponse.builder()
        .finPrdtCd(deposit.getFinPrdtCd())
        .finPrdtNm(deposit.getFinPrdtNm())
        .productType("deposit")
        .finCoNo(deposit.getFinCoNo())
        .korCoNm(companyName)
        .joinWay(deposit.getJoinWay())
        .joinDenyNm(FilterMappingUtil.convertJoinDenyCodeToDisplayName(deposit.getJoinDeny()))
        .joinMember(deposit.getJoinMember())
        .maxLimit(deposit.getMaxLimit())
        .spclCnd(deposit.getSpclCnd())
        .mtrtInt(deposit.getMtrtInt())
        .etcNote(deposit.getEtcNote())
        .dclsStrtDay(formatDate(deposit.getDclsStrtDay()))
        .dclsEndDay(formatDate(deposit.getDclsEndDay()))
        .interestRates(interestRateOptions)
        .build();
  }

  /**
   * 적금 상품을 상세 응답 DTO로 변환
   */
  private ProductDetailResponse convertSavingsToResponse(SavingsProducts savings) {
    // 금융회사명 null 체크
    String companyName = "정보없음";
    if (savings.getFinancialCompany() != null && savings.getFinancialCompany().getKorCoNm() != null) {
      companyName = savings.getFinancialCompany().getKorCoNm();
    }
    // 금리 옵션 정보 변환
    List<InterestRateOption> interestRateOptions =
        (savings.getInterestRates() != null) ?
        savings.getInterestRates().stream()
            .map(rate -> {
              // 총 저축금 계산 (저축기간 * 월 저축한도)
              BigDecimal totalMaxLimit = null;
              if (rate.getSaveTrm() != null && savings.getMaxLimit() != null) {
                totalMaxLimit = savings.getMaxLimit()
                    .multiply(BigDecimal.valueOf(rate.getSaveTrm()));
              }

              return ProductDetailResponse.InterestRateOption.builder()
                  .saveTrm(rate.getSaveTrm())
                  .intrRateTypeNm(FilterMappingUtil.convertInterestRateCodeToDisplayName(
                      rate.getIntrRateType()))
                  .intrRate(rate.getIntrRate())
                  .intrRate2(rate.getIntrRate2())
                  .rsrvTypeNm(
                      FilterMappingUtil.convertReserveTypeCodeToDisplayName(rate.getRsrvType()))
                  .totalMaxLimit(totalMaxLimit)
                  .build();
            })
            .collect(Collectors.toList())
        : List.of();

    // 기본 상품 정보 변환
    return ProductDetailResponse.builder()
        .finPrdtCd(savings.getFinPrdtCd())
        .finPrdtNm(savings.getFinPrdtNm())
        .productType("saving")
        .finCoNo(savings.getFinCoNo())
        .korCoNm(companyName)
        .joinWay(savings.getJoinWay())
        .joinDenyNm(FilterMappingUtil.convertJoinDenyCodeToDisplayName(savings.getJoinDeny()))
        .joinMember(savings.getJoinMember())
        .maxLimit(savings.getMaxLimit())
        .spclCnd(savings.getSpclCnd())
        .mtrtInt(savings.getMtrtInt())
        .etcNote(savings.getEtcNote())
        .dclsStrtDay(formatDate(savings.getDclsStrtDay()))
        .dclsEndDay(formatDate(savings.getDclsEndDay()))
        .interestRates(interestRateOptions)
        .build();
  }

  /**
   * LocalDate를 문자열로 포맷팅
   */
  private String formatDate(java.time.LocalDate date) {
    if (date == null) {
      return null;
    }
    return date.format(DATE_FORMATTER);
  }

}
