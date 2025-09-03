package com.project.savingbee.config;

import com.project.savingbee.domain.jwt.service.JwtService;
import com.project.savingbee.domain.user.entity.UserRoleType;
import com.project.savingbee.filter.JWTFilter;
import com.project.savingbee.filter.LoginFilter;
import com.project.savingbee.handler.RefreshTokenLogoutHandler;
import com.project.savingbee.util.JWTUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final AuthenticationConfiguration authenticationConfiguration;
  private final AuthenticationSuccessHandler loginSuccessHandler;
  private final AuthenticationSuccessHandler socialSuccessHandler;
  private final JwtService jwtService;
  private final JWTFilter jwtFilter;
  private final JWTUtil jwtUtil;

  public SecurityConfig(AuthenticationConfiguration authenticationConfiguration,
      @Qualifier("LoginSuccessHandler") AuthenticationSuccessHandler loginSuccessHandler,
      @Qualifier("SocialSuccessHandler") AuthenticationSuccessHandler socialSuccessHandler,
      JwtService jwtService,
      JWTFilter jwtFilter,
      JWTUtil jwtUtil
  ) {
    this.authenticationConfiguration = authenticationConfiguration;
    this.loginSuccessHandler = loginSuccessHandler;
    this.socialSuccessHandler = socialSuccessHandler;
    this.jwtService = jwtService;
    this.jwtFilter = jwtFilter;
    this.jwtUtil = jwtUtil;
  }

  // 커스텀 자체 로그인 필터를 위한 AuthenticationManager Bean 수동 등록
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  // 권한 계층
  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.withRolePrefix("ROLE_")
        .role(UserRoleType.ADMIN.name()).implies(UserRoleType.USER.name())
        .build();
  }

  // 비밀번호 단방향(BCrypt) 암호화용 Bean
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // CORS Bean
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // 특정 도메인만 허용 (보안 강화)
    configuration.setAllowedOrigins(List.of(
        "http://localhost:5173",           // 로컬 프론트엔드
        "http://localhost:3000",           // 로컬 리액트 기본 포트
        "http://34.64.73.53",             // 서버 IP
        "https://34.64.73.53"             // HTTPS 서버 IP
    ));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true); // 특정 도메인 허용시 true로 설정
    configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  // 비회원도 사용할 수 있는 공개 API용 보안 체인
  @Bean
  @Order(1)
  SecurityFilterChain publicApis(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/api/compare", "/api/compare/**", "/products/**")
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(a -> a.anyRequest().permitAll());
    return http.build();
  }

  // SecurityFilterChain
  @Bean
  @Order(2)
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    // CSRF 보안 필터 disable
    http
        .csrf(AbstractHttpConfigurer::disable);

    // CORS 설정(리액트+스프링)
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()));

    // 기본 로그아웃 필터 + 커스텀 Refresh 토큰 삭제 핸들러 추가
    http
        .logout(logout -> logout
            .addLogoutHandler(new RefreshTokenLogoutHandler(jwtService, jwtUtil)));

    // 기본 Form 기반 인증 필터들 disable
    http
        .formLogin(AbstractHttpConfigurer::disable);

    // 기본 Basic 인증 필터 disable
    http
        .httpBasic(AbstractHttpConfigurer::disable);

    // OAuth2 인증용
    http
        .oauth2Login(oauth2 -> oauth2
            .successHandler(socialSuccessHandler));

    // Session 정책 (JWT 사용으로 STATELESS)
    http
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    // 커스텀 로그인 필터 추가
    LoginFilter loginFilter = new LoginFilter(authenticationManager(authenticationConfiguration),
        loginSuccessHandler);
    loginFilter.setFilterProcessesUrl("/login");
    http
        .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

    // JWT Filter 추가
    http
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    // 인가 (모든 API 허용 - 개발/테스트용)
    http
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()  // 모든 API 허용
        );

        /*
        // 인가 (정상적인 JWT 인증 설정) - 운영시 주석 해제
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/jwt/exchange", "/jwt/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST,
                            "/user/exist",
                            "/user",
                            "/user/signup/**",
                            "/user/find-username/**",
                            "/user/find-password/**",
                            "/user/reset-password/**",
                            "/user/verify-code",
                            "/user/new-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/user").hasRole(UserRoleType.USER.name())
                        .requestMatchers(HttpMethod.PUT, "/user").hasRole(UserRoleType.USER.name())
                        .requestMatchers(HttpMethod.DELETE, "/user").hasRole(UserRoleType.USER.name())
                        .requestMatchers("/api/mypage/**").hasRole(UserRoleType.USER.name())
                        .requestMatchers("/api/user-products/**").hasRole(UserRoleType.USER.name())
                        .requestMatchers("/api/cart/**").hasRole(UserRoleType.USER.name())
                        .requestMatchers("/api/recommendations/**").hasRole(UserRoleType.USER.name())
                        .anyRequest().authenticated()
                );
        */


        /*http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/jwt/exchange", "/jwt/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST,
                            "/user/exist",
                            "/user",
                            "/user/signup/**",
                            "/user/find-username/**",
                            "/user/find-password/**",
                            "/user/reset-password/**",
                            "/user/verify-code",
                            "/user/new-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/user").hasRole(UserRoleType.USER.name())
                        .requestMatchers(HttpMethod.PUT, "/user").hasRole(UserRoleType.USER.name())
                        .requestMatchers(HttpMethod.DELETE, "/user").hasRole(UserRoleType.USER.name())
                        .requestMatchers("/api/mypage/**").hasRole(UserRoleType.USER.name())
                        .requestMatchers("/api/user-products/**").hasRole(UserRoleType.USER.name())
                        .requestMatchers("/api/cart/**").hasRole(UserRoleType.USER.name())
                        .requestMatchers("/api/recommendations/**").hasRole(UserRoleType.USER.name())
                    .anyRequest().authenticated()
                        );
                    .anyRequest().permitAll());*/

    // 예외 처리
    http
        .exceptionHandling(e -> e
            .authenticationEntryPoint((request, response, authException) -> {
              response.sendError(HttpServletResponse.SC_UNAUTHORIZED); // 401 응답
            })
            .accessDeniedHandler((request, response, authException) -> {
              response.sendError(HttpServletResponse.SC_FORBIDDEN); // 403 응답
            })
        );

    return http.build();
  }
}
