package com.project.savingbee.productAlert.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlertDispatchResponseDto {
  int processed;
  int sent;
  int failed;
}
