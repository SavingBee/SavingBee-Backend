package com.project.savingbee.domain.user.service;

import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceExistUserTests { //존재 여부

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    @Test
    @DisplayName("existUser: username이 존재하면 true를 반환한다")
    void existUser_true() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");
        given(userRepository.existsByUsername("kim")).willReturn(true);

        Boolean result = userService.existUser(dto);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existUser: username이 존재하지 않으면 false를 반환한다")
    void existUser_false() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("lee");
        given(userRepository.existsByUsername("lee")).willReturn(false);

        Boolean result = userService.existUser(dto);

        assertThat(result).isFalse();
    }
}
