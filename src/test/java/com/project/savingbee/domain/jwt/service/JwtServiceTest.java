package com.project.savingbee.domain.jwt.service;

import com.project.savingbee.domain.jwt.dto.JWTResponseDTO;
import com.project.savingbee.domain.jwt.dto.RefreshRequestDTO;
import com.project.savingbee.domain.jwt.entity.RefreshEntity;
import com.project.savingbee.domain.jwt.repository.RefreshRepository;
import com.project.savingbee.domain.jwt.service.JwtService;
import com.project.savingbee.util.JWTUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    RefreshRepository refreshRepository;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @InjectMocks
    JwtService jwtService;

    @Test
    @DisplayName("cookie2Header: refreshToken 쿠키가 존재하지 않으면 예외 발생")
    void cookie2Header_noCookie() {
        // given
        given(request.getCookies()).willReturn(null);

        // when & then
        assertThatThrownBy(() -> jwtService.cookie2Header(request, response))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("쿠키가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("cookie2Header: refreshToken 쿠키가 있지만 유효하지 않으면 예외 발생")
    void cookie2Header_invalidRefresh() {
        // given
        Cookie cookie = new Cookie("refreshToken", "INVALID");
        given(request.getCookies()).willReturn(new Cookie[]{cookie});

        mockStatic(JWTUtil.class).when(() -> JWTUtil.isValid("INVALID", false))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() -> jwtService.cookie2Header(request, response))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("유효하지 않은 refreshToken입니다.");
    }

    @Test
    @DisplayName("cookie2Header: 정상적인 refreshToken이면 access/refresh 토큰 재발급 및 저장")
    void cookie2Header_success() {
        // given
        String oldRefresh = "OLD_REFRESH";
        Cookie cookie = new Cookie("refreshToken", oldRefresh);
        given(request.getCookies()).willReturn(new Cookie[]{cookie});

        try (var mocked = mockStatic(JWTUtil.class)) {
            mocked.when(() -> JWTUtil.isValid(oldRefresh, false)).thenReturn(true);
            mocked.when(() -> JWTUtil.getUsername(oldRefresh)).thenReturn("kim");
            mocked.when(() -> JWTUtil.getRole(oldRefresh)).thenReturn("USER");
            mocked.when(() -> JWTUtil.createJWT("kim", "USER", true)).thenReturn("NEW_ACCESS");
            mocked.when(() -> JWTUtil.createJWT("kim", "USER", false)).thenReturn("NEW_REFRESH");

            given(refreshRepository.save(any(RefreshEntity.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            JWTResponseDTO dto = jwtService.cookie2Header(request, response);

            // then
            assertThat(dto.accessToken()).isEqualTo("NEW_ACCESS");
            assertThat(dto.refreshToken()).isEqualTo("NEW_REFRESH");

            then(refreshRepository).should().deleteByRefresh(oldRefresh);
            then(refreshRepository).should().save(any(RefreshEntity.class));
        }
    }
    @Test
    @DisplayName("refreshRotate: refreshToken 유효하지 않으면 예외 발생")
    void refreshRotate_invalid() {
        // given
        RefreshRequestDTO dto = new RefreshRequestDTO();
        dto.setRefreshToken("BAD_REFRESH");

        try (var mocked = mockStatic(JWTUtil.class)) {
            mocked.when(() -> JWTUtil.isValid("BAD_REFRESH", false)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> jwtService.refreshRotate(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("유효하지 않은 refreshToken입니다.");
        }
    }

    @Test
    @DisplayName("refreshRotate: refreshToken 유효하면 access/refresh 재발급 및 저장")
    void refreshRotate_success() {
        // given
        String oldRefresh = "OLD_REFRESH";
        RefreshRequestDTO dto = new RefreshRequestDTO();
        dto.setRefreshToken(oldRefresh);

        try (var mocked = mockStatic(JWTUtil.class)) {
            mocked.when(() -> JWTUtil.isValid(oldRefresh, false)).thenReturn(true);
            mocked.when(() -> JWTUtil.getUsername(oldRefresh)).thenReturn("kim");
            mocked.when(() -> JWTUtil.getRole(oldRefresh)).thenReturn("USER");
            mocked.when(() -> JWTUtil.createJWT("kim", "USER", true)).thenReturn("NEW_ACCESS");
            mocked.when(() -> JWTUtil.createJWT("kim", "USER", false)).thenReturn("NEW_REFRESH");

            given(refreshRepository.save(any(RefreshEntity.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            JWTResponseDTO result = jwtService.refreshRotate(dto);

            // then
            assertThat(result.accessToken()).isEqualTo("NEW_ACCESS");
            assertThat(result.refreshToken()).isEqualTo("NEW_REFRESH");
            then(refreshRepository).should().deleteByRefresh(oldRefresh);
            then(refreshRepository).should().save(any(RefreshEntity.class));
        }
    }

    @Test
    @DisplayName("addRefresh: DB에 refresh 토큰 저장")
    void addRefresh_success() {
        // given
        given(refreshRepository.save(any(RefreshEntity.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        jwtService.addRefresh("kim", "REFRESH");

        // then
        then(refreshRepository).should().save(any(RefreshEntity.class));
    }

    @Test
    @DisplayName("existsRefresh: refresh 토큰 존재여부 확인")
    void existsRefresh_success() {
        // given
        given(refreshRepository.existsByRefresh("REFRESH")).willReturn(true);

        // when
        Boolean result = jwtService.existsRefresh("REFRESH");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("removeRefresh: refresh 토큰 삭제 호출")
    void removeRefresh_success() {
        // when
        jwtService.removeRefresh("REFRESH");

        // then
        then(refreshRepository).should().deleteByRefresh("REFRESH");
    }

    @Test
    @DisplayName("removeRefreshUser: 특정 유저의 refresh 토큰 전체 삭제")
    void removeRefreshUser_success() {
        // when
        jwtService.removeRefreshUser("kim");

        // then
        then(refreshRepository).should().deleteByUsername("kim");
    }
}
