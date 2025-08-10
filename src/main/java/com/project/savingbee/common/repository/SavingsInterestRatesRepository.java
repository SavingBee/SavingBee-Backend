package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.SavingsInterestRates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavingsInterestRatesRepository extends JpaRepository<SavingsInterestRates, Long> {
}