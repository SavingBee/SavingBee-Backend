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

    // SameSite=None 설정으로 크로스 도메인 쿠키 전송 허용
    response.setHeader("Set-Cookie", "refreshToken=" + refreshToken +
            "; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=604800");

    response.sendRedirect("https://saving-bee.vercel.app/cookie");
  }

}