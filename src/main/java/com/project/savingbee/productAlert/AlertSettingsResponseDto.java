package com.project.savingbee.productAlert;

import com.project.savingbee.common.entity.ProductAlertSetting;
import com.project.savingbee.common.entity.ProductAlertSetting.AlertType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AlertSettingsResponseDto {
  private Long userId;
  private AlertType alertType;

  private boolean productTypeDeposit;
  private boolean productTypeSaving;

  private BigDecimal minInterestRate;

  private boolean interestCalcSimple;
  private boolean interestCalcCompound;

  private int maxSaveTerm;

  private BigInteger minAmount;
  private BigInteger maxLimit;

  private boolean rsrvTypeFlexible;
  private boolean rsrvTypeFixed;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public AlertSettingsResponseDto(ProductAlertSetting productAlertSetting) {
    this.userId = productAlertSetting.getUserId();
    this.alertType = productAlertSetting.getAlertType();
    this.productTypeDeposit = productAlertSetting.getProductTypeDeposit();
    this.productTypeSaving = productAlertSetting.getProductTypeSaving();
    this.minInterestRate = productAlertSetting.getMinInterestRate();
    this.interestCalcSimple = productAlertSetting.getInterestCalcSimple();
    this.interestCalcCompound = productAlertSetting.getInterestCalcCompound();
    this.maxSaveTerm = productAlertSetting.getMaxSaveTerm();
    this.minAmount = productAlertSetting.getMinAmount();
    this.maxLimit = productAlertSetting.getMaxLimit();
    this.rsrvTypeFlexible = productAlertSetting.getRsrvTypeFlexible();
    this.rsrvTypeFixed = productAlertSetting.getRsrvTypeFixed();
    this.createdAt = productAlertSetting.getCreatedAt();
    this.updatedAt = productAlertSetting.getUpdatedAt();
  }

  // 최초 설정 화면 진입 시 가져오는 default 값
  public static AlertSettingsResponseDto defaultDto(Long userId) {
    return new AlertSettingsResponseDto(
        userId,
        AlertType.EMAIL,
        false,
        false,
        null,
        false,
        false,
        1,
        null,
        null,
        false,
        false,
        null,
        null
    );
  }
}
