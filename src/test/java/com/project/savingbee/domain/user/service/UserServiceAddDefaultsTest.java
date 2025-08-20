package com.project.savingbee.domain.user.service;

import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.entity.UserEntity;
import com.project.savingbee.domain.user.entity.UserRoleType;
import com.project.savingbee.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserServiceAddDefaultsTest { //addUser 디폴트 필드 검증

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("addUser: 디폴트 플래그와 롤이 올바르게 설정된다")
    void addUser_setsDefaults() throws Exception {
        // given
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("park");
        dto.setPassword("pw");
        dto.setNickname("박닉");
        dto.setEmail("park@example.com");

        given(userRepository.existsByUsername("park")).willReturn(false);
        given(passwordEncoder.encode("pw")).willReturn("ENC");

        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            var id = UserEntity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(u, 7L);
            return u;
        });

        // when
        Long id = userService.addUser(dto);

        // then
        assertThat(id).isEqualTo(7L);

        var captor = org.mockito.ArgumentCaptor.forClass(UserEntity.class);
        then(userRepository).should().save(captor.capture());
        UserEntity saved = captor.getValue();

        assertThat(saved.getIsLock()).isFalse();
        assertThat(saved.getIsSocial()).isFalse();
        assertThat(saved.getRoleType()).isEqualTo(UserRoleType.USER);
        assertThat(saved.getPassword()).isEqualTo("ENC");
    }
}
