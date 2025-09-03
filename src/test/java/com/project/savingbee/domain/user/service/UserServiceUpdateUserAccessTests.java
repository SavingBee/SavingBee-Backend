//package com.project.savingbee.domain.user.service;
//
//import com.project.savingbee.domain.user.dto.UserRequestDTO;
//import com.project.savingbee.domain.user.entity.UserEntity;
//import com.project.savingbee.domain.user.repository.UserRepository;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.*;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.BDDMockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceUpdateUserAccessTests { // 회원정보수정: 권한검사/미존재
//
//    @Mock UserRepository userRepository;
//    @InjectMocks UserService userService;
//
//    private SecurityContext original;
//
//    @BeforeEach
//    void setUp() {
//        original = SecurityContextHolder.getContext();
//        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
//        Authentication auth = mock(Authentication.class);
//        given(auth.getName()).willReturn("owner");
//        ctx.setAuthentication(auth);
//        SecurityContextHolder.setContext(ctx);
//    }
//
//    @AfterEach
//    void tearDown() {
//        SecurityContextHolder.clearContext();
//        SecurityContextHolder.setContext(original);
//    }
//
//    @Test
//    @DisplayName("updateUser: 세션 사용자와 다른 username이면 AccessDeniedException")
//    void updateUser_accessDenied() {
//        UserRequestDTO dto = new UserRequestDTO();
//        dto.setUsername("other");
//
//        assertThatThrownBy(() -> userService.updateUser(dto))
//                .isInstanceOf(AccessDeniedException.class)
//                .hasMessageContaining("본인 계정만 수정 가능");
//    }
//
//    @Test
//    @DisplayName("updateUser: 대상 유저 미존재면 UsernameNotFoundException")
//    void updateUser_notFound() {
//        UserRequestDTO dto = new UserRequestDTO();
//        dto.setUsername("owner");
//
//        given(userRepository.findByUsernameAndIsLockAndIsSocial("owner", false, false))
//                .willReturn(Optional.empty());
//
//        assertThatThrownBy(() -> userService.updateUser(dto))
//                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
//    }
//
//    @Test
//    @DisplayName("updateUser: 정상 수정 시 save 호출 및 id 반환")
//    void updateUser_success() {
//        UserRequestDTO dto = new UserRequestDTO();
//        dto.setUsername("owner");
//        dto.setNickname("새닉");
//        dto.setEmail("n@e.com");
//
//        UserEntity entity = UserEntity.builder().id(1L).username("owner").build();
//        given(userRepository.findByUsernameAndIsLockAndIsSocial("owner", false, false))
//                .willReturn(Optional.of(entity));
//        given(userRepository.save(entity)).willReturn(entity);
//
//        Long id = userService.updateUser(dto);
//
//        assertThat(id).isEqualTo(1L);
//        then(userRepository).should().save(entity);
//    }
//}
