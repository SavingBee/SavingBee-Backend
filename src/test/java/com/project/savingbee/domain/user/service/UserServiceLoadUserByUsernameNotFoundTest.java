package com.project.savingbee.domain.user.service;

import com.project.savingbee.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceLoadUserByUsernameNotFoundTest { //로그인 (자체) not found

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    @Test
    @DisplayName("loadUserByUsername: 미존재/잠김/소셜이면 UsernameNotFoundException")
    void loadUserByUsername_notFound() {
        given(userRepository.findByUsernameAndIsLockAndIsSocial("nope", false, false))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("nope"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("nope");
    }
}
