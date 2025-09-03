package com.project.savingbee.productCompare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.project.savingbee.common.entity.DepositInterestRates;
import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.SavingsInterestRates;
import com.project.savingbee.common.repository.DepositInterestRatesRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.productCompare.dto.CompareRequestDto;
import com.project.savingbee.productCompare.dto.PageResponseDto;
import com.project.savingbee.productCompare.dto.ProductInfoDto;
import com.project.savingbee.productCompare.service.ProductCompareService;
import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductCompareServiceFilteringTest {
  @Mock
  private DepositInterestRatesRepository depositInterestRatesRepository;
  @Mock
  private SavingsInterestRatesRepository savingsInterestRatesRepository;

  @InjectMocks
  private ProductCompareService productCompareService;

  private CompareRequestDto requestDto(String type, String amount, int term, String minRate, String intrType) {
    CompareRequestDto requestDto = new CompareRequestDto();
    requestDto.setType(type); // D / S
    requestDto.setAmount(new BigDecimal(amount));
    requestDto.setTermMonth(term);
    requestDto.setMinRate(new BigDecimal(minRate));
    requestDto.setIntrRateType(intrType); // S / M / Any
    return requestDto;
  }

  private Pageable pageable(int page, int size) {
    return PageRequest.of(page, size);
  }

  // 필터링 통과하는 케이스 용
  private DepositInterestRates depositRateMapped(
      String prdtCd, String intr2, String intrType,
      String minAmt, String maxAmt, int term) {

    DepositInterestRates r = mock(DepositInterestRates.class, Answers.RETURNS_DEEP_STUBS);
    DepositProducts p = r.getDepositProduct();

    given(r.getDepositProduct()).willReturn(p);

    given(p.getFinPrdtCd()).willReturn(prdtCd);
    given(p.getMinAmount()).willReturn(new BigDecimal(minAmt));
    given(p.getMaxLimit()).willReturn(new BigDecimal(maxAmt));

    // 우대금리, null일 경우 기본금리 값으로 대체(2.50 설정)
    given(r.getIntrRate2()).willReturn(intr2 != null ? new BigDecimal(intr2) : new BigDecimal("2.50"));
    given(r.getIntrRateType()).willReturn(intrType);
    given(r.getIntrRate()).willReturn(new BigDecimal("2.00"));
    given(r.getSaveTrm()).willReturn(term);
    return r;
  }

  // 타입에서 탈락하는 케이스 용
  private DepositInterestRates depositRateTypeOnly(String intrType) {
    DepositInterestRates r = mock(DepositInterestRates.class);
    given(r.getIntrRateType()).willReturn(intrType);
    return r;
  }

  // 최소 이자율에서 탈락하는 케이스 용
  private DepositInterestRates depositRateMinOnly(String intr2, String intrType) {
    DepositInterestRates r = mock(DepositInterestRates.class);
    given(r.getIntrRateType()).willReturn(intrType);
    given(r.getIntrRate2()).willReturn(new BigDecimal(intr2));
    return r;
  }

  // 금액에서 탈락하는 케이스 용
  private DepositInterestRates depositRateWithAmountOnly(String intr2, String intrType,
      String minAmt, String maxAmt) {
    DepositInterestRates r = mock(DepositInterestRates.class, Answers.RETURNS_DEEP_STUBS);
    given(r.getIntrRateType()).willReturn(intrType);
    given(r.getIntrRate2()).willReturn(new BigDecimal(intr2));
    given(r.getDepositProduct().getMinAmount()).willReturn(new BigDecimal(minAmt));
    given(r.getDepositProduct().getMaxAmount()).willReturn(new BigDecimal(maxAmt));
    return r;
  }

  // 필터링 탈락하는 케이스 용
  private SavingsInterestRates savingsRateFilter(
      String prdtCd, String intr2, String intrType,
      String monthlyMin, String monthlyMax, int term) {

    SavingsInterestRates r = mock(SavingsInterestRates.class, Answers.RETURNS_DEEP_STUBS);

    given(r.getMonthlyLimitMin()).willReturn(new BigDecimal(monthlyMin));
    given(r.getMonthlyLimitMax()).willReturn(new BigDecimal(monthlyMax));
    given(r.getIntrRate2()).willReturn(new BigDecimal(intr2));
    given(r.getIntrRateType()).willReturn(intrType);

    return r;
  }

  // 필터링 통과하는 케이스 용
  private SavingsInterestRates savingsRateMapped(
      String prdtCd, String intr2, String intrType,
      String monthlyMin, String monthlyMax, int term) {

    SavingsInterestRates r = savingsRateFilter(prdtCd, intr2, intrType, monthlyMin, monthlyMax, term);

    given(r.getSavingsProduct().getFinPrdtCd()).willReturn(prdtCd);
    given(r.getIntrRate()).willReturn(new BigDecimal("2.00"));
    given(r.getSaveTrm()).willReturn(term);

    return r;
  }

  @Nested
  @DisplayName("예금 필터링")
  class DepositFiltering {

    @Test
    @DisplayName("단리(S), 최소 이자율, 예치금액 범위 필터링")
    void depositFilter() {
        // given
      int term = 12;
      DepositInterestRates r1 = depositRateMapped("A", "3.40", "S", "1000000", "5000000", term); // 통과
      DepositInterestRates r2 = depositRateMapped("B", "3.20", "S", "1000000", "5000000", term); // 통과
      DepositInterestRates r3 = depositRateTypeOnly("M"); // 타입 불일치
      DepositInterestRates r4 = depositRateMinOnly("2.90", "S"); // 금리 미달
      DepositInterestRates r5 = depositRateWithAmountOnly("3.30", "S", "6000000", "10000000"); // 금액 범위 불일치

      given(depositInterestRatesRepository.findAllBySaveTrmOrderByFinPrdtCd(term))
          .willReturn(Arrays.asList(r1, r2, r3, r4, r5));

      CompareRequestDto dto = requestDto("D", "3000000", term, "3.00", "S");
      Pageable pageable = pageable(0, 20);

        // when
      PageResponseDto<ProductInfoDto> result = productCompareService.findFilteredProducts(dto, pageable);

        // then
      then(depositInterestRatesRepository).should().findAllBySaveTrmOrderByFinPrdtCd(term);

      assertThat(result.getPage()).isEqualTo(0);
      assertThat(result.getSize()).isEqualTo(20);
      assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("예치금이 경계값일 때")
    void depositAmountRangInclusive() {
        // given
      int term = 6;
      DepositInterestRates rMin = depositRateMapped("MIN", "3.00", "S", "1000000", "5000000", term);
      DepositInterestRates rMax = depositRateMapped("MAX", "3.10", "S", "1000000", "5000000", term);

      given(depositInterestRatesRepository.findAllBySaveTrmOrderByFinPrdtCd(term))
          .willReturn(Arrays.asList(rMin, rMax));

      Pageable pageable = pageable(0, 20);

        // when
      CompareRequestDto minDto = requestDto("D", "1000000", term, "0.00", "S"); // min
      CompareRequestDto maxDto = requestDto("D", "5000000", term, "0.00", "S"); // max

      PageResponseDto<ProductInfoDto> atMin = productCompareService.findFilteredProducts(minDto, pageable);
      PageResponseDto<ProductInfoDto> atMax = productCompareService.findFilteredProducts(maxDto, pageable);

        // then
      assertThat(atMin.getTotalElements()).isEqualTo(2);
      assertThat(atMin.getContent())
          .extracting(ProductInfoDto::getProductId).containsExactly("MAX", "MIN");  // 상품코드 정렬
    }

    @Test
    @DisplayName("우대금리(null일 경우 기본금리 값으로 대체) 내림차순, 동률시 상품코드 오름차순")
    void depositSortByPrefRateDesc() {
      int term = 12;

      DepositInterestRates c = depositRateMapped("C", "3.40", "S", "1000000", "5000000", term);
      DepositInterestRates a = depositRateMapped("A", "4.10", "S", "1000000", "5000000", term);
      DepositInterestRates b = depositRateMapped("B", "3.40", "S", "1000000", "5000000", term);
      DepositInterestRates e = depositRateMapped("E", null,"S", "1000000", "5000000", term);
      DepositInterestRates d = depositRateMapped("D", "2.90", "S", "1000000", "5000000", term);

      // 섞어서 반환 -> 서비스에서 정렬되는지 검증
      given(depositInterestRatesRepository.findAllBySaveTrmOrderByFinPrdtCd(term))
          .willReturn(Arrays.asList(c, a, b, e, d));

      CompareRequestDto dto = requestDto("D", "3000000", term, "0.00", "S");
      Pageable page = pageable(0, 10);

      PageResponseDto<ProductInfoDto> result = productCompareService.findFilteredProducts(dto, page);

      // 정렬 - a b c d e 순(우대금리(null일 경우 기본금리 값으로 대체) 내림차순, 동률 시 상품코드 오름차순)
      assertThat(result.getContent()).extracting(ProductInfoDto::getProductId)
          .containsExactly("A", "B", "C", "D", "E");
    }
  }

  @Nested
  @DisplayName("적금 필터링")
  class SavingsFiltering {

    @Test
    @DisplayName("월 납입금액이 월 한도 범위에 있을 때(경계 포함)")
    void savingsMonthlyAmountRangeInclusive() {
        // given
      int term = 24;
      SavingsInterestRates r1 = savingsRateMapped("S1", "4.00", "S", "100000", "500000", term);
      SavingsInterestRates r2 = savingsRateMapped("S2", "3.50", "S", "300000", "700000", term);

      given(savingsInterestRatesRepository.findAllBySaveTrmOrderByFinPrdtCd(term))
          .willReturn(Arrays.asList(r1, r2));

      Pageable pageable = PageRequest.of(0, 20);

        // when
      CompareRequestDto dto1 = requestDto("S", "100000", term, "0.00", "S"); // min
      CompareRequestDto dto2 = requestDto("S", "700000", term, "0.00", "S"); // max

      PageResponseDto<ProductInfoDto> atLower =
          productCompareService.findFilteredProducts(dto1, pageable);
      PageResponseDto<ProductInfoDto> atUpper =
          productCompareService.findFilteredProducts(dto2, pageable);

        // then
      then(savingsInterestRatesRepository).should(times(2)).findAllBySaveTrmOrderByFinPrdtCd(term);
      assertThat(atLower.getPage()).isEqualTo(0);
      assertThat(atLower.getSize()).isEqualTo(20);
      assertThat(atLower.getTotalElements()).isEqualTo(1);
      assertThat(atLower.getContent())
          .extracting(ProductInfoDto::getProductId)
          .containsExactly("S1");

      assertThat(atUpper.getPage()).isEqualTo(0);
      assertThat(atUpper.getSize()).isEqualTo(20);
      assertThat(atUpper.getTotalElements()).isEqualTo(1);
      assertThat(atUpper.getContent())
          .extracting(ProductInfoDto::getProductId)
          .containsExactly("S2");
    }

    @Test
    @DisplayName("복리(M), 최소 이자율 필터링")
    void savingsFilter() {
        // given
      int term = 12;
      SavingsInterestRates mOk = savingsRateMapped("OK", "4.10", "M", "1", "99999999", term); // 통과
      SavingsInterestRates mLow = savingsRateFilter("LOW", "3.00", "M", "1", "99999999", term); // 금리 미달
      SavingsInterestRates sType = savingsRateFilter("S", "5.00", "S", "1", "99999999", term);  // 타입 불일치

      given(savingsInterestRatesRepository.findAllBySaveTrmOrderByFinPrdtCd(term))
          .willReturn(Arrays.asList(mOk, mLow, sType));

      CompareRequestDto dto = requestDto("S", "10000", term, "4.00", "M");

      Pageable pageable = PageRequest.of(0, 20);

        // when
      PageResponseDto<ProductInfoDto> result =
          productCompareService.findFilteredProducts(dto, pageable);

        // then
      assertThat(result.getPage()).isEqualTo(0);
      assertThat(result.getSize()).isEqualTo(20);
      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent())
          .extracting(ProductInfoDto::getProductId)
          .containsExactly("OK");
    }
  }
}
