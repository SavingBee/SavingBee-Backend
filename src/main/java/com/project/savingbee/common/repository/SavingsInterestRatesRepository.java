package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.SavingsInterestRates;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
  
  // 예치 기간 + 이자계산방식 + 적립방식 필터링 후 최고 금리
  Optional<SavingsInterestRates>
  findTopByFinPrdtCdAndSaveTrmAndIntrRateTypeInAndRsrvTypeInOrderByIntrRate2DescIntrRateDesc(
      String code, Integer saveTrm, Collection<String> intrTypes, Collection<String> rsrvTypes);

  // 예치 기간 + 이자계산방식 필터링 후 최고 금리
  Optional<SavingsInterestRates>
  findTopByFinPrdtCdAndSaveTrmAndIntrRateTypeInOrderByIntrRate2DescIntrRateDesc(
      String code, Integer saveTrm, Collection<String> intrTypes);

  // 예치 기간 + 적립방식 필터링 후 최고 금리
  Optional<SavingsInterestRates>
  findTopByFinPrdtCdAndSaveTrmAndRsrvTypeInOrderByIntrRate2DescIntrRateDesc(
      String code, Integer saveTrm, Collection<String> rsrvTypes);

  // 이자계산방식, 적립방식 상관없이 예치 기간에 따른 최고 금리
  Optional<SavingsInterestRates>
  findTopByFinPrdtCdAndSaveTrmOrderByIntrRate2DescIntrRateDesc(String code, Integer saveTrm);

  // dedupeKey에 들어갈 버전 계산용(이자계산방식, 적립방식 포함)
  Optional<SavingsInterestRates>
  findTopByFinPrdtCdAndSaveTrmAndIntrRateTypeInAndRsrvTypeInOrderByUpdatedAtDesc(
      String finPrdtCd, Integer saveTrm, Collection<String> intrTypes, Collection<String> rsrvTypes);

  // dedupeKey에 들어갈 버전 계산용(이자계산방식 포함)
  Optional<SavingsInterestRates>
  findTopByFinPrdtCdAndSaveTrmAndIntrRateTypeInOrderByUpdatedAtDesc(
      String finPrdtCd, Integer saveTrm, Collection<String> intrTypes);

  // dedupeKey에 들어갈 버전 계산용(적립방식 포함)
  Optional<SavingsInterestRates> findTopByFinPrdtCdAndSaveTrmAndRsrvTypeInOrderByUpdatedAtDesc(
      String finPrdtCd, Integer saveTrm, Collection<String> rsrvTypes);

  // dedupeKey에 들어갈 버전 계산용(모두 미포함)
  Optional<SavingsInterestRates> findTopByFinPrdtCdAndSaveTrmOrderByUpdatedAtDesc(
      String finPrdtCd, Integer saveTrm);

  // 알람 후보 수집용(금리 옵션 테이블에서 since 이후 변경된 상품 코드 목록)
  @Query("select distinct r.finPrdtCd from SavingsInterestRates r where r.updatedAt > :since")
  List<String> findDistinctFinPrdtCdUpdatedAfter(@Param("since") LocalDateTime since);
  
  // 특정 상품의 최고 금리 조회 (추천 시스템용)
  @Query("SELECT MAX(COALESCE(s.intrRate2, s.intrRate)) FROM SavingsInterestRates s WHERE s.finPrdtCd = :productCode")
  Optional<BigDecimal> findMaxInterestRateByProductCode(@Param("productCode") String productCode);
  
  // 특정 조건의 상품들 중 최고 금리 상품들 조회
  @Query("SELECT s FROM SavingsInterestRates s WHERE s.finPrdtCd IN :productCodes ORDER BY COALESCE(s.intrRate2, s.intrRate) DESC")
  List<SavingsInterestRates> findTopRatesByProductCodes(@Param("productCodes") List<String> productCodes);
  
  // 예치 기간이 일치한 금리 정보 조회(상품코드 순)
  List<SavingsInterestRates> findAllBySaveTrmOrderByFinPrdtCd(Integer saveTrm);

  // 상품코드 + 이자계산방식 + 기간으로 두 상품의 금리 정보 조회(상품 비교용)
  List<SavingsInterestRates> findAllByFinPrdtCdInAndIntrRateTypeAndSaveTrm(
      List<String> finPrdtCd, String intrRateType, Integer saveTrm);
}