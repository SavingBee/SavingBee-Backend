package com.project.savingbee.productAlert.controller;

import com.project.savingbee.productAlert.dto.AlertDispatchResponseDto;
import com.project.savingbee.productAlert.dto.AlertEventScanResponseDto;
import com.project.savingbee.productAlert.service.AlertDispatchService;
import com.project.savingbee.productAlert.service.AlertMatchService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test/alert-events")
public class AlertEventDevController {
  private final AlertMatchService alertMatchService;
  private final AlertDispatchService alertDispatchService;

  // 알림 매칭 실행 -> 이벤트 큐 적재
  @PostMapping("/scan-now")
  public ResponseEntity<AlertEventScanResponseDto> scanNow() {
    int created = alertMatchService.scanAndEnqueue();

    return ResponseEntity.ok(new AlertEventScanResponseDto(created, LocalDateTime.now()));
  }

  // 모든 매칭된 알림 이벤트 삭제
  @DeleteMapping("/all")
  public ResponseEntity<Void> deleteAllAlertEvents() {
    alertMatchService.deleteAllAlertEvents();
    return ResponseEntity.noContent().build();
  }

  // 알림 발송
  @PostMapping("/dispatch-now")
  public ResponseEntity<AlertDispatchResponseDto> dispatchNow(
      @RequestParam(defaultValue = "100") int batchSize) {

    AlertDispatchResponseDto r = alertDispatchService.dispatchNow(batchSize);

    return ResponseEntity.ok(new AlertDispatchResponseDto(r.getProcessed(), r.getSent(), r.getFailed()));
  }
}
