package com.project.savingbee.config;

import com.project.savingbee.connectApi.service.DepositConnectApi;
import com.project.savingbee.connectApi.service.SavingConnectApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableScheduling
@Slf4j
public class SchedulingConfig {

  private final DepositConnectApi depositConnectApi;
  private final SavingConnectApi savingConnectApi;

  public SchedulingConfig(DepositConnectApi depositConnectApi, SavingConnectApi savingConnectApi) {
    this.depositConnectApi = depositConnectApi;
    this.savingConnectApi = savingConnectApi;
  }

  /**
   * 서버 시작 시 API 연동 실행
   */
  @PostConstruct
  public void initializeApiData() {
    log.info("=== 서버 시작 시 금융 상품 데이터 초기화 시작 ===");
    try {
      // 예금 상품 데이터 연동
      log.info("예금 상품 API 연동 시작");
      depositConnectApi.connectDepositApi();
      log.info("예금 상품 API 연동 완료");

      // 적금 상품 데이터 연동
      log.info("적금 상품 API 연동 시작");
      savingConnectApi.connectSavingApi();
      log.info("적금 상품 API 연동 완료");

      log.info("=== 서버 시작 시 금융 상품 데이터 초기화 완료 ===");

    } catch (Exception e) {
      log.error("서버 시작 시 API 연동 중 오류 발생 + 서버 진행 계속", e);

    }
  }

  /**
   * 매일 새벽 3시에 API 데이터 업데이트 실행 cron: 초(0) 분(0) 시간(3) 일(*) 월(*) 요일(*)
   */
  @Scheduled(cron = "0 0 3 * * *")
  public void updateApiDataDaily() {
    log.info("=== 매일 새벽 3시 금융 상품 데이터 업데이트 시작 ===");
    try {
      // 예금 상품 데이터 업데이트
      log.info("예금 상품 API 업데이트 시작");
      depositConnectApi.connectDepositApi();
      log.info("예금 상품 API 업데이트 완료");

      // 적금 상품 데이터 업데이트
      log.info("적금 상품 API 업데이트 시작");
      savingConnectApi.connectSavingApi();
      log.info("적금 상품 API 업데이트 완료");

      log.info("=== 매일 새벽 3시 금융 상품 데이터 업데이트 완료 ===");

    } catch (Exception e) {
      log.error("매일 새벽 3시 API 업데이트 중 오류 발생", e);
    }
  }
}