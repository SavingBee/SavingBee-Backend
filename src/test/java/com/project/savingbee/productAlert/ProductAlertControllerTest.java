package com.project.savingbee.productAlert;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.savingbee.common.entity.ProductAlertSetting.AlertType;
import com.project.savingbee.productAlert.controller.ProductAlertController;
import com.project.savingbee.productAlert.dto.AlertSettingsRequestDto;
import com.project.savingbee.productAlert.dto.AlertSettingsResponseDto;
import com.project.savingbee.productAlert.service.ProductAlertService;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductAlertController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductAlertControllerTest {
  @MockitoBean
  private ProductAlertService productAlertService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("현재 사용자 알림 설정 조회 성공")
  void successGetAlertSettings() throws Exception {
      //given
    AlertSettingsResponseDto alertSettingsResponseDto = AlertSettingsResponseDto.builder()
        .userId(3L)
        .alertType(AlertType.SMS)
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

    given(productAlertService.getAlertSettings(anyLong())).willReturn(alertSettingsResponseDto);

      //when
      //then
    mockMvc.perform(get("/api/alerts/settings?userId=3"))
        .andDo(print())
        .andExpect(jsonPath("$.userId").value(3))
        .andExpect(jsonPath("$.alertType").value("SMS"))
        .andExpect(jsonPath("$.productTypeDeposit").value(true))
        .andExpect(jsonPath("$.productTypeSaving").value(false))
        .andExpect(jsonPath("$.minInterestRate").value(BigDecimal.valueOf(2.80)))
        .andExpect(jsonPath("$.interestCalcSimple").value(false))
        .andExpect(jsonPath("$.interestCalcCompound").value(true))
        .andExpect(jsonPath("$.maxSaveTerm").value(24))
        .andExpect(jsonPath("$.minAmount", nullValue()))
        .andExpect(jsonPath("$.maxLimit", nullValue()))
        .andExpect(jsonPath("$.rsrvTypeFlexible").value(false))
        .andExpect(jsonPath("$.rsrvTypeFixed").value(false))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.updatedAt", nullValue()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("알림 조건 설정 성공")
  void successCreateAlertSettings() throws Exception {
      //given
    AlertSettingsRequestDto alertSettingsRequestDto = AlertSettingsRequestDto.builder()
        .alertType(AlertType.PUSH)
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

    AlertSettingsResponseDto alertSettingsResponseDto = AlertSettingsResponseDto.builder()
        .userId(3L)
        .alertType(AlertType.PUSH)
        .productTypeDeposit(true)
        .productTypeSaving(false)
        .minInterestRate(BigDecimal.valueOf(3.00))
        .interestCalcSimple(false)
        .interestCalcCompound(false)
        .maxSaveTerm(12)
        .minAmount(BigInteger.valueOf(1000000))
        .maxLimit(null)
        .rsrvTypeFlexible(false)
        .rsrvTypeFixed(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(null)
        .build();

    given(productAlertService.createAlertSettings(anyLong(), any(AlertSettingsRequestDto.class)))
        .willReturn(alertSettingsResponseDto);

      //when
      //then
    mockMvc.perform(post("/api/alerts/settings?userId=3")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(alertSettingsRequestDto)))
        .andDo(print())
        .andExpect(jsonPath("$.userId").value(3))
        .andExpect(jsonPath("$.alertType").value("PUSH"))
        .andExpect(jsonPath("$.productTypeDeposit").value(true))
        .andExpect(jsonPath("$.productTypeSaving").value(false))
        .andExpect(jsonPath("$.minInterestRate").value(BigDecimal.valueOf(3.00)))
        .andExpect(jsonPath("$.interestCalcSimple").value(false))
        .andExpect(jsonPath("$.interestCalcCompound").value(false))
        .andExpect(jsonPath("$.maxSaveTerm").value(12))
        .andExpect(jsonPath("$.minAmount").value(BigInteger.valueOf(1000000)))
        .andExpect(jsonPath("$.maxLimit", nullValue()))
        .andExpect(jsonPath("$.rsrvTypeFlexible").value(false))
        .andExpect(jsonPath("$.rsrvTypeFixed").value(true))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.updatedAt", nullValue()))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("알림 조건 설정 실패")
  void failCreateAlertSettings() throws Exception {
    //given
    AlertSettingsRequestDto alertSettingsRequestDto = AlertSettingsRequestDto.builder()
        .alertType(AlertType.PUSH)
        .minInterestRate(BigDecimal.valueOf(-1.00)) // 범위 위반
        .build();

    //when
    //then
    mockMvc.perform(post("/api/alerts/settings?userId=3")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(alertSettingsRequestDto)))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("알림 조건 수정 성공")
  void successUpdateAlertSettings() throws Exception {
      //given
    AlertSettingsRequestDto alertSettingsRequestDto = AlertSettingsRequestDto.builder()
        .alertType(AlertType.EMAIL)
        .minInterestRate(BigDecimal.valueOf(2.80))
        .maxSaveTerm(6)
        .minAmount(BigInteger.valueOf(500000))
        .build();

    AlertSettingsResponseDto alertSettingsResponseDto = AlertSettingsResponseDto.builder()
        .userId(3L)
        .alertType(AlertType.EMAIL)
        .productTypeDeposit(true)
        .productTypeSaving(false)
        .minInterestRate(BigDecimal.valueOf(2.80))
        .interestCalcSimple(false)
        .interestCalcCompound(false)
        .maxSaveTerm(6)
        .minAmount(BigInteger.valueOf(500000))
        .maxLimit(null)
        .rsrvTypeFlexible(false)
        .rsrvTypeFixed(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    given(productAlertService.updateAlertSettings(anyLong(), any(AlertSettingsRequestDto.class)))
        .willReturn(alertSettingsResponseDto);

    //when
    //then
    mockMvc.perform(patch("/api/alerts/settings?userId=3")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(alertSettingsRequestDto)))
        .andDo(print())
        .andExpect(jsonPath("$.userId").value(3))
        .andExpect(jsonPath("$.alertType").value("EMAIL"))
        .andExpect(jsonPath("$.productTypeDeposit").value(true))
        .andExpect(jsonPath("$.productTypeSaving").value(false))
        .andExpect(jsonPath("$.minInterestRate").value(BigDecimal.valueOf(2.80)))
        .andExpect(jsonPath("$.interestCalcSimple").value(false))
        .andExpect(jsonPath("$.interestCalcCompound").value(false))
        .andExpect(jsonPath("$.maxSaveTerm").value(6))
        .andExpect(jsonPath("$.minAmount").value(BigInteger.valueOf(500000)))
        .andExpect(jsonPath("$.maxLimit", nullValue()))
        .andExpect(jsonPath("$.rsrvTypeFlexible").value(false))
        .andExpect(jsonPath("$.rsrvTypeFixed").value(true))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.updatedAt", notNullValue()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("알림 조건 수정 실패")
  void failUpdateAlertSettings() throws Exception {
    //given
    AlertSettingsRequestDto alertSettingsRequestDto = AlertSettingsRequestDto.builder()
        .minAmount(BigInteger.valueOf(-1))  // 범위 위반
        .build();

    //when
    //then
    mockMvc.perform(patch("/api/alerts/settings?userId=3")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(alertSettingsRequestDto)))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }
}
