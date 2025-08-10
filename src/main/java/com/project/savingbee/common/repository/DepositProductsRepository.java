package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.DepositProducts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositProductsRepository extends JpaRepository<DepositProducts, String> {
}
