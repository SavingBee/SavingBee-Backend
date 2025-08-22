package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.DepositProducts;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositProductsRepository extends JpaRepository<DepositProducts, String>,
    JpaSpecificationExecutor<DepositProducts> {

  // 마지막 스캔 이후 수정된 상품만 조회
  List<DepositProducts> findByUpdatedAtAfter(LocalDateTime since);

  // 금융상품코드 목록에 해당하는 상품 조회
  List<DepositProducts> findByFinPrdtCdIn(Collection<String> codes);

  // 상품명으로 검색 - 가입 가능한 상품만
  List<DepositProducts> findByFinPrdtNmContainingIgnoreCaseAndIsActiveTrue(String finPrdtNm);

  // 활성 상품을 최신 등록순으로 조회
  List<DepositProducts> findByIsActiveTrueOrderByCreatedAtDesc();

  
  // 활성 상품 조회 (추천 시스템용)
  List<DepositProducts> findByIsActiveTrue();


//  /**
//   * 필터링 사용 QueryDSL
//   */
//  // 연관관계 함께 로딩하는 메서드
//  @Query("SELECT DISTINCT dp FROM DepositProducts dp " +
//      "LEFT JOIN FETCH dp.financialCompany " +
//      "LEFT JOIN FETCH dp.interestRates " +
//      "WHERE dp.isActive = true")
//  List<DepositProducts> findAllActiveWithRelations();
//
//
//  @Query("SELECT DISTINCT dp FROM DepositProducts dp " +
//      "LEFT JOIN FETCH dp.financialCompany " +
//      "LEFT JOIN FETCH dp.interestRates " +
//      "WHERE dp.isActive = true AND dp.finCoNo IN :finCoNos")
//  List<DepositProducts> findActiveByFinCoNoWithRelations(@Param("finCoNos") List<String> finCoNos);
//
//  // 우대조건 없는 상품들 조회
//  @Query("SELECT d FROM DepositProducts d WHERE " +
//      "d.spclCnd IN ('없음', '우대조건 없음', '해당사항 없음', '해당사항없음') OR " +
//      "d.spclCnd IS NULL OR " +
//      "TRIM(d.spclCnd) = ''")
//  List<DepositProducts> findProductsWithoutPreferentialConditions();
//
//  // 특정 키워드 포함 상품들 조회
//  @Query("SELECT DISTINCT d FROM DepositProducts d WHERE d.spclCnd LIKE %:keyword%")
//  List<DepositProducts> findProductsWithKeyword(@Param("keyword") String keyword);
//
//  // 금융회사 타입별 조회
//  @Query("SELECT d FROM DepositProducts d " +
//      "JOIN d.financialCompany fc " +
//      "WHERE fc.orgTypeCode IN :orgTypeCodes")
//  List<DepositProducts> findByFinancialCompanyTypes(
//      @Param("orgTypeCodes") List<String> orgTypeCodes);
//
//  // 가입제한별 조회
//  @Query("SELECT d FROM DepositProducts d WHERE d.joinDeny IN :joinDenyTypes")
//  List<DepositProducts> findByJoinDenyTypes(@Param("joinDenyTypes") List<String> joinDenyTypes);
//
//  // 금리 조건과 저축기간을 만족하는 상품들 조회
//  @Query("SELECT DISTINCT d FROM DepositProducts d " +
//      "JOIN d.interestRates ir " +
//      "WHERE (:saveTrms IS NULL OR ir.saveTrm IN :saveTrms) " +
//      "AND (:minRate IS NULL OR ir.intrRate2 >= :minRate) " +
//      "AND (:maxRate IS NULL OR ir.intrRate2 <= :maxRate) " +
//      "AND (:intRateTypes IS NULL OR ir.intrRateType IN :intRateTypes)")
//  List<DepositProducts> findProductsWithRateAndTerms(
//      @Param("saveTrms") List<Integer> saveTrms,
//      @Param("minRate") BigDecimal minRate,
//      @Param("maxRate") BigDecimal maxRate,
//      @Param("intRateTypes") List<String> intRateTypes
//  );
//
//  // 저축금액 범위 조건을 만족하는 상품들 조회
//  @Query("SELECT d FROM DepositProducts d WHERE " +
//      "(:saveAmount IS NULL OR " +
//      " (d.maxLimit IS NULL OR d.maxLimit >= :saveAmount) AND " +
//      " (d.minAmount IS NULL OR d.minAmount <= :saveAmount))")
//  List<DepositProducts> findProductsWithinAmountRange(@Param("saveAmount") BigDecimal saveAmount);
}