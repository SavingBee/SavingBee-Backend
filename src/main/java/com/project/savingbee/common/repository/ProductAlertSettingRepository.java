package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.ProductAlertSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ProductAlertSettingRepository extends JpaRepository<ProductAlertSetting, Long> {

  Optional<ProductAlertSetting> findByUserId(Long userId);

  void deleteByUserId(Long userId);
}