package com.project.savingbee.domain.user.service;

import com.project.savingbee.domain.jwt.service.JwtService;
import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.entity.PasswordResetToken;
import com.project.savingbee.domain.user.entity.UserEntity;
import com.project.savingbee.domain.user.repository.PasswordResetTokenRepository;
import com.project.savingbee.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceSetNewPasswordTests { //새 비밀번호 설정

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;          // UserService 생성자 충족용
    @Mock EmailService emailService;      // UserService 생성자 충족용

    @InjectMocks UserService userService;

    @Test
    @DisplayName("setNewPassword: 인증 성공 시 비밀번호 변경 및 토큰 사용 처리")
    void setNewPassword_success() {
        // given
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");
        dto.setVerificationCode("123456");
        dto.setPassword("newPass");
        dto.setPasswordConfirm("newPass");

        PasswordResetToken token = PasswordResetToken.builder()
                .username("kim")
                .email("k@k.com")
                .verificationCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        UserEntity user = UserEntity.builder()
                .username("kim")
                .build();

        given(passwordResetTokenRepository
                .findByUsernameAndVerificationCodeAndIsUsedFalseAndExpiresAtAfter(
                        eq("kim"), eq("123456"), any(LocalDateTime.class)))
                .willReturn(Optional.of(token));

        given(userRepository.findByUsernameAndIsSocial("kim", false))
                .willReturn(Optional.of(user));

        given(passwordEncoder.encode("newPass")).willReturn("encodedPass");

        // when
        userService.setNewPassword(dto);

        // then
        assertThat(user.getPassword()).isEqualTo("encodedPass");
        assertThat(token.getIsUsed()).isTrue();
        then(userRepository).should().save(user);
        then(passwordResetTokenRepository).should().save(token);
    }

    @Test
    @DisplayName("setNewPassword: 비밀번호 확인 불일치 시 예외 발생")
    void setNewPassword_passwordMismatch() {
        // given
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");
        dto.setVerificationCode("123456");
        dto.setPassword("newPass");
        dto.setPasswordConfirm("wrongPass");

        PasswordResetToken token = PasswordResetToken.builder()
                .username("kim")
                .verificationCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        given(passwordResetTokenRepository
                .findByUsernameAndVerificationCodeAndIsUsedFalseAndExpiresAtAfter(
                        eq("kim"), eq("123456"), any(LocalDateTime.class)))
                .willReturn(Optional.of(token));

        // when & then
        assertThatThrownBy(() -> userService.setNewPassword(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다.");

        // 비밀번호 불일치이므로, 유저 조회/저장은 호출되지 않는 게 자연스럽습니다(선택적 검증)
        then(userRepository).should(never()).findByUsernameAndIsSocial(anyString(), anyBoolean());
        then(userRepository).should(never()).save(any(UserEntity.class));
        then(passwordEncoder).should(never()).encode(anyString());
        // 토큰 save도 일어나지 않음
        then(passwordResetTokenRepository).should(never()).save(any(PasswordResetToken.class));
    }
}
