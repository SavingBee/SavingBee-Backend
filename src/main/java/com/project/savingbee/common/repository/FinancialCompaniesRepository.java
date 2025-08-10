package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.FinancialCompanies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialCompaniesRepository extends JpaRepository<FinancialCompanies, String> {
}