package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.ProductAlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductAlertEventRepository extends JpaRepository<ProductAlertEvent, Long> {

  boolean existsByDedupeKey(String dedupeKey);
}
