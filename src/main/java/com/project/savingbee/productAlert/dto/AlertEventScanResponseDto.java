package com.project.savingbee.productAlert.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlertEventScanResponseDto {
  private int created;
  private LocalDateTime executedAt;
}
