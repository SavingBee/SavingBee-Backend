package com.project.savingbee.domain.user.service;

import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.entity.UserEntity;
import com.project.savingbee.domain.user.repository.PasswordResetTokenRepository;
import com.project.savingbee.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceFindPasswordTests { //비밀번호 찾기 → 인증 코드 발송

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("비밀번호 찾기 성공 시 인증 코드 발송")
    void findPassword_success() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");
        dto.setEmail("kim@test.com");

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername("kim");
        userEntity.setEmail("kim@test.com");

        when(userRepository.findByUsernameAndEmailAndIsSocial("kim", "kim@test.com", false))
                .thenReturn(Optional.of(userEntity));

        userService.findPassword(dto);

        verify(passwordResetTokenRepository).deleteByUsernameAndEmail("kim", "kim@test.com");
        verify(passwordResetTokenRepository).save(any());
        verify(emailService).sendVerificationCodeEmail(eq("kim@test.com"), eq("kim"), anyString());
    }

    @Test
    @DisplayName("일치하는 계정이 없으면 예외 발생")
    void findPassword_userNotFound() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");
        dto.setEmail("wrong@test.com");

        when(userRepository.findByUsernameAndEmailAndIsSocial(any(), any(), eq(false)))
                .thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> userService.findPassword(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
