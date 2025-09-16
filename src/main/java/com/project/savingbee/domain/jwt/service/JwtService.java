package com.project.savingbee.domain.jwt.service;

import com.project.savingbee.domain.jwt.dto.JWTResponseDTO;
import com.project.savingbee.domain.jwt.dto.RefreshRequestDTO;
import com.project.savingbee.domain.jwt.entity.RefreshEntity;
import com.project.savingbee.domain.jwt.repository.RefreshRepository;
import com.project.savingbee.util.JWTUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtService {

  private final RefreshRepository refreshRepository;
  private final JWTUtil jwtUtil;

  public JwtService(RefreshRepository refreshRepository, JWTUtil jwtUtil) {
    this.refreshRepository = refreshRepository;
    this.jwtUtil = jwtUtil;
  }

  // 소셜 로그인 성공 후 쿠키(Refresh) -> 헤더 방식으로 응답 <-- 이건 추후에 작성
  @Transactional
  public JWTResponseDTO cookie2Header(
      HttpServletRequest request,
      HttpServletResponse response
  ) {

    // 쿠키 리스트
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      throw new RuntimeException("쿠키가 존재하지 않습니다.");
    }

    // Access & Refresh 토큰 획득
    String accessToken = null;
    String refreshToken = null;
    for (Cookie cookie : cookies) {
      if ("accessToken".equals(cookie.getName())) {
        accessToken = cookie.getValue();
      } else if ("refreshToken".equals(cookie.getName())) {
        refreshToken = cookie.getValue();
      }
    }

    // accessToken이 있으면 그대로 사용, 없으면 refreshToken으로 새로 생성
    if (accessToken != null && jwtUtil.isValid(accessToken, true)) {
      // 유효한 accessToken이 있는 경우 - 기존 쿠키 제거
      clearAuthCookies(response);
      
      return new JWTResponseDTO(accessToken, refreshToken);
    }

    if (refreshToken == null) {
      throw new RuntimeException("refreshToken 쿠키가 없습니다.");
    }

    // Refresh 토큰 검증
    Boolean isValid = jwtUtil.isValid(refreshToken, false);
    if (!isValid) {
      throw new RuntimeException("유효하지 않은 refreshToken입니다.");
    }

    // 정보 추출
    String username = jwtUtil.getUsername(refreshToken);
    String role = jwtUtil.getRole(refreshToken);

    // 토큰 생성
    String newAccessToken = jwtUtil.createJWT(username, role, true);
    String newRefreshToken = jwtUtil.createJWT(username, role, false);

    // 기존 Refresh 토큰 DB 삭제 후 신규 추가
    RefreshEntity newRefreshEntity = RefreshEntity.builder()
        .username(username)
        .refresh(newRefreshToken)
        .build();

    removeRefresh(refreshToken);
    refreshRepository.flush(); // 같은 트랜잭션 내부라 : 삭제 -> 생성 문제 해결
    refreshRepository.save(newRefreshEntity);

    // 기존 쿠키 제거
    clearAuthCookies(response);

    return new JWTResponseDTO(newAccessToken, newRefreshToken);
  }

  // Refresh 토큰으로 Access 토큰 재발급 로직 (Rotate 포함) <-- 이건 추후에 작성
  @Transactional
  public JWTResponseDTO refreshRotate(RefreshRequestDTO dto) {

    String refreshToken = dto.getRefreshToken();

    // Refresh 토큰 검증
    Boolean isValid = jwtUtil.isValid(refreshToken, false);
    if (!isValid) {
      throw new RuntimeException("유효하지 않은 refreshToken입니다.");
    }

    // 정보 추출
    String username = jwtUtil.getUsername(refreshToken);
    String role = jwtUtil.getRole(refreshToken);

    // 토큰 생성
    String newAccessToken = jwtUtil.createJWT(username, role, true);
    String newRefreshToken = jwtUtil.createJWT(username, role, false);

    // 기존 Refresh 토큰 DB 삭제 후 신규 추가
    RefreshEntity newRefreshEntity = RefreshEntity.builder()
        .username(username)
        .refresh(newRefreshToken)
        .build();

    removeRefresh(refreshToken);
    refreshRepository.save(newRefreshEntity);

    return new JWTResponseDTO(newAccessToken, newRefreshToken);
  }

  // JWT Refresh 토큰 발급 후 저장 메소드
  @Transactional
  public void addRefresh(String username, String refreshToken) {
    RefreshEntity entity = RefreshEntity.builder()
        .username(username)
        .refresh(refreshToken)
        .build();

    refreshRepository.save(entity);
  }

  // JWT Refresh 존재 확인 메소드
  @Transactional(readOnly = true)
  public Boolean existsRefresh(String refreshToken) {
    return refreshRepository.existsByRefresh(refreshToken);
  }

  // JWT Refresh 토큰 삭제 메소드
  @Transactional
  public void removeRefresh(String refreshToken) {
    refreshRepository.deleteByRefresh(refreshToken);
  }

  // 특정 유저 Refresh 토큰 모두 삭제 (탈퇴)
  @Transactional
  public void removeRefreshUser(String username) {
    refreshRepository.deleteByUsername(username);
  }

  // 인증 관련 쿠키 제거 헬퍼 메서드
  private void clearAuthCookies(HttpServletResponse response) {
    // accessToken 쿠키 제거
    Cookie accessCookie = new Cookie("accessToken", null);
    accessCookie.setHttpOnly(true);
    accessCookie.setSecure(true);
    accessCookie.setPath("/");
    accessCookie.setMaxAge(0);
    response.addCookie(accessCookie);

    // refreshToken 쿠키 제거
    Cookie refreshCookie = new Cookie("refreshToken", null);
    refreshCookie.setHttpOnly(true);
    refreshCookie.setSecure(true);
    refreshCookie.setPath("/");
    refreshCookie.setMaxAge(0);
    response.addCookie(refreshCookie);
  }
}