package com.project.savingbee.productAlert.payload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.FinancialCompanies;
import com.project.savingbee.common.entity.SavingsProducts;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class AlertPayloadBuilder {

  private final ObjectMapper objectMapper;

  public String forDeposit(DepositProducts product, BigDecimal rate, FinancialCompanies company,
      LocalDateTime snapshotAt) {

    return buildCommon("DEPOSIT", product.getFinPrdtCd(), product.getFinPrdtNm(),
        company.getKorCoNm(), rate, snapshotAt);
  }

  public String forSavings(SavingsProducts product, BigDecimal rate, FinancialCompanies company,
      LocalDateTime snapshotAt) {

    return buildCommon("SAVINGS", product.getFinPrdtCd(), product.getFinPrdtNm(),
        company.getKorCoNm(), rate, snapshotAt);
  }

  private String buildCommon(String type, String productCode, String productName, String bankName,
      BigDecimal rate, LocalDateTime snapshotAt) {

    try {
      Map<String, Object> payload = Map.of(
          "type", type, "productCode", productCode, "productName", productName,
          "bankName", bankName, "rate", rate, "snapshotAt", snapshotAt.toString());
      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      log.warn("Build payload failed", e);
      return "{}";
    }
  }
}
