package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.DepositInterestRates;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositInterestRatesRepository extends JpaRepository<DepositInterestRates, Long> {

  // 금리 옵션이 이미 존재하는지 확인
  boolean existsByFinPrdtCdAndIntrRateTypeAndSaveTrm(String finPrdtCd, String intrRateType,
      Integer saveTrm);

  // 해당 상품의 금리 옵션 찾기
  List<DepositInterestRates> findByFinPrdtCd(String finPrdtCd);
}