package com.project.savingbee.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtil {

  private static SecretKey secretKey;
  private static Long accessTokenExpiresIn;
  private static Long refreshTokenExpiresIn;

  // @Value를 통해 설정 파일에서 값 주입
  public JWTUtil(@Value("${spring.jwt.secret}") String secretKeyString,
      @Value("${jwt.access-token.expire-time}") Long accessExpireTime,
      @Value("${jwt.refresh-token.expire-time}") Long refreshExpireTime) {

    secretKey = new SecretKeySpec(secretKeyString.getBytes(StandardCharsets.UTF_8),
        Jwts.SIG.HS256.key().build().getAlgorithm());
    accessTokenExpiresIn = accessExpireTime;
    refreshTokenExpiresIn = refreshExpireTime;
  }

  // JWT 클레임 username 파싱
  public String getUsername(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
        .get("sub", String.class);
  }

  // JWT 클레임 role 파싱
  public String getRole(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
        .get("role", String.class);
  }

  // JWT 유효 여부 (위조, 시간, Access/Refresh 여부)
  public Boolean isValid(String token, Boolean isAccess) {
    try {  /*try 하는 이유: {Claims ... retrun false;}를 파싱하는 과정에서 시간이 다되었다면 자동으로 JwtException이 던져지기 때문에
            이것을 잡을 수 있도록하기 위함*/
      System.out.println("=== JWTUtil.isValid Debug ===");
      System.out.println("Validating token for isAccess: " + isAccess);

      Claims claims = Jwts.parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token)
          .getPayload();

      System.out.println("Token parsed successfully");
      System.out.println("Claims: " + claims);

      String type = claims.get("type", String.class);
      System.out.println("Token type: " + type);
      System.out.println("Expected type: " + (isAccess ? "access" : "refresh"));

      if (type == null) {
        System.out.println("Type is null - returning false");
        return false;
      }

      if (isAccess && !type.equals("access")) {
        System.out.println("Access token expected but got: " + type);
        return false;
      }
      if (!isAccess && !type.equals("refresh")) {
        System.out.println("Refresh token expected but got: " + type);
        return false;
      }

      System.out.println("Token validation successful");
      return true;

    } catch (JwtException | IllegalArgumentException e) {
      System.out.println("Token validation failed with exception: " + e.getClass().getSimpleName());
      System.out.println("Exception message: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  // JWT(Access/Refresh) 생성
  public String createJWT(String username, String role, Boolean isAccess) {

    long now = System.currentTimeMillis();
    long expiry = isAccess ? accessTokenExpiresIn : refreshTokenExpiresIn;
    String type = isAccess ? "access" : "refresh";

    System.out.println("=== JWT Creation Debug ===");
    System.out.println("Current time (ms): " + now);
    System.out.println("Current time (date): " + new Date(now));
    System.out.println("Expiry duration (ms): " + expiry);
    System.out.println("Expiration time (ms): " + (now + expiry));
    System.out.println("Expiration time (date): " + new Date(now + expiry));
    System.out.println("Token type: " + type);

    return Jwts.builder()
        .claim("sub", username)
        .claim("role", role)
        .claim("type", type) // type: Access인지 Refresh에 대한 타입
        .issuedAt(new Date(now)) // 현재 JWT에 생성, 즉 발급시간
        .expiration(new Date(now + expiry)) // 생명주기
        .signWith(secretKey) // JWT를 우리의 비밀키로 시그니처를 만드는 메서드
        .compact();
  }

}