package com.project.savingbee.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JWTUtil {

    private static final SecretKey secretKey;
    private static final Long accessTokenExpiresIn;
    private static final Long refreshTokenExpiresIn;

    static  {
        String secretKeyString = "himynameissavingbeenicetomeetyou";
        secretKey = new SecretKeySpec(secretKeyString.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());

        // 개발용으로 24시간 설정 (운영시에는 1-2시간 권장)
        accessTokenExpiresIn = 24 * 3600L * 1000; // 24시간 (개발용)
        refreshTokenExpiresIn = 604800L * 1000; // 7일
    }

    // JWT 클레임 username 파싱
    public static String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("sub", String.class);
    }

    // JWT 클레임 role 파싱
    public static String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    // JWT 유효 여부 (위조, 시간, Access/Refresh 여부)
    public static Boolean isValid(String token, Boolean isAccess) {
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
    public static String createJWT(String username, String role, Boolean isAccess) {

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