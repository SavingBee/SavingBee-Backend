package com.project.savingbee.productCompare.service;

import com.project.savingbee.common.entity.DepositInterestRates;
import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.SavingsInterestRates;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.repository.DepositInterestRatesRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.productCompare.dto.CompareExecuteRequestDto;
import com.project.savingbee.productCompare.dto.CompareRequestDto;
import com.project.savingbee.productCompare.dto.CompareResponseDto;
import com.project.savingbee.productCompare.dto.ProductCompareInfosDto;
import com.project.savingbee.productCompare.dto.ProductInfoDto;
import com.project.savingbee.productCompare.util.CalcEngine;
import com.project.savingbee.productCompare.util.CalcEngine.CalcResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 읽기 전용
public class ProductCompareService {
  private final DepositInterestRatesRepository depositInterestRatesRepository;
  private final SavingsInterestRatesRepository savingsInterestRatesRepository;

  // 상품 필터링
  public List<ProductInfoDto> findFilteredProducts(CompareRequestDto requestDto) {
    return requestDto.getType().equals("D") ?
        findDepositProducts(requestDto) : findSavingsProducts(requestDto);
  }

  // 예금 필터링
  private List<ProductInfoDto> findDepositProducts(CompareRequestDto requestDto) {
    // 예치 기간이 일치한 금리 정보 조회
    List<DepositInterestRates> rates =
        depositInterestRatesRepository.findAllBySaveTrmOrderByFinPrdtCd(requestDto.getTermMonth());

    return rates.stream()
        // 단리 / 복리 / 상관없음
        .filter(r -> matchesIntrRateType(requestDto, r.getIntrRateType()))
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
        // 단리 / 복리 / 상관없음
        .filter(r -> matchesIntrRateType(requestDto, r.getIntrRateType()))
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

  // 단리 / 복리 / 상관없음
  private boolean matchesIntrRateType(CompareRequestDto requestDto, String intrRateType) {
    if (requestDto.getIntrRateType().equals("Any")) {
      return true;
    }
    return requestDto.getIntrRateType().equals(intrRateType);
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
        requestDto.getType().equals("D") ? compareDeposit(requestDto) : compareSavings(requestDto));

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
    int saveTrm = requestDto.getTermMonth();

    // 선택한 두 상품 정보 가져오기(상품코드 + 이자계산방식 + 예치기간으로)
    List<ProductCompareInfosDto> products = new ArrayList<>(requestDto.getSelections().size());
    for (CompareExecuteRequestDto.Selection selection : requestDto.getSelections()) {
      DepositInterestRates r = depositInterestRatesRepository.findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm(
              selection.getProductId(), selection.getIntrRateType(), saveTrm)
          .orElseThrow(
              () -> new IllegalArgumentException("Invalid productIds/intrRateType/termMonth."));

      DepositProducts p = r.getDepositProduct();

      // 세후 이자, 실수령액 계산
      CalcResult c = CalcEngine.deposit(
          requestDto.getAmount(), r.getIntrRate(), saveTrm, r.getIntrRateType());

      products.add(ProductCompareInfosDto.builder()
          .productId(p.getFinPrdtCd())
          .bankName(p.getFinancialCompany().getKorCoNm())
          .productName(p.getFinPrdtNm())
          .intrRateBeforeTax(r.getIntrRate2())
          .intrRateAfterTax(r.getIntrRate())
          .highestPrefRate(r.getIntrRate2())
          .intrAfterTax(c.getAfterTaxInterest())
          .amountReceived(c.getAmountReceived())
          .intrRateType(r.getIntrRateType())
          .build());
    }

    return products;
  }

  // 적금 상품 비교
  private List<ProductCompareInfosDto> compareSavings(CompareExecuteRequestDto requestDto) {
    int saveTrm = requestDto.getTermMonth();

    // 선택한 두 상품 정보 가져오기(상품코드 + 이자계산방식 + 예치기간으로)
    List<ProductCompareInfosDto> products = new ArrayList<>(requestDto.getSelections().size());
    for (CompareExecuteRequestDto.Selection selection : requestDto.getSelections()) {
      SavingsInterestRates r = savingsInterestRatesRepository.findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm(
              selection.getProductId(), selection.getIntrRateType(), saveTrm)
          .orElseThrow(
              () -> new IllegalArgumentException("Invalid productIds/intrRateType/termMonth."));

      SavingsProducts p = r.getSavingsProduct();

      // 세후 이자, 실수령액 계산
      CalcResult c = CalcEngine.savings(
          requestDto.getAmount(), r.getIntrRate(), saveTrm, r.getIntrRateType());

      products.add(ProductCompareInfosDto.builder()
          .productId(p.getFinPrdtCd())
          .bankName(p.getFinancialCompany().getKorCoNm())
          .productName(p.getFinPrdtNm())
          .intrRateBeforeTax(r.getIntrRate2())
          .intrRateAfterTax(r.getIntrRate())
          .highestPrefRate(r.getIntrRate2())
          .intrAfterTax(c.getAfterTaxInterest())
          .amountReceived(c.getAmountReceived())
          .intrRateType(r.getIntrRateType())
          .build());
    }

    return products;
  }
}
