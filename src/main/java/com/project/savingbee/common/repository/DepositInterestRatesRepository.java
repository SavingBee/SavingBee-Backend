package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.DepositInterestRates;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositInterestRatesRepository extends JpaRepository<DepositInterestRates, Long> {

  // 금리 옵션이 이미 존재하는지 확인
  boolean existsByFinPrdtCdAndIntrRateTypeAndSaveTrm(String finPrdtCd, String intrRateType,
      Integer saveTrm);

  // 해당 상품의 금리 옵션 찾기
  List<DepositInterestRates> findByFinPrdtCd(String finPrdtCd);

  // 예치 기간 + 이자계산방식 필터링 후 최고 금리
  Optional<DepositInterestRates>
  findTopByFinPrdtCdAndSaveTrmAndIntrRateTypeInOrderByIntrRate2DescIntrRateDesc(
          String finPrdtCd, Integer saveTrm, Collection<String> intrRateTypes);

  // 이자계산방식 상관없이 예치 기간에 따른 최고 금리
  Optional<DepositInterestRates>
  findTopByFinPrdtCdAndSaveTrmOrderByIntrRate2DescIntrRateDesc(String finPrdtCd, Integer saveTrm);

  // dedupeKey에 들어갈 버전 계산용(이자계산방식 포함)
  Optional<DepositInterestRates> findTopByFinPrdtCdAndSaveTrmAndIntrRateTypeInOrderByUpdatedAtDesc(
      String finPrdtCd, Integer saveTrm, Collection<String> intrRateTypes);

  // dedupeKey에 들어갈 버전 계산용(이자계산방식 미포함)
  Optional<DepositInterestRates>
  findTopByFinPrdtCdAndSaveTrmOrderByUpdatedAtDesc(String finPrdtCd, Integer saveTrm);

  // 알람 후보 수집용(금리 옵션 테이블에서 since 이후 변경된 상품 코드 목록)
  @Query("select distinct r.finPrdtCd from DepositInterestRates r where r.updatedAt > :since")
  List<String> findDistinctFinPrdtCdUpdatedAfter(@Param("since") LocalDateTime since);
  
  // 특정 상품의 최고 금리 조회 (추천 시스템용)
  @Query("SELECT MAX(COALESCE(d.intrRate2, d.intrRate)) FROM DepositInterestRates d WHERE d.finPrdtCd = :productCode")
  Optional<BigDecimal> findMaxInterestRateByProductCode(@Param("productCode") String productCode);
  
  // 특정 조건의 상품들 중 최고 금리 상품들 조회
  @Query("SELECT d FROM DepositInterestRates d WHERE d.finPrdtCd IN :productCodes ORDER BY COALESCE(d.intrRate2, d.intrRate) DESC")
  List<DepositInterestRates> findTopRatesByProductCodes(@Param("productCodes") List<String> productCodes);
  
  // 예치 기간이 일치한 금리 정보 조회(상품코드 순)
  List<DepositInterestRates> findAllBySaveTrmOrderByFinPrdtCd(Integer saveTrm);

  // 상품코드 + 예치 기간으로 두 상품 정보 조회(상품 비교용)
  List<DepositInterestRates> findAllByFinPrdtCdInAndSaveTrm(List<String> ids, Integer saveTrm);
}