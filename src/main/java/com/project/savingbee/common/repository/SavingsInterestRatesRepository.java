package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.SavingsInterestRates;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavingsInterestRatesRepository extends JpaRepository<SavingsInterestRates, Long> {

  // 금융 상품 데이터 정보 존재 여부
  boolean existsByFinPrdtCdAndIntrRateTypeAndRsrvTypeAndSaveTrm(
      String finPrdtCd,
      String intrRateType,
      String rsrvType,
      Integer saveTrm
  );

  // 금융상품 번호로 상품찾기
  List<SavingsInterestRates> findByFinPrdtCd(String finPrdtCd);
}