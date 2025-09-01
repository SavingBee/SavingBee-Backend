package com.project.savingbee.productAlert.controller;

import com.project.savingbee.domain.user.service.UserService;
import com.project.savingbee.productAlert.dto.AlertSettingsRequestDto;
import com.project.savingbee.productAlert.dto.AlertSettingsResponseDto;
import com.project.savingbee.productAlert.service.ProductAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alerts/settings")
public class ProductAlertController {
  private final ProductAlertService productAlertService;
  private final UserService userService;

  // 기존에 알람 설정된 조건 가져오기
  @GetMapping
  public ResponseEntity<AlertSettingsResponseDto> getAlertSettings(
      @AuthenticationPrincipal UserDetails userDetails) {

    Long userId = userService.findIdByUsername(userDetails.getUsername());

    AlertSettingsResponseDto alertSettingsResponseDto = productAlertService.getAlertSettings(userId);
    return ResponseEntity.ok(alertSettingsResponseDto);
  }

  // 상품 알림 조건 설정
  @PostMapping
  public ResponseEntity<AlertSettingsResponseDto> createAlertSettings(
      @RequestBody @Valid AlertSettingsRequestDto alertSettingsRequestDto,
      @AuthenticationPrincipal UserDetails userDetails) throws Exception {

    Long userId = userService.findIdByUsername(userDetails.getUsername());

    AlertSettingsResponseDto alertSettingsResponseDto =
        productAlertService.createAlertSettings(userId, alertSettingsRequestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(alertSettingsResponseDto);
  }

  // 상품 알림 조건 수정
  @PatchMapping
  public ResponseEntity<AlertSettingsResponseDto> updateAlertSettings(
      @RequestBody @Valid AlertSettingsRequestDto alertSettingsRequestDto,
      @AuthenticationPrincipal UserDetails userDetails) throws Exception {

    Long userId = userService.findIdByUsername(userDetails.getUsername());

    AlertSettingsResponseDto alertSettingsResponseDto =
        productAlertService.updateAlertSettings(userId, alertSettingsRequestDto);
    return ResponseEntity.ok(alertSettingsResponseDto);
  }
}
