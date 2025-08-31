package com.project.savingbee.productCompare.service;

import com.project.savingbee.common.entity.DepositInterestRates;
import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.SavingsInterestRates;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.repository.DepositInterestRatesRepository;
import com.project.savingbee.common.repository.FinancialCompaniesRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.productCompare.dto.CompareExecuteRequestDto;
import com.project.savingbee.productCompare.dto.CompareRequestDto;
import com.project.savingbee.productCompare.dto.CompareResponseDto;
import com.project.savingbee.productCompare.dto.PageResponseDto;
import com.project.savingbee.productCompare.dto.PageResponseDto.MatchedBank;
import com.project.savingbee.productCompare.dto.PageResponseDto.MatchedBankInfo;
import com.project.savingbee.productCompare.dto.ProductCompareInfosDto;
import com.project.savingbee.productCompare.dto.ProductInfoDto;
import com.project.savingbee.productCompare.util.CalcEngine;
import com.project.savingbee.productCompare.util.CalcEngine.CalcResult;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 읽기 전용
public class ProductCompareService {
  private final DepositInterestRatesRepository depositInterestRatesRepository;
  private final SavingsInterestRatesRepository savingsInterestRatesRepository;
  private final FinancialCompaniesRepository financialCompaniesRepository;

  // 상품 필터링
  public PageResponseDto<ProductInfoDto> findFilteredProducts(CompareRequestDto requestDto, Pageable pageable) {
    List<ProductInfoDto> productInfoDtos = requestDto.getType().equals("D")
        ? findDepositProducts(requestDto)
        : findSavingsProducts(requestDto);

    MatchedBankInfo matchedBankInfo = null;

    // BankKeyword가 있을 경우
    if (requestDto.getBankKeyword() != null) {
      String keyword = normalizeKeyword(requestDto.getBankKeyword());

      // BankKeyword가 포함되는 금융회사명 목록
      List<MatchedBank> matchedBanks =
          financialCompaniesRepository.findByKorCoNmContainingOrderByFinCoNo(keyword);

      matchedBankInfo = new MatchedBankInfo(requestDto.getBankKeyword(), matchedBanks);

      // BankKeyword가 포함되는 금융회사명으로 필터링
      productInfoDtos = productInfoDtos.stream()
          .filter(p -> p.getBankName().contains(keyword)).toList();
    }

    // 우대금리 내림차순(null일 경우 기본금리를 비교), 동률일 경우 상품코드 오름차순
    Comparator<ProductInfoDto> intrRate2Desc =
        Comparator.comparing(
            (ProductInfoDto p) -> {
              BigDecimal rate2 = p.getIntrRate2();
              return (rate2 != null) ? rate2 : p.getIntrRate();
            }).reversed().thenComparing(ProductInfoDto::getProductId);

    List<ProductInfoDto> sorted = productInfoDtos.stream().sorted(intrRate2Desc).toList();

    return PageResponseDto.fromList(sorted, pageable, matchedBankInfo);
  }

  // 예금 필터링
  private List<ProductInfoDto> findDepositProducts(CompareRequestDto requestDto) {
    // 예치 기간이 일치한 금리 정보 조회
    List<DepositInterestRates> rates =
        depositInterestRatesRepository.findAllBySaveTrmOrderByFinPrdtCd(requestDto.getTermMonth());

    return rates.stream()
        // 단리 / 복리
        .filter(r -> requestDto.getIntrRateType().equalsIgnoreCase(r.getIntrRateType()))
        // 최소 이자율(우대금리 기준, 없으면 기본금리)
        .filter(r -> {
          BigDecimal base = r.getIntrRate2() != null ? r.getIntrRate2() : r.getIntrRate();
          return base != null && base.compareTo(requestDto.getMinRate()) >= 0;
        })
        // 예치금 범위
        .filter(r -> ableDepositAmount(requestDto, r.getDepositProduct()))
        .map(ProductInfoDto::fromDeposit)
        .toList();
  }

  // 적금 필터링
  private List<ProductInfoDto> findSavingsProducts(CompareRequestDto requestDto) {
    // 예치 기간이 일치한 금리 정보 조회
    List<SavingsInterestRates> rates =
        savingsInterestRatesRepository.findAllBySaveTrmOrderByFinPrdtCd(requestDto.getTermMonth());

    return rates.stream()
        // 단리 / 복리
        .filter(r -> requestDto.getIntrRateType().equalsIgnoreCase(r.getIntrRateType()))
        // 최소 이자율(우대금리 기준, 없으면 기본금리)
        .filter(r -> {
          BigDecimal base = r.getIntrRate2() != null ? r.getIntrRate2() : r.getIntrRate();
          return base != null && base.compareTo(requestDto.getMinRate()) >= 0;
        })
        // 월 납입금액 범위
        .filter(r -> ableSavingsAmount(requestDto, r))
        .map(ProductInfoDto::fromSavings)
        .toList();
  }

  // 최소 가입금액 <= 예치 금액 <= 최대 한도
  private boolean ableDepositAmount(CompareRequestDto requestDto, DepositProducts product) {
    BigDecimal amount = requestDto.getAmount();
    BigDecimal min = product.getMinAmount();
    BigDecimal max = product.getMaxLimit();

    if (min != null && amount.compareTo(min) < 0) {
      return false;
    }
    if (max != null && amount.compareTo(max) > 0) {
      return false;
    }

    return true;
  }

  // 최소 월 납입금액 <= 월 납입금액 <= 최대 월 납입금액
  private boolean ableSavingsAmount(CompareRequestDto requestDto, SavingsInterestRates rate) {
    BigDecimal amount = requestDto.getAmount();
    BigDecimal min = rate.getMonthlyLimitMin();
    BigDecimal max = rate.getMonthlyLimitMax();

    if (min != null && amount.compareTo(min) < 0) {
      return false;
    }
    if (max != null && amount.compareTo(max) > 0) {
      return false;
    }

    return true;
  }

  // 상품 비교
  public CompareResponseDto compareProducts(CompareExecuteRequestDto requestDto) {
    List<ProductCompareInfosDto> products = new ArrayList<>(
        requestDto.getType().equalsIgnoreCase("D")
            ? compareDeposit(requestDto) : compareSavings(requestDto));

    // 입력 순서 유지 정렬(먼저 선택한 상품을 왼쪽에)
    List<String> ids = requestDto.getProductIds();
    products.sort(Comparator.comparingInt(p -> ids.indexOf(p.getProductId())));

    long maxAmount = products.stream().mapToLong(ProductCompareInfosDto::getAmountReceived).max().orElse(0);
    long winners = products.stream().filter(p -> p.getAmountReceived() == maxAmount).count();

    // 실수령액이 같은 경우 winner -> 둘 다 false
    products = products.stream().map(p ->
        p.toBuilder().winner(winners == 1 && p.getAmountReceived() == maxAmount).build())
        .toList();

    String winnerId = (winners == 1)
        ? products.stream().filter(ProductCompareInfosDto::isWinner)
            .findFirst().map(ProductCompareInfosDto::getProductId).orElse(null)
        : null;

    return new CompareResponseDto(products, winnerId);
  }

  // 예금 상품 비교
  private List<ProductCompareInfosDto> compareDeposit(CompareExecuteRequestDto requestDto) {
    List<String> ids = requestDto.getProductIds();
    int saveTrm = requestDto.getTermMonth();
    String intrRateType = requestDto.getIntrRateType();

    // 선택한 두 상품 정보 가져오기(상품코드 + 이자계산방식 + 예치기간으로)
    List<DepositInterestRates> rates = depositInterestRatesRepository
        .findAllByFinPrdtCdInAndIntrRateTypeAndSaveTrm(ids, intrRateType, saveTrm);

    if (rates.size() != ids.size()) {
      throw new IllegalArgumentException("Invalid productIds/intrRateType/termMonth.");
    }

    return rates.stream().map(r -> {
      DepositProducts p = r.getDepositProduct();

      // 세후 이자, 실수령액 계산
      CalcResult c = CalcEngine.deposit(
          requestDto.getAmount(), r.getIntrRate(), saveTrm, intrRateType);

      return ProductCompareInfosDto.builder()
          .productId(p.getFinPrdtCd())
          .bankName(p.getFinancialCompany().getKorCoNm())
          .productName(p.getFinPrdtNm())
          .intrRateBeforeTax(r.getIntrRate2())
          .intrRateAfterTax(r.getIntrRate())
          .highestPrefRate(r.getIntrRate2())
          .intrAfterTax(c.getAfterTaxInterest())
          .amountReceived(c.getAmountReceived())
          .intrRateType(r.getIntrRateType())
          .build();
    }).toList();
  }

  // 적금 상품 비교
  private List<ProductCompareInfosDto> compareSavings(CompareExecuteRequestDto requestDto) {
    List<String> ids = requestDto.getProductIds();
    int saveTrm = requestDto.getTermMonth();
    String intrRateType = requestDto.getIntrRateType();

    // 선택한 두 상품 정보 가져오기(상품코드 + 이자계산방식 + 예치기간으로)
    List<SavingsInterestRates> rates = savingsInterestRatesRepository
        .findAllByFinPrdtCdInAndIntrRateTypeAndSaveTrm(ids, intrRateType, saveTrm);

    if (rates.size() != ids.size()) {
      throw new IllegalArgumentException("Invalid productIds/intrRateType/termMonth.");
    }

    return rates.stream().map(r -> {
      SavingsProducts p = r.getSavingsProduct();

      // 세후 이자, 실수령액 계산
      CalcResult c = CalcEngine.savings(
          requestDto.getAmount(), r.getIntrRate(), saveTrm, intrRateType);

      return ProductCompareInfosDto.builder()
          .productId(p.getFinPrdtCd())
          .bankName(p.getFinancialCompany().getKorCoNm())
          .productName(p.getFinPrdtNm())
          .intrRateBeforeTax(r.getIntrRate2())
          .intrRateAfterTax(r.getIntrRate())
          .highestPrefRate(r.getIntrRate2())
          .intrAfterTax(c.getAfterTaxInterest())
          .amountReceived(c.getAmountReceived())
          .intrRateType(r.getIntrRateType())
          .build();
    }).toList();
  }

  // 입력받은 키워드를 정규화
  private String normalizeKeyword(String keyword) {
    return Normalizer.normalize(keyword, Form.NFKC)
        .toUpperCase(Locale.ROOT) // 소문자 -> 대문자
        .replaceAll("\\s+", ""); // 모든 공백 제거
  }
}
