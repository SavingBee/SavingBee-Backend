package com.project.savingbee.filter;

import com.project.savingbee.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JWTFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        System.out.println("=== JWT Filter Debug ===");
        System.out.println("Authorization Header: " + authorization);
        
        if (authorization == null) {
            System.out.println("No Authorization header found");
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith("Bearer ")) {
            System.out.println("Not a Bearer token");
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 파싱 (여러 공백 처리)
        String accessToken = authorization.substring("Bearer".length()).trim();
        System.out.println("Extracted Token: " + (accessToken.length() > 20 ? accessToken.substring(0, 20) + "..." : accessToken));
        System.out.println("Token length: " + accessToken.length());

        if (accessToken.isEmpty()) {
            System.out.println("Token is empty after extraction");
            filterChain.doFilter(request, response);
            return;
        }

        boolean isTokenValid = JWTUtil.isValid(accessToken, true);
        System.out.println("Token Valid: " + isTokenValid);

        if (isTokenValid) {
            try {
                String username = JWTUtil.getUsername(accessToken);
                String role = JWTUtil.getRole(accessToken);
                
                System.out.println("Username from token: " + username);
                System.out.println("Role from token: " + role);

                List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

                // UserDetails 객체 생성
                UserDetails userDetails = User.builder()
                        .username(username)
                        .password("") // JWT에서는 비밀번호가 필요없음
                        .authorities(authorities)
                        .build();

                Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                
                System.out.println("Authentication set successfully");
                System.out.println("SecurityContext authentication: " + SecurityContextHolder.getContext().getAuthentication());
                System.out.println("SecurityContext principal: " + SecurityContextHolder.getContext().getAuthentication().getPrincipal());
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                System.out.println("Error parsing token: " + e.getMessage());
                e.printStackTrace();
                filterChain.doFilter(request, response);
            }

        } else {
            System.out.println("Token is invalid - continuing without authentication");
            filterChain.doFilter(request, response);
            return;
        }

    }

}