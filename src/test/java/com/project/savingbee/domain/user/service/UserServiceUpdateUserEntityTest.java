package com.project.savingbee.domain.user.service;

import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.entity.UserEntity;
import com.project.savingbee.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.lang.reflect.Field;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceUpdateUserEntityTest {

    @Mock
    UserRepository userRepository;

    @Mock
    SecurityContext securityContext;
    @Mock
    Authentication authentication;

    @InjectMocks
    UserService userService;

    @AfterEach
    void tearDown() {
        // 테스트 사이에 SecurityContext 누수 방지
        SecurityContextHolder.clearContext();
    }

    private void mockSessionUsername(String username) {
        SecurityContextHolder.setContext(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn(username);
    }

    @Test
    @DisplayName("본인이 아니면 AccessDeniedException 발생")
    void updateUser_notOwner_throwsAccessDenied() {
        // given
        mockSessionUsername("other"); // 세션 사용자
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");       // 수정 대상 사용자

        // when & then
        assertThatThrownBy(() -> userService.updateUser(dto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("본인 계정만 수정 가능");

        then(userRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("본인이지만 사용자가 없으면 UsernameNotFoundException 발생")
    void updateUser_userNotFound_throws() {
        // given
        mockSessionUsername("kim");
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");

        given(userRepository.findByUsernameAndIsLockAndIsSocial("kim", false, false))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUser(dto))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("kim");

        then(userRepository).should(never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("정상 수정: 엔티티 저장 후 ID 반환")
    void updateUser_success_returnsId() throws Exception {
        // given
        mockSessionUsername("kim");

        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername("kim");
        dto.setNickname("새닉");
        dto.setEmail("new@example.com");

        // 엔티티 준비(필요 최소 필드만). 빌더가 없다면 생성자/세터로 생성해도 무방
        UserEntity entity = UserEntity.builder()
                .username("kim")
                .password("encoded") // 아무 값
                .build();

        given(userRepository.findByUsernameAndIsLockAndIsSocial("kim", false, false))
                .willReturn(Optional.of(entity));

        // save가 호출되면 id가 채워진 엔티티를 반환하도록 모킹
        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            Field id = UserEntity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(u, 10L);
            return u;
        });

        // when
        Long id = userService.updateUser(dto);

        // then
        assertThat(id).isEqualTo(10L);

        // 저장 시 넘긴 객체 캡처해서 실제로 save가 불렸는지 검증
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        then(userRepository).should().save(captor.capture());
        UserEntity saved = captor.getValue();

        // updateUser(dto)가 적용됐는지(가능한 범위에서) 검증
        // 실제 엔티티 필드 이름에 맞게 필요한 항목만 확인해도 됨
        assertThat(saved.getUsername()).isEqualTo("kim");
        // 닉네임/이메일 등의 세터가 존재한다면 아래도 확인 가능:
        // assertThat(saved.getNickname()).isEqualTo("새닉");
        // assertThat(saved.getEmail()).isEqualTo("new@example.com");
    }
}