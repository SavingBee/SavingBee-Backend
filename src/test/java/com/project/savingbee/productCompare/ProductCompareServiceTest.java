package com.project.savingbee.productCompare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.project.savingbee.common.entity.DepositInterestRates;
import com.project.savingbee.common.entity.SavingsInterestRates;
import com.project.savingbee.common.repository.DepositInterestRatesRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.productCompare.dto.CompareExecuteRequestDto;
import com.project.savingbee.productCompare.dto.CompareResponseDto;
import com.project.savingbee.productCompare.dto.ProductCompareInfosDto;
import com.project.savingbee.productCompare.service.ProductCompareService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductCompareServiceTest {
  @Mock
  private DepositInterestRatesRepository depositInterestRatesRepository;
  @Mock
  private SavingsInterestRatesRepository savingsInterestRatesRepository;

  @InjectMocks
  private ProductCompareService productCompareService;

  private CompareExecuteRequestDto depositRequestDto(List<String> ids, String amount, int term, String intrType) {
    CompareExecuteRequestDto requestDto = new CompareExecuteRequestDto();
    requestDto.setType("D");
    requestDto.setAmount(new BigDecimal(amount));
    requestDto.setTermMonth(term);

    List<CompareExecuteRequestDto.Selection> selections =
        ids.stream().map(id -> {
          CompareExecuteRequestDto.Selection selection = new CompareExecuteRequestDto.Selection();
          selection.setProductId(id);
          selection.setIntrRateType(intrType);
          return selection;
        }).collect(Collectors.toList());

    requestDto.setSelections(selections);
    return requestDto;
  }
  private CompareExecuteRequestDto savingsRequestDto(List<String> ids, String amount, int term, String intrType) {
    CompareExecuteRequestDto requestDto = new CompareExecuteRequestDto();
    requestDto.setType("S");
    requestDto.setAmount(new BigDecimal(amount));
    requestDto.setTermMonth(term);

    List<CompareExecuteRequestDto.Selection> selections =
        ids.stream().map(id -> {
          CompareExecuteRequestDto.Selection selection = new CompareExecuteRequestDto.Selection();
          selection.setProductId(id);
          selection.setIntrRateType(intrType);
          return selection;
        }).collect(Collectors.toList());

    requestDto.setSelections(selections);
    return requestDto;
  }

  private DepositInterestRates depositRate(String prdtCd, String bank, String name,
      String intr, String intr2, String intrType) {
    DepositInterestRates r = mock(DepositInterestRates.class, Answers.RETURNS_DEEP_STUBS);
    // product
    given(r.getDepositProduct().getFinPrdtCd()).willReturn(prdtCd);
    given(r.getDepositProduct().getFinancialCompany().getKorCoNm()).willReturn(bank);
    given(r.getDepositProduct().getFinPrdtNm()).willReturn(name);
    // rates
    given(r.getIntrRate()).willReturn(new BigDecimal(intr));   // 기본금리(세후 계산 입력)
    given(r.getIntrRate2()).willReturn(new BigDecimal(intr2)); // 우대금리/표시용
    given(r.getIntrRateType()).willReturn(intrType);
    return r;
  }

  private SavingsInterestRates savingsRate(String prdtCd, String bank, String name,
      String intr, String intr2, String intrType) {
    SavingsInterestRates r = mock(SavingsInterestRates.class, Answers.RETURNS_DEEP_STUBS);
    // product
    given(r.getSavingsProduct().getFinPrdtCd()).willReturn(prdtCd);
    given(r.getSavingsProduct().getFinancialCompany().getKorCoNm()).willReturn(bank);
    given(r.getSavingsProduct().getFinPrdtNm()).willReturn(name);
    // rates
    given(r.getIntrRate()).willReturn(new BigDecimal(intr));
    given(r.getIntrRate2()).willReturn(new BigDecimal(intr2));
    given(r.getIntrRateType()).willReturn(intrType);
    return r;
  }

  @Nested
  @DisplayName("예금 비교")
  class DepositCompare {

    @Test
    @DisplayName("두 상품의 금리가 다를 때, 더 높은 금리가 winner")
    void depositWinnerAndPreserveInputOrder() {
        // given
      int term = 12;
      DepositInterestRates A = depositRate("A", "은행A", "상품A", "3.10", "3.40", "S");
      DepositInterestRates B = depositRate("B", "은행B", "상품B", "3.20", "3.50", "S");

      CompareExecuteRequestDto requestDto = depositRequestDto(Arrays.asList("B","A"), "3000000", term, "S");

      given(depositInterestRatesRepository
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("B", "S", term))
          .willReturn(Optional.of(B));
      given(depositInterestRatesRepository
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("A", "S", term))
          .willReturn(Optional.of(A));

        // when
      CompareResponseDto responseDto = productCompareService.compareProducts(requestDto);

        // then
      then(depositInterestRatesRepository).should()
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("B", "S", term);
      then(depositInterestRatesRepository).should()
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("A", "S", term);

      then(depositInterestRatesRepository).shouldHaveNoMoreInteractions();

      // B 금리가 더 높으므로 수령액도 높아 winnerId = "B"
      assertThat(responseDto.getWinnerId()).isEqualTo("B");
      assertThat(responseDto.getInfo()).filteredOn(ProductCompareInfosDto::isWinner)
          .extracting(ProductCompareInfosDto::getProductId)
          .containsExactly("B");
    }

    @Test
    @DisplayName("두 상품의 금리가 동일할 때(winner=null, isWinner=false)")
    void depositSameResults() {
        // given
      int term = 12;
      DepositInterestRates A = depositRate("A", "은행A", "상품A", "3.10", "3.40", "S");
      DepositInterestRates B = depositRate("B", "은행B", "상품B", "3.10", "3.40", "S");

      given(depositInterestRatesRepository
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("A", "S", term))
          .willReturn(Optional.of(A));
      given(depositInterestRatesRepository
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("B", "S", term))
          .willReturn(Optional.of(B));

      CompareExecuteRequestDto requestDto = depositRequestDto(Arrays.asList("A","B"), "3000000", term, "S");

        // when
      CompareResponseDto responseDto = productCompareService.compareProducts(requestDto);

        // then
      assertThat(responseDto.getWinnerId()).isNull();
      assertThat(responseDto.getInfo()).extracting(ProductCompareInfosDto::isWinner)
          .containsExactly(false, false);
    }

    @Test
    @DisplayName("상품코드, 이자계산방식 반환 오류")
    void depositInvalidSelection() {
        // given
      int term = 12;
      DepositInterestRates A = depositRate("A", "은행A", "상품A", "3.10", "3.40", "S");

      given(depositInterestRatesRepository
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("A", "S", term))
          .willReturn(Optional.of(A));
      given(depositInterestRatesRepository
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("B", "S", term))
          .willReturn(Optional.empty());

      CompareExecuteRequestDto requestDto = depositRequestDto(Arrays.asList("A","B"), "3000000", term, "S");

        // when

        // then
      assertThatThrownBy(() -> productCompareService.compareProducts(requestDto))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid productIds/intrRateType/termMonth.");
    }
  }

  @Nested
  @DisplayName("적금 비교")
  class SavingsCompare {

    @Test
    @DisplayName("두 상품의 금리가 다를 때, 더 높은 금리가 winner")
    void savingsWinnerAndPreserveInputOrder() {
        // given
      int term = 24;
      SavingsInterestRates X = savingsRate("X", "은행X", "적금X", "4.00", "4.20", "S");
      SavingsInterestRates Y = savingsRate("Y", "은행Y", "적금Y", "3.50", "3.70", "S");

      given(savingsInterestRatesRepository
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("Y", "S", term))
          .willReturn(Optional.of(Y));
      given(savingsInterestRatesRepository
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("X", "S", term))
          .willReturn(Optional.of(X));

      CompareExecuteRequestDto requestDto = savingsRequestDto(Arrays.asList("Y","X"), "100000", term, "S");

        // when
      CompareResponseDto responseDto = productCompareService.compareProducts(requestDto);

        // then
      then(savingsInterestRatesRepository).should()
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("Y", "S", term);
      then(savingsInterestRatesRepository).should()
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("X", "S", term);
      then(savingsInterestRatesRepository).shouldHaveNoMoreInteractions();

      // X 금리가 더 높으므로 수령액도 높아 winnerId = "X"
      assertThat(responseDto.getWinnerId()).isEqualTo("X");
    }

    @Test
    @DisplayName("두 상품의 금리가 동일할 때(winner=null, isWinner=false)")
    void savingsSameResults() {
        // given
      int term = 24;
      SavingsInterestRates X = savingsRate("X", "은행X", "적금X", "3.50", "3.70", "S");
      SavingsInterestRates Y = savingsRate("Y", "은행Y", "적금Y", "3.50", "3.70", "S");

      given(savingsInterestRatesRepository
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("Y", "S", term))
          .willReturn(Optional.of(Y));
      given(savingsInterestRatesRepository
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("X", "S", term))
          .willReturn(Optional.of(X));

      CompareExecuteRequestDto requestDto = savingsRequestDto(Arrays.asList("Y","X"), "100000", term, "S");

      // when
      CompareResponseDto responseDto = productCompareService.compareProducts(requestDto);

      // then
      assertThat(responseDto.getWinnerId()).isNull();
      assertThat(responseDto.getInfo()).extracting(ProductCompareInfosDto::isWinner)
          .containsExactly(false, false);
    }

    @Test
    @DisplayName("상품코드, 이자계산방식 반환 오류")
    void savingsInvalidSelection() {
        // given
      int term = 24;
      SavingsInterestRates X = savingsRate("X", "은행X", "적금X", "3.50", "3.70", "S");

      given(savingsInterestRatesRepository
          .findFirstByFinPrdtCdAndIntrRateTypeAndSaveTrm("X", "S", term))
          .willReturn(Optional.of(X));

      CompareExecuteRequestDto requestDto = savingsRequestDto(Arrays.asList("X","Y"), "100000", term, "S");

        // when

        // then
      assertThatThrownBy(() -> productCompareService.compareProducts(requestDto))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid productIds/intrRateType/termMonth.");
    }
  }
}
