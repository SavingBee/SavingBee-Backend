package com.project.savingbee.domain.user.service;

import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.entity.User;
import com.project.savingbee.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceAddUserTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("아이디가 이미 존재하면 IllegalArgumentException을 던진다")
    void addUser_duplicateUsername_throws() {
        // given
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");

        given(userRepository.existsByUsername("kim")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.addUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 유저가 존재합니다");

        // 그리고 저장(save)은 호출되지 않아야 함
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("정상 가입: 비밀번호는 인코딩되어 저장되고, 저장된 ID를 반환한다")
    void addUser_success_returnsId_and_encodesPassword() throws Exception {
        // given
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("lee");
        dto.setPassword("plainPw");
        dto.setNickname("닉네임");
        dto.setEmail("lee@example.com");

        given(userRepository.existsByUsername("lee")).willReturn(false);
        given(passwordEncoder.encode("plainPw")).willReturn("ENCODED_PW");

        // save가 호출되면, 저장된 엔티티에 id가 채워졌다고 가정하고 반환
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User u = invocation.getArgument(0);
            // id 필드에 1L을 주입(엔티티 id가 private일 수 있으니 리플렉션 사용)
            Field id = User.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(u, 1L);
            return u;
        });

        // when
        Long savedId = userService.addUser(dto);

        // then
        assertThat(savedId).isEqualTo(1L);

        // 저장 호출 시 넘긴 엔티티 값들도 검증(비밀번호가 인코딩되었는지 등)
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(captor.capture());
        User savedEntity = captor.getValue();

        assertThat(savedEntity.getUsername()).isEqualTo("lee");
        assertThat(savedEntity.getPassword()).isEqualTo("ENCODED_PW");
        assertThat(savedEntity.getEmail()).isEqualTo("lee@example.com");
        assertThat(savedEntity.getNickname()).isEqualTo("닉네임");

        // 인코더가 호출되었는지 추가 확인
        then(passwordEncoder).should().encode("plainPw");
    }
}