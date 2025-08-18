package com.project.savingbee.domain.user.service;

import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceAddUserValidationTests { // 회원가입 검증(중복/비밀번호 불일치)

    @Mock PasswordEncoder passwordEncoder;
    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    @Test
    @DisplayName("addUser: 이미 유저가 존재하면 IllegalArgumentException")
    void addUser_duplicatedUsername_throws() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");
        dto.setPassword("pw");
        dto.setPasswordConfirm("pw");
        dto.setNickname("닉");
        dto.setEmail("kim@example.com");

        given(userRepository.existsByUsername("kim")).willReturn(true);

        assertThatThrownBy(() -> userService.addUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 유저가 존재합니다.");
    }

    @Test
    @DisplayName("addUser: 비밀번호/확인 불일치면 IllegalArgumentException")
    void addUser_passwordMismatch_throws() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");
        dto.setPassword("pw1");
        dto.setPasswordConfirm("pw2");
        dto.setNickname("닉");
        dto.setEmail("kim@example.com");

        given(userRepository.existsByUsername("kim")).willReturn(false);

        assertThatThrownBy(() -> userService.addUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다.");
    }
}
