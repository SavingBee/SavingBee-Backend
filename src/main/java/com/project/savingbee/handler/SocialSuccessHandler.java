package com.project.savingbee.handler;

import com.project.savingbee.domain.jwt.service.JwtService;
import com.project.savingbee.util.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Qualifier("SocialSuccessHandler")
public class SocialSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtService jwtService;
  private final JWTUtil jwtUtil;

  public SocialSuccessHandler(JwtService jwtService, JWTUtil jwtUtil) {
    this.jwtService = jwtService;
    this.jwtUtil = jwtUtil;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    // username, role
    String username = authentication.getName();
    String role = authentication.getAuthorities().iterator().next().getAuthority();

    // JWT(Refresh) 발급
    String refreshToken = jwtUtil.createJWT(username, "ROLE_" + role, false);

    // 발급한 Refresh DB 테이블 저장 (Refresh whitelist)
    jwtService.addRefresh(username, refreshToken);

    // 응답
    Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
    refreshCookie.setHttpOnly(true);
    refreshCookie.setSecure(false); //true면 https만 가능 false면 http, https 둘다 가능
    refreshCookie.setPath("/");
    refreshCookie.setMaxAge(604800); // 7일 (프론트에서 발급 후 바로 헤더 전환 로직 진행 예정)

    response.addCookie(refreshCookie);
    response.sendRedirect("http://34.64.73.53/cookie"); // 이부분에 프론트엔드 서버 주소 등록해야함
  }

}