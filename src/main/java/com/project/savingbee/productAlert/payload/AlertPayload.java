package com.project.savingbee.productAlert.payload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AlertPayload {

  private String type;
  private String productCode;
  private String productName;
  private String bankName;
  private BigDecimal rate;
  private LocalDateTime snapshotAt;
}
