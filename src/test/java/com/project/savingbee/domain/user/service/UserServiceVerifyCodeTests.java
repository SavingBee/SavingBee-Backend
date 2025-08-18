package com.project.savingbee.domain.user.service;

import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.project.savingbee.domain.user.entity.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceVerifyCodeTests { //인증 코드 검증

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("인증 코드가 유효하면 true 반환")
    void verifyCode_success() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");
        dto.setVerificationCode("123456");

        // PasswordResetToken은 entity 패키지에 있음
        PasswordResetToken token = PasswordResetToken.builder()
                .username("kim")
                .verificationCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        when(passwordResetTokenRepository.findByUsernameAndVerificationCodeAndIsUsedFalseAndExpiresAtAfter(
                eq("kim"), eq("123456"), any()
        )).thenReturn(Optional.of(token));

        boolean result = userService.verifyCode(dto);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("인증 코드가 없으면 false 반환")
    void verifyCode_fail() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");
        dto.setVerificationCode("999999");

        when(passwordResetTokenRepository.findByUsernameAndVerificationCodeAndIsUsedFalseAndExpiresAtAfter(
                any(), any(), any()))
                .thenReturn(Optional.empty());

        boolean result = userService.verifyCode(dto);

        org.assertj.core.api.Assertions.assertThat(result).isFalse();
    }
}
