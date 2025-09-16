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
    
    // JWT(Access/Refresh) 발급
    String accessToken = jwtUtil.createJWT(username, role, true);
    String refreshToken = jwtUtil.createJWT(username, role, false);
    System.out.println("Access token created: " + accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
    System.out.println("Refresh token created: " + refreshToken.substring(0, Math.min(20, refreshToken.length())) + "...");
    
    // 발급한 Refresh DB 테이블 저장 (Refresh whitelist)
    jwtService.addRefresh(username, refreshToken);
    System.out.println("Refresh token saved to DB");

    // 토큰을 쿠키에 임시 저장 (프론트엔드에서 읽을 수 있도록)
    response.setHeader("Set-Cookie", 
        "accessToken=" + accessToken + "; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=86400, " +
        "refreshToken=" + refreshToken + "; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=604800");
    
    System.out.println("Tokens set in cookies, redirecting to frontend...");
    response.sendRedirect("https://saving-bee.vercel.app/cookie");
  }

}