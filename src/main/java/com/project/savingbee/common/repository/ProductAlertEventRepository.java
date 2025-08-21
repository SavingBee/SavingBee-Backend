package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.ProductAlertEvent;
import com.project.savingbee.common.entity.ProductAlertEvent.EventStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductAlertEventRepository extends JpaRepository<ProductAlertEvent, Long> {

  boolean existsByDedupeKey(String dedupeKey);

  // 알림 발송 후보 조회(status = READY, FAILED && sendNotBefore <= now)
  Page<ProductAlertEvent> findByStatusInAndSendNotBeforeLessThanEqual(
      List<EventStatus> status, LocalDateTime now, Pageable pageable);

  // SENDING timeout 확인용
  List<ProductAlertEvent> findByStatusAndUpdatedAtBefore(
      EventStatus eventStatus, LocalDateTime before);
}
