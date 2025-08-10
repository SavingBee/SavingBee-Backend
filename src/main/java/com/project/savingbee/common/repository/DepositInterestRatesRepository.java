package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.DepositInterestRates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositInterestRatesRepository extends JpaRepository<DepositInterestRates, Long> {
}