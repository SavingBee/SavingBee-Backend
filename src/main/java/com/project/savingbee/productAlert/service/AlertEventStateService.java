package com.project.savingbee.productAlert.service;

import com.project.savingbee.common.entity.ProductAlertEvent;
import com.project.savingbee.common.entity.ProductAlertEvent.EventStatus;
import com.project.savingbee.common.repository.ProductAlertEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertEventStateService {

  private final ProductAlertEventRepository productAlertEventRepository;

  // READY/FAILED -> SENDING 후 커밋
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean toSending(ProductAlertEvent event, LocalDateTime now) {
    if (event.getStatus() != EventStatus.READY && event.getStatus() != EventStatus.FAILED) {
      return false;
    }
    event.setStatus(EventStatus.SENDING);
    event.setUpdatedAt(now);
    productAlertEventRepository.save(event);
    return true;
  }

  // 1분간 SENDING 유지 시 FAILED 처리
  @Transactional
  public void recoverStuckSending(LocalDateTime now) {
    LocalDateTime threshold = now.minusSeconds(60);

    List<ProductAlertEvent> stuck = productAlertEventRepository.findByStatusAndUpdatedAtBefore(
        EventStatus.SENDING, threshold);

    for (ProductAlertEvent event : stuck) {
      event.setAttempts(event.getAttempts() + 1);
      event.setStatus(EventStatus.FAILED);
      event.setLastError("SENDING timeout");
    }

    productAlertEventRepository.saveAll(stuck);
  }

}
