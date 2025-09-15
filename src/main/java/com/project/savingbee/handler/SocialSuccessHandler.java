package com.project.savingbee.handler;

import com.project.savingbee.domain.jwt.service.JwtService;
import com.project.savingbee.util.JWTUtil;
import jakarta.servlet.ServletException;
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
    String rawRole = authentication.getAuthorities().iterator().next().getAuthority();
    
    // 디버그 로그 추가
    System.out.println("=== Social Login Success Handler Debug ===");
    System.out.println("Username: " + username);
    System.out.println("Raw Role from Authentication: " + rawRole);
    
    // Role 중복 접두사 문제 해결: 이미 ROLE_로 시작하는지 확인
    String role;
    if (rawRole.startsWith("ROLE_")) {
      role = rawRole; // 이미 ROLE_ 접두사가 있으면 그대로 사용
    } else {
      role = "ROLE_" + rawRole; // 없으면 ROLE_ 접두사 추가
    }
    
    System.out.println("Final Role for JWT: " + role);
    
    // JWT(Refresh) 발급
    String refreshToken = jwtUtil.createJWT(username, role, false);
    System.out.println("Refresh token created: " + refreshToken.substring(0, Math.min(20, refreshToken.length())) + "...");
    
    // 발급한 Refresh DB 테이블 저장 (Refresh whitelist)
    jwtService.addRefresh(username, refreshToken);
    System.out.println("Refresh token saved to DB");

    // SameSite=None 설정으로 크로스 도메인 쿠키 전송 허용
    response.setHeader("Set-Cookie", "refreshToken=" + refreshToken +
            "; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=604800");
    
    System.out.println("Cookie set, redirecting to frontend...");
    response.sendRedirect("https://saving-bee.vercel.app/cookie");
  }

}