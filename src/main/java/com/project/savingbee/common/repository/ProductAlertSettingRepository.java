package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.ProductAlertSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductAlertSettingRepository extends JpaRepository<ProductAlertSetting, Long> {

}