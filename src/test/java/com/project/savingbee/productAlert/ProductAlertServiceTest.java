package com.project.savingbee.productAlert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.project.savingbee.common.entity.ProductAlertSetting;
import com.project.savingbee.common.entity.ProductAlertSetting.AlertType;
import com.project.savingbee.common.repository.ProductAlertSettingRepository;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductAlertServiceTest {
  @Mock
  ProductAlertSettingRepository productAlertSettingRepository;

  @InjectMocks
  ProductAlertService productAlertService;

  @Test
  @DisplayName("현재 사용자 알림 설정 조회 성공")
  void successGetAlertSettings() {
      //given
    long userId = 3L;

    ProductAlertSetting productAlertSetting = ProductAlertSetting.builder()
        .userId(userId)
        .alertType(AlertType.EMAIL)
        .productTypeDeposit(true)
        .productTypeSaving(false)
        .minInterestRate(BigDecimal.valueOf(2.80))
        .interestCalcSimple(false)
        .interestCalcCompound(true)
        .maxSaveTerm(24)
        .minAmount(null)
        .maxLimit(null)
        .rsrvTypeFlexible(false)
        .rsrvTypeFixed(false)
        .createdAt(LocalDateTime.now())
        .updatedAt(null)
        .build();

    given(productAlertSettingRepository.findByUserId(userId)).willReturn(Optional.of(productAlertSetting));

      //when
    AlertSettingsResponseDto alertSettingsResponseDto = productAlertService.getAlertSettings(userId);

      //then
    verify(productAlertSettingRepository, times(1)).findByUserId(userId);

    assertThat(alertSettingsResponseDto.getUserId()).isEqualTo(userId);
    assertThat(alertSettingsResponseDto.getAlertType()).isEqualTo(AlertType.EMAIL);
    assertThat(alertSettingsResponseDto.isProductTypeDeposit()).isTrue();
    assertThat(alertSettingsResponseDto.isProductTypeSaving()).isFalse();
    assertThat(alertSettingsResponseDto.getMinInterestRate()).isEqualByComparingTo("2.80");
    assertThat(alertSettingsResponseDto.isInterestCalcSimple()).isFalse();
    assertThat(alertSettingsResponseDto.isInterestCalcCompound()).isTrue();
    assertThat(alertSettingsResponseDto.getMaxSaveTerm()).isEqualTo(24);
    assertThat(alertSettingsResponseDto.isRsrvTypeFlexible()).isFalse();
    assertThat(alertSettingsResponseDto.isRsrvTypeFixed()).isFalse();
  }

  @Test
  @DisplayName("알림 설정 첫 조회 시 기본값 반환")
  void successGetDefaultAlertSettings() {
      //given
    long userId = 3L;

    given(productAlertSettingRepository.findByUserId(userId)).willReturn(Optional.empty());

      //when
    AlertSettingsResponseDto alertSettingsResponseDto = productAlertService.getAlertSettings(userId);

      //then
    verify(productAlertSettingRepository, times(1)).findByUserId(userId);

    assertThat(alertSettingsResponseDto.getUserId()).isEqualTo(userId);
    assertThat(alertSettingsResponseDto.getAlertType()).isEqualTo(AlertType.EMAIL);
    assertThat(alertSettingsResponseDto.isProductTypeDeposit()).isFalse();
    assertThat(alertSettingsResponseDto.isProductTypeSaving()).isFalse();
    assertThat(alertSettingsResponseDto.getMinInterestRate()).isNull();
    assertThat(alertSettingsResponseDto.isInterestCalcSimple()).isFalse();
    assertThat(alertSettingsResponseDto.isInterestCalcCompound()).isFalse();
    assertThat(alertSettingsResponseDto.getMaxSaveTerm()).isEqualTo(1);
    assertThat(alertSettingsResponseDto.getMinAmount()).isNull();
    assertThat(alertSettingsResponseDto.getMaxLimit()).isNull();
    assertThat(alertSettingsResponseDto.isRsrvTypeFlexible()).isFalse();
    assertThat(alertSettingsResponseDto.isRsrvTypeFixed()).isFalse();
    assertThat(alertSettingsResponseDto.getCreatedAt()).isNull();
    assertThat(alertSettingsResponseDto.getUpdatedAt()).isNull();
  }

  @Test
  @DisplayName("알림 조건 설정 성공")
  void successCreateAlertSettings() throws BadRequestException {
      //given
    long userId = 3L;

    AlertSettingsRequestDto alertSettingsRequestDto = AlertSettingsRequestDto.builder()
        .alertType(AlertType.SMS)
        .productTypeDeposit(true)
        .productTypeSaving(false)
        .minInterestRate(BigDecimal.valueOf(3.00))
        .interestCalcSimple(null)
        .interestCalcCompound(null)
        .maxSaveTerm(12)
        .minAmount(BigInteger.valueOf(1000000))
        .maxLimit(null)
        .rsrvTypeFlexible(false)
        .rsrvTypeFixed(true)
        .build();

    given(productAlertSettingRepository.save(any(ProductAlertSetting.class)))
        .willAnswer(inv -> inv.getArgument(0));

      //when
    AlertSettingsResponseDto alertSettingsResponseDto =
        productAlertService.createAlertSettings(userId, alertSettingsRequestDto);

      //then
    ArgumentCaptor<ProductAlertSetting> captor = ArgumentCaptor.forClass(ProductAlertSetting.class);
    verify(productAlertSettingRepository, times(1)).save(captor.capture());

    assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    assertThat(captor.getValue().getAlertType()).isEqualTo(AlertType.SMS);
    assertThat(captor.getValue().getProductTypeDeposit()).isTrue();
    assertThat(captor.getValue().getProductTypeSaving()).isFalse();
    assertThat(captor.getValue().getMinInterestRate()).isEqualTo(BigDecimal.valueOf(3.00));
    assertThat(captor.getValue().getInterestCalcSimple()).isFalse();
    assertThat(captor.getValue().getInterestCalcCompound()).isFalse();
    assertThat(captor.getValue().getMaxSaveTerm()).isEqualTo(12);
    assertThat(captor.getValue().getMinAmount()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(captor.getValue().getMaxLimit()).isNull();
    assertThat(captor.getValue().getRsrvTypeFlexible()).isFalse();
    assertThat(captor.getValue().getRsrvTypeFixed()).isTrue();

    // 저장값과 일치하는 지 확인용(일부)
    assertThat(alertSettingsResponseDto.getUserId()).isEqualTo(captor.getValue().getUserId());
    assertThat(alertSettingsResponseDto.getAlertType()).isEqualTo(captor.getValue().getAlertType());
    assertThat(alertSettingsResponseDto.isProductTypeDeposit()).isEqualTo(captor.getValue().getProductTypeDeposit());
    assertThat(alertSettingsResponseDto.getMaxSaveTerm()).isEqualTo(captor.getValue().getMaxSaveTerm());
  }

  @Test
  @DisplayName("알림 조건 설정 실패")
  void failCreateAlertSettings() throws BadRequestException {
    //given
    long userId = 3L;

    AlertSettingsRequestDto alertSettingsRequestDto = AlertSettingsRequestDto.builder()
        .alertType(AlertType.SMS)
        .minInterestRate(BigDecimal.valueOf(-1.00)) // 범위 위반
        .build();

    //when
    //then
    assertThatThrownBy(() -> productAlertService.createAlertSettings(userId, alertSettingsRequestDto))
        .isInstanceOf(BadRequestException.class);

    verify(productAlertSettingRepository, times(0)).save(any());
  }

  @Test
  @DisplayName("알림 조건 수정 성공")
  void successUpdateAlertSettings() throws Exception {
      //given
    long userId = 3L;

    // 기존 저장된 설정
    ProductAlertSetting productAlertSetting = ProductAlertSetting.builder()
        .userId(userId)
        .alertType(AlertType.EMAIL)
        .productTypeDeposit(true)
        .productTypeSaving(false)
        .minInterestRate(BigDecimal.valueOf(2.80))
        .interestCalcSimple(false)
        .interestCalcCompound(true)
        .maxSaveTerm(24)
        .minAmount(null)
        .maxLimit(null)
        .rsrvTypeFlexible(false)
        .rsrvTypeFixed(false)
        .createdAt(LocalDateTime.now())
        .updatedAt(null)
        .build();

    AlertSettingsRequestDto alertSettingsRequestDto = AlertSettingsRequestDto.builder()
        .alertType(AlertType.PUSH)
        .productTypeDeposit(false)
        .productTypeSaving(true)
        .minInterestRate(BigDecimal.valueOf(3.00))
        .maxSaveTerm(12)
        .minAmount(BigInteger.valueOf(1000000))
        .build();

    given(productAlertSettingRepository.findByUserId(userId))
        .willReturn(Optional.of(productAlertSetting));

    given(productAlertSettingRepository.save(any(ProductAlertSetting.class)))
        .willAnswer(inv -> inv.getArgument(0));

      //when
    AlertSettingsResponseDto alertSettingsResponseDto =
        productAlertService.updateAlertSettings(userId, alertSettingsRequestDto);

      //then
    ArgumentCaptor<ProductAlertSetting> captor = ArgumentCaptor.forClass(ProductAlertSetting.class);
    verify(productAlertSettingRepository, times(1)).findByUserId(userId);
    verify(productAlertSettingRepository, times(1)).save(captor.capture());

    assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    assertThat(captor.getValue().getAlertType()).isEqualTo(AlertType.PUSH);
    assertThat(captor.getValue().getProductTypeDeposit()).isFalse();
    assertThat(captor.getValue().getProductTypeSaving()).isTrue();
    assertThat(captor.getValue().getMinInterestRate()).isEqualTo(BigDecimal.valueOf(3.00));
    assertThat(captor.getValue().getInterestCalcSimple()).isFalse();
    assertThat(captor.getValue().getInterestCalcCompound()).isTrue();
    assertThat(captor.getValue().getMaxSaveTerm()).isEqualTo(12);
    assertThat(captor.getValue().getMinAmount()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(captor.getValue().getMaxLimit()).isNull();
    assertThat(captor.getValue().getRsrvTypeFlexible()).isFalse();
    assertThat(captor.getValue().getRsrvTypeFixed()).isFalse();

    // 저장값과 일치하는 지 확인용(일부)
    assertThat(alertSettingsResponseDto.getUserId()).isEqualTo(captor.getValue().getUserId());
    assertThat(alertSettingsResponseDto.getAlertType()).isEqualTo(captor.getValue().getAlertType());
    assertThat(alertSettingsResponseDto.getMaxSaveTerm()).isEqualTo(captor.getValue().getMaxSaveTerm());
  }

  @Test
  @DisplayName("알림 조건 수정 실패")
  void failUpdateAlertSettings() throws Exception {
    //given
    long userId = 3L;

    // 기존 저장된 설정
    ProductAlertSetting productAlertSetting = ProductAlertSetting.builder()
        .userId(userId)
        .build();

    AlertSettingsRequestDto alertSettingsRequestDto = AlertSettingsRequestDto.builder()
        .minAmount(BigInteger.valueOf(1000000))
        .maxLimit(BigInteger.valueOf(100000))   // 범위 위반(minAmount > maxLimit)
        .build();

    //when
    //then
    assertThatThrownBy(() -> productAlertService.updateAlertSettings(userId, alertSettingsRequestDto))
        .isInstanceOf(BadRequestException.class);

    verify(productAlertSettingRepository, times(0)).save(any());
  }
}
