package com.project.savingbee.domain.user.service;

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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceLoadUserTests { //loadUserByUsername 성공/실패

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder; // @InjectMocks 주입 맞추려고 둠

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("loadUserByUsername: 유저가 존재하면 UserDetails를 반환한다")
    void loadUser_success() {
        // given
        UserEntity entity = UserEntity.builder()
                .username("kim")
                .password("ENC_PW")
                .isLock(false)
                .isSocial(false)
                .roleType(UserRoleType.USER)
                .build();

        given(userRepository.findByUsernameAndIsLockAndIsSocial("kim", false, false))
                .willReturn(java.util.Optional.of(entity));

        // when
        var details = userService.loadUserByUsername("kim");

        // then
        assertThat(details.getUsername()).isEqualTo("kim");
        assertThat(details.getPassword()).isEqualTo("ENC_PW");
        // Spring Security의 roles()는 "ROLE_USER" 형태로 권한 부여됨
        assertThat(details.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .toList())
                .containsExactly("ROLE_USER");

        assertThat(details.isAccountNonLocked()).isTrue(); // isLock(false) → nonLocked(true)
    }

    @Test
    @DisplayName("loadUserByUsername: 유저가 없으면 UsernameNotFoundException")
    void loadUser_notFound_throws() {
        // given
        given(userRepository.findByUsernameAndIsLockAndIsSocial("nope", false, false))
                .willReturn(java.util.Optional.empty());

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        userService.loadUserByUsername("nope"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
                .hasMessageContaining("nope");
    }
}
