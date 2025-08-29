package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.UserProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 사용자 보유 예적금 상품 데이터 접근을 위한 Repository
 * 추천 시스템의 기준 데이터 조회 및 관리
 */
@Repository
public interface UserProductRepository extends JpaRepository<UserProduct, Long> {

    // 사용자별 보유 상품 목록 조회
    List<UserProduct> findByUserIdAndIsActiveTrue(Long userId);
    
    // 사용자별 보유 상품 목록 조회 (페이징)
    Page<UserProduct> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);
    
    // 사용자별 전체 보유 상품 목록 (비활성 포함)
    List<UserProduct> findByUserId(Long userId);
    
    // 특정 상품 타입별 조회
    List<UserProduct> findByUserIdAndProductTypeAndIsActiveTrue(Long userId, UserProduct.ProductType productType);
    
    // 만기일 기준 조회 (알림용)
    @Query("SELECT up FROM UserProduct up WHERE up.isActive = true AND up.maturityDate BETWEEN :startDate AND :endDate")
    List<UserProduct> findByMaturityDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // D-30, D-7, D-1 알림 대상 조회
    @Query("SELECT up FROM UserProduct up WHERE up.isActive = true AND up.maturityDate = :targetDate")
    List<UserProduct> findByMaturityDate(@Param("targetDate") LocalDate targetDate);
    
    // 사용자별 만기일 기준 조회
    @Query("SELECT up FROM UserProduct up WHERE up.userId = :userId AND up.isActive = true AND up.maturityDate = :targetDate")
    List<UserProduct> findByUserIdAndMaturityDateAndIsActiveTrue(@Param("userId") Long userId, @Param("targetDate") LocalDate targetDate);
    
    // 사용자별 활성 상품 개수
    long countByUserIdAndIsActiveTrue(Long userId);
    
    // 은행별 보유 상품 조회
    List<UserProduct> findByUserIdAndBankNameContainingIgnoreCaseAndIsActiveTrue(Long userId, String bankName);
}