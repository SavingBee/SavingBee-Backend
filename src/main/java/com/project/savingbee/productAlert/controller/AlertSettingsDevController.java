package com.project.savingbee.productAlert.controller;

import com.project.savingbee.domain.user.service.UserService;
import com.project.savingbee.productAlert.service.ProductAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev/alerts/settings")
public class AlertSettingsDevController {
  private final ProductAlertService productAlertService;
  private final UserService userService;

  // 특정 사용자 알림 설정 삭제
  @DeleteMapping
  public ResponseEntity<Void> deleteAlertSettings(@AuthenticationPrincipal UserDetails userDetails) {
    Long userId = userService.findIdByUsername(userDetails.getUsername());
    productAlertService.deleteAlertSettings(userId);
    return ResponseEntity.noContent().build();
  }

  // 모든 알림 설정 삭제
  @DeleteMapping("/all")
  public ResponseEntity<Void> deleteAllAlertSettings() {
    productAlertService.deleteAllAlertSettings();
    return ResponseEntity.noContent().build();
  }
}
