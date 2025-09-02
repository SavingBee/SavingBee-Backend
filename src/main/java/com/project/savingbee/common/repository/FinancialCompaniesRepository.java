package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.FinancialCompanies;
import com.project.savingbee.productCompare.dto.PageResponseDto.MatchedBank;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialCompaniesRepository extends JpaRepository<FinancialCompanies, String> {

  // 키워드가 포함되는 금융회사명 찾기
  List<MatchedBank> findByKorCoNmContainingOrderByFinCoNo(String keyword);

  FinancialCompanies findByFinCoNo(String finCoNo);
}