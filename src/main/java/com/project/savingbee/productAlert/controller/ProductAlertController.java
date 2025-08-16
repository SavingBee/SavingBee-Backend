package com.project.savingbee.productAlert.controller;

import com.project.savingbee.productAlert.dto.AlertSettingsRequestDto;
import com.project.savingbee.productAlert.dto.AlertSettingsResponseDto;
import com.project.savingbee.productAlert.service.ProductAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alerts/settings")
public class ProductAlertController {
  private final ProductAlertService productAlertService;

  // 기존에 알람 설정된 조건 가져오기
  @GetMapping
  public ResponseEntity<AlertSettingsResponseDto> getAlertSettings(
      @RequestParam Long userId     // 인증 구현 전 임시로 사용
//       @AuthenticationPrincipal 인증 구현 시 적용

  ) {
    AlertSettingsResponseDto alertSettingsResponseDto = productAlertService.getAlertSettings(userId);
    return ResponseEntity.ok(alertSettingsResponseDto);
  }

  // 상품 알림 조건 설정
  @PostMapping
  public ResponseEntity<AlertSettingsResponseDto> createAlertSettings(
      @RequestBody @Valid AlertSettingsRequestDto alertSettingsRequestDto,
      @RequestParam Long userId
//      , @AuthenticationPrincipal
      ) throws BadRequestException {

    AlertSettingsResponseDto alertSettingsResponseDto =
        productAlertService.createAlertSettings(userId, alertSettingsRequestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(alertSettingsResponseDto);
  }

  // 상품 알림 조건 수정
  @PatchMapping
  public ResponseEntity<AlertSettingsResponseDto> updateAlertSettings(
      @RequestBody @Valid AlertSettingsRequestDto alertSettingsRequestDto,
      @RequestParam Long userId
//      , @AuthenticationPrincipal
  ) throws Exception {

    AlertSettingsResponseDto alertSettingsResponseDto =
        productAlertService.updateAlertSettings(userId, alertSettingsRequestDto);
    return ResponseEntity.ok(alertSettingsResponseDto);
  }
}
