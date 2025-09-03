package com.project.savingbee.productCompare.dto;

import com.project.savingbee.common.entity.DepositInterestRates;
import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.SavingsInterestRates;
import com.project.savingbee.common.entity.SavingsProducts;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProductInfoDto {

  private final String productId; // 상품코드
  private final String bankName;  // 금융회사명
  private final String productName; // 상품명

  private final BigDecimal intrRate;  // 기본금리
  private final BigDecimal intrRate2;  // 우대금리

  private Integer termMonth;  // 예치기간
  private String intrRateType;  // 단리(S) / 복리(M)

  // 예금 Dto 매핑
  public static ProductInfoDto fromDeposit(DepositInterestRates rate) {
    DepositProducts products = rate.getDepositProduct();

    return ProductInfoDto.builder()
        .productId(products.getFinPrdtCd())
        .bankName(products.getFinancialCompany().getKorCoNm())
        .productName(products.getFinPrdtNm())
        .intrRate(rate.getIntrRate())
        .intrRate2(rate.getIntrRate2())
        .termMonth(rate.getSaveTrm())
        .intrRateType(rate.getIntrRateType())
        .build();
  }

  // 적금 Dto 매핑
  public static ProductInfoDto fromSavings(SavingsInterestRates rate) {
    SavingsProducts products = rate.getSavingsProduct();

    return ProductInfoDto.builder()
        .productId(products.getFinPrdtCd())
        .bankName(products.getFinancialCompany().getKorCoNm())
        .productName(products.getFinPrdtNm())
        .intrRate(rate.getIntrRate())
        .intrRate2(rate.getIntrRate2())
        .termMonth(rate.getSaveTrm())
        .intrRateType(rate.getIntrRateType())
        .build();
  }
}
