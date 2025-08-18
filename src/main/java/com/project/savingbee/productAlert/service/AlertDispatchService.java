package com.project.savingbee.productAlert.service;

import com.project.savingbee.common.entity.ProductAlertEvent;
import com.project.savingbee.common.entity.ProductAlertEvent.EventStatus;
import com.project.savingbee.common.repository.ProductAlertEventRepository;
import com.project.savingbee.productAlert.dto.AlertDispatchResponseDto;
import java.time.LocalDateTime;
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

  /**
   * 배치 전송 1회 수행
   * 대상 : status = READY, sendNotBefore <= now
   * 순서 : 1.조회  2.READY -> SENDING 변경  3. 전송 시도(SENT/FAILED)
  */
  @Transactional
  public AlertDispatchResponseDto dispatchNow(int batchSize) {
    LocalDateTime now = LocalDateTime.now();

    int processed = 0, sent = 0, failed = 0;

    // 발송 후보(READY) 조회 (id 순으로 정렬)
    Page<ProductAlertEvent> page =
        productAlertEventRepository.findByStatusAndSendNotBeforeLessThanEqual(
        EventStatus.READY, now,
            PageRequest.of(0, batchSize, Sort.by(Sort.Order.asc("id")))
        );

    for (ProductAlertEvent event : page.getContent()) {
      // READY -> SENDING
      if (toSending(event, now)) {
        processed++;
      }

      // 전송 시도(성공/실패)
      try {
        event.setStatus(EventStatus.SENT);
        event.setSentAt(now);
        event.setAttempts(event.getAttempts() + 1);
        sent++;
      } catch (Exception e) {
        event.setStatus(EventStatus.FAILED);
        event.setAttempts(event.getAttempts() + 1);
        event.setLastError(e.getMessage());
        failed++;
      }

      productAlertEventRepository.save(event);
    }

    return new AlertDispatchResponseDto(processed, sent, failed);
  }

  // READY -> SENDING
  private boolean toSending(ProductAlertEvent event, LocalDateTime now) {
    if (event.getStatus() != EventStatus.READY) {
      return false;
    }
    event.setStatus(EventStatus.SENDING);
    event.setUpdatedAt(now);
    productAlertEventRepository.save(event);
    return true;
  }
}
