package com.project.savingbee.productCompare.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 세후 이자, 실수령액 계산
public final class CalcEngine {

  /**
   * 예금 만기시 실수령액 계산
   *
   * @param amount       예치금액
   * @param rate         세후 이자율
   * @param termMonth    예치기간
   * @param intrRateType 이자계산방식(단리/복리)
   */
  public static CalcResult deposit(
      BigDecimal amount, BigDecimal rate, int termMonth, String intrRateType) {

    BigDecimal A = amount.setScale(0, RoundingMode.DOWN); // 예치금(원)
    BigDecimal R = rate.movePointLeft(2); // % -> 소수
    BigDecimal I = R.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);

    BigDecimal interest = intrRateType.equals("S")
        ? A.multiply(R).multiply(BigDecimal.valueOf(termMonth)
        .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP))
        : A.multiply(BigDecimal.ONE.add(I).pow(termMonth).subtract(BigDecimal.ONE));

    long afterTaxInterest = interest.setScale(0, RoundingMode.DOWN).longValue();
    long amountReceived = A.longValue() + afterTaxInterest;

    return new CalcResult(afterTaxInterest, amountReceived);
  }

  /**
   * 적금 만기시 실수령액 계산
   *
   * @param amount       월 납입금액
   * @param rate         세후 이자율
   * @param termMonth    예치기간
   * @param intrRateType 이자계산방식(단리/복리)
   */
  public static CalcResult savings(
      BigDecimal amount, BigDecimal rate, int termMonth, String intrRateType) {

    BigDecimal A = amount.setScale(0, RoundingMode.DOWN);
    BigDecimal R = rate.movePointLeft(2);
    BigDecimal I = R.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

    BigDecimal totalAmount = A.multiply(BigDecimal.valueOf(termMonth)); // 총 납입원금

    BigDecimal interest;

    if (intrRateType.equals("S")) {
      // A * I * n(n+1)/2
      interest = A.multiply(I).multiply(BigDecimal.valueOf(termMonth))
          .multiply(BigDecimal.valueOf(termMonth + 1))
          .divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
    } else {
      if (I.signum() == 0) {
        interest = BigDecimal.ZERO;
      } else {
        // A * ((1+I)^n-1) / I
        interest = A.multiply(BigDecimal.ONE.add(I).pow(termMonth).subtract(BigDecimal.ONE))
            .divide(I, 10, RoundingMode.HALF_UP).subtract(totalAmount);
      }
    }

    long afterTaxInterest = interest.setScale(0, RoundingMode.DOWN).longValue();
    long amountReceived =
        totalAmount.setScale(0, RoundingMode.DOWN).longValue() + afterTaxInterest;

    return new CalcResult(afterTaxInterest, amountReceived);
  }

  @Getter
  @AllArgsConstructor
  public static class CalcResult {

    private final long afterTaxInterest;  // 세후 이자
    private final long amountReceived;  // 실수령액
  }

}
