package com.project.savingbee.productAlert.service;

import com.project.savingbee.common.entity.ProductAlertEvent;
import com.project.savingbee.common.entity.ProductAlertEvent.EventStatus;
import com.project.savingbee.common.entity.ProductAlertSetting;
import com.project.savingbee.common.entity.ProductAlertSetting.AlertType;
import com.project.savingbee.common.repository.ProductAlertEventRepository;
import com.project.savingbee.domain.user.entity.UserEntity;
import com.project.savingbee.productAlert.channel.compose.AlertMessage;
import com.project.savingbee.productAlert.channel.compose.AlertMessageComposer;
import com.project.savingbee.productAlert.channel.exception.NonRetryableChannelException;
import com.project.savingbee.productAlert.channel.exception.RetryableChannelException;
import com.project.savingbee.productAlert.channel.router.ChannelRouter;
import com.project.savingbee.productAlert.dto.AlertDispatchResponseDto;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertDispatchService {

  private final ProductAlertEventRepository productAlertEventRepository;
  private final AlertEventStateService alertEventStateService;
  private final ChannelRouter channelRouter;
  private final AlertMessageComposer alertMessageComposer;

  /**
   * 배치 전송 1회 수행
   * 대상 : status = READY, sendNotBefore <= now
   * 순서 : 1.조회  2.READY -> SENDING 변경  3. 전송 시도(SENT/FAILED)
   */
  @Transactional
  public AlertDispatchResponseDto dispatchNow(int batchSize) {
    LocalDateTime now = LocalDateTime.now();

    // SENDING timeout 복구
    alertEventStateService.recoverStuckSending(now);

    int processed = 0, sent = 0, failed = 0;

    // 발송 후보(READY, FAILED) 조회 (id 순으로 정렬)
    Page<ProductAlertEvent> page =
        productAlertEventRepository.findByStatusInAndSendNotBeforeLessThanEqual(
            List.of(EventStatus.READY, EventStatus.FAILED), now,
            PageRequest.of(0, batchSize, Sort.by(Sort.Order.asc("id")))
        );

    for (ProductAlertEvent event : page.getContent()) {
      // READY/FAILED -> SENDING 후 커밋
      if (!alertEventStateService.toSending(event, now)) {
        continue;
      }

      processed++;

      // 전송 시도(성공/실패)
      try {
        // 이벤트 매칭 이후 알림 OFF 시, FAILED 처리 + SendNotBefore를 아주 먼 시간으로
        ProductAlertSetting setting = event.getProductAlertSetting();
        UserEntity user = setting.getUserEntity();
        if (user == null || !Boolean.TRUE.equals(user.getAlarm())) {
          event.setStatus(EventStatus.FAILED);
          event.setAttempts(event.getAttempts() + 1);
          event.setSendNotBefore(now.plusYears(100));
          event.setLastError("User alerts off");
          failed++;
          alertEventStateService.commitStatus(event);
          continue;
        }

        AlertType alertType = setting.getAlertType();

        AlertMessage message =
            alertMessageComposer.compose(alertType, event);

        channelRouter.send(alertType, message);

        event.setStatus(EventStatus.SENT);
        event.setSentAt(now);
        event.setAttempts(event.getAttempts() + 1);
        sent++;
      } catch (NonRetryableChannelException e) {
        event.setStatus(EventStatus.FAILED);
        event.setAttempts(event.getAttempts() + 1);
        event.setSendNotBefore(now.plusYears(100));   // 재시도 소용 없는 실패로 아주 먼 시간 설정
        event.setLastError(truncate(e.getMessage(), 200));
        failed++;
      } catch (RetryableChannelException e) {
        event.setStatus(EventStatus.FAILED);
        event.setAttempts(event.getAttempts() + 1);
        event.setLastError(truncate(e.getMessage(), 200));
        failed++;
      }

      alertEventStateService.commitStatus(event);
    }

    return new AlertDispatchResponseDto(processed, sent, failed);
  }

  // 에러 메시지가 너무 길 경우 자르기
  private String truncate(String s, int max) {
    if (s == null) {
      return null;
    }
    return s.length() <= max ? s : s.substring(0, max);
  }
}
