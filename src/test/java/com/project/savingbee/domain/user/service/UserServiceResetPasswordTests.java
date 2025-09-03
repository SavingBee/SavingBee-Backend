//package com.project.savingbee.domain.user.service;
//
//import com.project.savingbee.domain.jwt.service.JwtService;
//import com.project.savingbee.domain.user.dto.UserRequestDTO;
//import com.project.savingbee.domain.user.entity.UserEntity;
//import com.project.savingbee.domain.user.repository.UserRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.Optional;
//import java.util.regex.Pattern;
//
//import static org.mockito.BDDMockito.*;
//import static org.assertj.core.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceResetPasswordTests { //비밀번호 재설정 테스트
//
//    @Mock PasswordEncoder passwordEncoder;
//    @Mock UserRepository userRepository;
//    @Mock
//    JwtService jwtService;
//    @Mock EmailService emailService;
//
//    @InjectMocks UserService userService;
//
//    @Test
//    @DisplayName("resetPassword: 아이디/이메일이 일치하는 계정이 없으면 IllegalArgumentException")
//    void resetPassword_notFound() {
//        // given
//        UserRequestDTO dto = new UserRequestDTO();
//        dto.setUsername("kim");
//        dto.setEmail("kim@example.com");
//
//        given(userRepository.findByUsernameAndEmailAndIsSocial("kim", "kim@example.com", false))
//                .willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> userService.resetPassword(dto))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("입력하신 아이디와 이메일이 일치하는 계정이 없습니다.");
//        then(emailService).shouldHaveNoInteractions();
//        then(passwordEncoder).shouldHaveNoInteractions();
//    }
//
//    @Test
//    @DisplayName("resetPassword: 임시 비밀번호(8자리 영숫자) 생성, 인코딩 저장, 이메일 발송")
//    void resetPassword_success() {
//        // given
//        UserRequestDTO dto = new UserRequestDTO();
//        dto.setUsername("kim");
//        dto.setEmail("kim@example.com");
//
//        UserEntity user = UserEntity.builder()
//                .id(10L).username("kim").email("kim@example.com").isSocial(false)
//                .password("old") // 기존 패스워드
//                .build();
//
//        given(userRepository.findByUsernameAndEmailAndIsSocial("kim", "kim@example.com", false))
//                .willReturn(Optional.of(user));
//
//        // passwordEncoder가 어떤 문자열이 오든 "ENC(xxx)"로 감싸서 반환하도록 설정
//        given(passwordEncoder.encode(anyString()))
//                .willAnswer(inv -> "ENC(" + inv.getArgument(0, String.class) + ")");
//
//        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> inv.getArgument(0));
//
//        // ArgumentCaptor로 이메일에 담겨가는 임시 비밀번호 캡처
//        ArgumentCaptor<String> tempPwCaptor = ArgumentCaptor.forClass(String.class);
//
//        // when
//        userService.resetPassword(dto);
//
//        // then: emailService 호출 내용 검증 (임시 비밀번호 형태 체크)
//        then(emailService).should().sendTemporaryPasswordEmail(
//                eq("kim@example.com"),
//                eq("kim"),
//                tempPwCaptor.capture()
//        );
//
//        String tempPw = tempPwCaptor.getValue();
//        assertThat(tempPw).isNotNull();
//        assertThat(tempPw.length()).isEqualTo(8);
//        assertThat(Pattern.matches("^[A-Za-z0-9]{8}$", tempPw)).isTrue();
//
//        // 비밀번호 인코딩/저장 시 동일한 임시 비밀번호가 사용됐는지 검증
//        then(passwordEncoder).should().encode(eq(tempPw));
//        then(userRepository).should().save(argThat(saved ->
//                saved.getUsername().equals("kim") &&
//                        saved.getPassword().equals("ENC(" + tempPw + ")")
//        ));
//    }
//}
