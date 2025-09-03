package com.project.savingbee.productAlert.dto;

import com.project.savingbee.common.entity.ProductAlertSetting.AlertType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AlertSettingsRequestDto {

  private AlertType alertType;

  private Boolean productTypeDeposit;
  private Boolean productTypeSaving;

  @DecimalMin("0.0")
  private BigDecimal minInterestRate;

  private Boolean interestCalcSimple;
  private Boolean interestCalcCompound;

  @Positive
  private int maxSaveTerm;

  @PositiveOrZero
  private BigInteger minAmount;
  @PositiveOrZero
  private BigInteger maxLimit;
}
