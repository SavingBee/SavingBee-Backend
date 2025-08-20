package com.project.savingbee.domain.user.service;

import com.project.savingbee.domain.jwt.service.JwtService;
import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.entity.UserEntity;
import com.project.savingbee.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserServiceFindUsernameTests { //아이디 찾기 테스트

    @Mock PasswordEncoder passwordEncoder;
    @Mock UserRepository userRepository;
    @Mock
    JwtService jwtService;
    @Mock EmailService emailService; // 👈 이메일 전송 서비스 Mock

    @InjectMocks UserService userService;

    @Test
    @DisplayName("findUsername: 이메일로 가입된 계정이 없으면 IllegalArgumentException")
    void findUsername_notFound() {
        // given
        UserRequestDTO dto = new UserRequestDTO();
        dto.setEmail("no@no.com");
        given(userRepository.findByEmailAndIsSocial("no@no.com", false))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findUsername(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 이메일로 가입된 계정이 없습니다.");
        then(emailService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("findUsername: 존재하면 username을 이메일로 발송한다")
    void findUsername_success() {
        // given
        UserRequestDTO dto = new UserRequestDTO();
        dto.setEmail("kim@example.com");

        UserEntity entity = UserEntity.builder()
                .id(1L).username("kim").email("kim@example.com").isSocial(false)
                .build();

        given(userRepository.findByEmailAndIsSocial("kim@example.com", false))
                .willReturn(Optional.of(entity));

        // when
        userService.findUsername(dto);

        // then
        then(emailService).should()
                .sendUsernameEmail("kim@example.com", "kim");
    }
}
