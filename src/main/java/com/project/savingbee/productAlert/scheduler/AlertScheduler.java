package com.project.savingbee.productAlert.scheduler;

import com.project.savingbee.productAlert.dto.AlertDispatchResponseDto;
import com.project.savingbee.productAlert.service.AlertDispatchService;
import com.project.savingbee.productAlert.service.AlertMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 9시 일괄 발송, 백오프 = [5, 10, 15]
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

  private final AlertMatchService alertMatchService;
  private final AlertDispatchService alertDispatchService;

  private static final int BATCH_SIZE = 100;

  // 상품 정보 갱신 후 알림과 매칭(매일 5시)
  @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
  public void scanAfterRefresh() {
    int created = alertMatchService.scanAndEnqueue();
  }

  // 알림 전송(매일 9시)
  @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
  public void dispatch0900() {
    runSlot("09:00");
  }

  // 재시도 1
  @Scheduled(cron = "0 5 9 * * *", zone = "Asia/Seoul")
  public void dispatch0905() {
    runSlot("09:05");
  }

  // 재시도 2
  @Scheduled(cron = "0 15 9 * * *", zone = "Asia/Seoul")
  public void dispatch0915() {
    runSlot("09:15");
  }

  // 재시도 3
  @Scheduled(cron = "0 30 9 * * *", zone = "Asia/Seoul")
  public void dispatch0930() {
    runSlot("09:30");
  }

  // 큐를 배치 단위로 비울 때까지 반복, log에 결과 표시
  private void runSlot(String slotName) {
    long t = System.currentTimeMillis();

    try {
      int totalProcessed = 0, totalSent = 0, totalFailed = 0;

      while (true) {
        AlertDispatchResponseDto res = alertDispatchService.dispatchNow(BATCH_SIZE);

        totalProcessed += res.getProcessed();
        totalSent += res.getSent();
        totalFailed += res.getFailed();

        if (res.getProcessed() < BATCH_SIZE) {
          break;
        }
      }

      log.info("[ALERT][DISPATCH][{}] processed={} sent={} failed={} elapsedMs={}",
          slotName, totalProcessed, totalSent, totalFailed, System.currentTimeMillis() - t);

    } catch (Exception e) {
      log.error("[ALERT][DISPATCH][{}][ERROR] {}", slotName, e.getMessage(), e);
    }
  }
}
