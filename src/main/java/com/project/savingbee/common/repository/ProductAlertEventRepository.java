package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.ProductAlertEvent;
import com.project.savingbee.common.entity.ProductAlertEvent.EventStatus;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductAlertEventRepository extends JpaRepository<ProductAlertEvent, Long> {

  boolean existsByDedupeKey(String dedupeKey);

  // 알림 발송 후보 조회(status = READY, sendNotBefore <= now)
  Page<ProductAlertEvent> findByStatusAndSendNotBeforeLessThanEqual(
      EventStatus status, LocalDateTime now, Pageable pageable);
}
