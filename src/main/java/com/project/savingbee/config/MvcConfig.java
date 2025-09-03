package com.project.savingbee.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
  // CORS 설정은 SecurityConfig에서 통합 관리
}