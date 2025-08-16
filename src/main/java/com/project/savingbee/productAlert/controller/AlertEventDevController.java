package com.project.savingbee.productAlert.controller;

import com.project.savingbee.productAlert.dto.AlertEventScanResponseDto;
import com.project.savingbee.productAlert.service.AlertMatchService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test/alert-events")
public class AlertEventDevController {
  private final AlertMatchService alertMatchService;

  // 알림 매칭 실행 -> 이벤트 큐 적재
  @PostMapping("/scan-now")
  public ResponseEntity<AlertEventScanResponseDto> scanNow() {
    int created = alertMatchService.scanAndEnqueue();

    return ResponseEntity.ok(new AlertEventScanResponseDto(created, LocalDateTime.now()));
  }
}
