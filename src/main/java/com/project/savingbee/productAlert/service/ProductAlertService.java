package com.project.savingbee.productAlert.service;

import com.project.savingbee.common.entity.ProductAlertSetting;
import com.project.savingbee.common.repository.ProductAlertSettingRepository;
import com.project.savingbee.productAlert.dto.AlertSettingsRequestDto;
import com.project.savingbee.productAlert.dto.AlertSettingsResponseDto;
import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProductAlertService {
  private final ProductAlertSettingRepository productAlertSettingRepository;

  // 기존에 알림 설정된 조건 가져오기
  @Transactional(readOnly = true)
  public AlertSettingsResponseDto getAlertSettings(Long userId) {
    return productAlertSettingRepository.findByUserId(userId)
        .map(AlertSettingsResponseDto::new)
        .orElseGet(() -> AlertSettingsResponseDto.defaultDto(userId));  // 기존 설정 없을 경우 기본값 반환
  }

  // 상품 알림 조건 설정
  @Transactional
  public AlertSettingsResponseDto createAlertSettings(
      Long userId, AlertSettingsRequestDto alertSettingsRequestDto) throws Exception {

    // 알림은 사용자 당 하나만 설정 가능
    if (productAlertSettingRepository.findByUserId(userId).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "alert setting is already exists!");
    }

    validate(alertSettingsRequestDto);

    ProductAlertSetting productAlertSetting = ProductAlertSetting.builder()
        .userId(userId)
        .alertType(alertSettingsRequestDto.getAlertType())
        .productTypeDeposit(Boolean.TRUE.equals(alertSettingsRequestDto.getProductTypeDeposit()))
        .productTypeSaving(Boolean.TRUE.equals(alertSettingsRequestDto.getProductTypeSaving()))
        .minInterestRate(alertSettingsRequestDto.getMinInterestRate())
        .interestCalcSimple(Boolean.TRUE.equals(alertSettingsRequestDto.getInterestCalcSimple()))
        .interestCalcCompound(Boolean.TRUE.equals(alertSettingsRequestDto.getInterestCalcCompound()))
        .maxSaveTerm(alertSettingsRequestDto.getMaxSaveTerm())
        .minAmount(alertSettingsRequestDto.getMinAmount())
        .maxLimit(alertSettingsRequestDto.getMaxLimit())
        .rsrvTypeFlexible(Boolean.TRUE.equals(alertSettingsRequestDto.getRsrvTypeFlexible()))
        .rsrvTypeFixed(Boolean.TRUE.equals(alertSettingsRequestDto.getRsrvTypeFixed()))
        .build();

    return new AlertSettingsResponseDto(productAlertSettingRepository.save(productAlertSetting));
  }

  // 상품 알림 조건 수정
  @Transactional
  public AlertSettingsResponseDto updateAlertSettings(
      Long userId, AlertSettingsRequestDto alertSettingsRequestDto)
      throws Exception {

    validate(alertSettingsRequestDto);

    ProductAlertSetting productAlertSetting = productAlertSettingRepository.findByUserId(userId)
        .orElseThrow(NotFoundException::new);

    // 필수 입력값
    productAlertSetting.setAlertType(alertSettingsRequestDto.getAlertType());
    productAlertSetting.setMaxSaveTerm(alertSettingsRequestDto.getMaxSaveTerm());

    // 변경 사항이 있을 경우에만 수정
    if (alertSettingsRequestDto.getProductTypeDeposit() != null) {
      productAlertSetting.setProductTypeDeposit(alertSettingsRequestDto.getProductTypeDeposit()); }
    if (alertSettingsRequestDto.getProductTypeSaving() != null) {
      productAlertSetting.setProductTypeSaving(alertSettingsRequestDto.getProductTypeSaving()); }
    if (alertSettingsRequestDto.getMinInterestRate() != null) {
      productAlertSetting.setMinInterestRate(alertSettingsRequestDto.getMinInterestRate()); }
    if (alertSettingsRequestDto.getInterestCalcSimple() != null) {
      productAlertSetting.setInterestCalcSimple(alertSettingsRequestDto.getInterestCalcSimple()); }
    if (alertSettingsRequestDto.getInterestCalcCompound() != null) {
      productAlertSetting.setInterestCalcCompound(alertSettingsRequestDto.getInterestCalcCompound()); }
    if (alertSettingsRequestDto.getMinAmount() != null) {
      productAlertSetting.setMinAmount(alertSettingsRequestDto.getMinAmount()); }
    if (alertSettingsRequestDto.getMaxLimit() != null) {
      productAlertSetting.setMaxLimit(alertSettingsRequestDto.getMaxLimit()); }
    if (alertSettingsRequestDto.getRsrvTypeFlexible() != null) {
      productAlertSetting.setRsrvTypeFlexible(alertSettingsRequestDto.getRsrvTypeFlexible()); }
    if (alertSettingsRequestDto.getRsrvTypeFixed() != null) {
      productAlertSetting.setRsrvTypeFixed(alertSettingsRequestDto.getRsrvTypeFixed()); }

    return new AlertSettingsResponseDto(productAlertSettingRepository.save(productAlertSetting));
  }

  // 유효성 검증
  private void validate(AlertSettingsRequestDto dto)
      throws BadRequestException {
    if (dto.getAlertType() == null) {
      throw new BadRequestException("alertType is required");
    }

    if (dto.getMinInterestRate() != null) {
      if (dto.getMinInterestRate().compareTo(BigDecimal.ZERO) < 0) {
        throw new BadRequestException("minInterestRate must be >= 0");
      }
    }

    if (dto.getMinAmount() != null) {
      if (dto.getMinAmount().compareTo(BigInteger.ZERO) < 0) {
        throw new BadRequestException("minAmount must be >= 0");
      }
    }

    if (dto.getMaxLimit() != null) {
      if (dto.getMaxLimit().compareTo(BigInteger.ZERO) < 0) {
        throw new BadRequestException("maxLimit must be >= 0");
      }
    }

    if (dto.getMinAmount() != null && dto.getMaxLimit() != null) {
      if (dto.getMinAmount().compareTo(dto.getMaxLimit()) > 0) {
        throw new BadRequestException("minAmount must be <= maxLimit");
      }
    }
  }
}
