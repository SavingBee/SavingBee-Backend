package com.project.savingbee.api;

import com.project.savingbee.domain.jwt.service.JwtService;
import com.project.savingbee.util.JWTUtil;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

  private final JwtService jwtService;
  private final JWTUtil jwtUtil;

  public AuthController(JwtService jwtService, JWTUtil jwtUtil) {
    this.jwtService = jwtService;
    this.jwtUtil = jwtUtil;
  }

  // 로그아웃 API
  @PostMapping(value = "/auth/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, String>> logoutApi(
      @RequestBody(required = false) Map<String, String> requestBody
  ) {
    try {
      // Refresh Token이 제공된 경우 DB에서 삭제
      if (requestBody != null && requestBody.containsKey("refreshToken")) {
        String refreshToken = requestBody.get("refreshToken");

        // Refresh Token 유효성 검증
        if (refreshToken != null && jwtUtil.isValid(refreshToken, false)) {
          jwtService.removeRefresh(refreshToken);
        }
      }

      Map<String, String> responseBody = Collections.singletonMap("message", "로그아웃이 완료되었습니다.");
      return ResponseEntity.ok(responseBody);

    } catch (Exception e) {
      Map<String, String> responseBody = Collections.singletonMap("message",
          "로그아웃 처리 중 오류가 발생했습니다.");
      return ResponseEntity.ok(responseBody); // 로그아웃은 항상 성공으로 처리
    }
  }
}
