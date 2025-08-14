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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("아이디가 이미 존재하면 true를 반환한다")
    void existUser_true() {
        // given
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim"); // DTO에 맞춰 셋터/생성자 사용
        given(userRepository.existsByUsername("kim")).willReturn(true);

        // when
        Boolean result = userService.existUser(dto);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("아이디가 존재하지 않으면 false를 반환한다")
    void existUser_false() {
        // given
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("lee");
        given(userRepository.existsByUsername("lee")).willReturn(false);

        // when
        Boolean result = userService.existUser(dto);

        // then
        assertThat(result).isFalse();
    }
}
