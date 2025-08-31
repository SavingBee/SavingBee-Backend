package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.Cart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 장바구니 데이터 접근을 위한 Repository
 * 사용자의 관심 상품 관리 및 비교 분석에 활용
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    // 사용자별 장바구니 목록 조회 (페이징)
    Page<Cart> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // 은행명 필터링 + 페이징
    Page<Cart> findByUserIdAndBankNameContainingIgnoreCaseOrderByCreatedAtDesc(
        Long userId, String bankName, Pageable pageable);
    
    // 최근 담은 순 정렬
    List<Cart> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // 금리 높은 순 정렬
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId ORDER BY c.maxInterestRate DESC")
    List<Cart> findByUserIdOrderByMaxInterestRateDesc(@Param("userId") Long userId);
    
    // 특정 상품이 장바구니에 있는지 확인
    boolean existsByUserIdAndProductCodeAndProductType(
        Long userId, String productCode, Cart.ProductType productType);
    
    // 사용자별 장바구니 전체 개수
    long countByUserId(Long userId);
    
    // 사용자별 최고 금리
    @Query("SELECT MAX(c.maxInterestRate) FROM Cart c WHERE c.userId = :userId")
    Optional<BigDecimal> findMaxInterestRateByUserId(@Param("userId") Long userId);
    
    // 선택 삭제 (복수)
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.cartId IN :cartIds AND c.userId = :userId")
    int deleteByCartIdsAndUserId(@Param("cartIds") List<Long> cartIds, @Param("userId") Long userId);
    
    // 전체 비우기
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
    
    // 은행명별 필터링
    List<Cart> findByUserIdAndBankNameContainingIgnoreCase(Long userId, String bankName);
    
    // 상품 타입별 필터링
    List<Cart> findByUserIdAndProductType(Long userId, Cart.ProductType productType);
    
    // 상품 타입별 필터링 + 페이징 (최근순)
    Page<Cart> findByUserIdAndProductTypeOrderByCreatedAtDesc(Long userId, Cart.ProductType productType, Pageable pageable);
    
    // 상품 타입별 필터링 + 페이징 (금리순)  
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND c.productType = :productType ORDER BY c.maxInterestRate DESC")
    Page<Cart> findByUserIdAndProductTypeOrderByMaxInterestRateDesc(@Param("userId") Long userId, @Param("productType") Cart.ProductType productType, Pageable pageable);
    
    // 은행명 + 상품타입 필터링 + 페이징 (최근순)
    Page<Cart> findByUserIdAndBankNameContainingIgnoreCaseAndProductTypeOrderByCreatedAtDesc(
        Long userId, String bankName, Cart.ProductType productType, Pageable pageable);
        
    // 은행명 + 상품타입 필터링 + 페이징 (금리순)
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND UPPER(c.bankName) LIKE UPPER(CONCAT('%', :bankName, '%')) AND c.productType = :productType ORDER BY c.maxInterestRate DESC")
    Page<Cart> findByUserIdAndBankNameContainingIgnoreCaseAndProductTypeOrderByMaxInterestRateDesc(
        @Param("userId") Long userId, @Param("bankName") String bankName, @Param("productType") Cart.ProductType productType, Pageable pageable);
        
    // 금리순 정렬 + 페이징
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId ORDER BY c.maxInterestRate DESC")
    Page<Cart> findByUserIdOrderByMaxInterestRateDesc(@Param("userId") Long userId, Pageable pageable);
    
    // 은행명 필터링 + 금리순 정렬 + 페이징
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND UPPER(c.bankName) LIKE UPPER(CONCAT('%', :bankName, '%')) ORDER BY c.maxInterestRate DESC")
    Page<Cart> findByUserIdAndBankNameContainingIgnoreCaseOrderByMaxInterestRateDesc(
        @Param("userId") Long userId, @Param("bankName") String bankName, Pageable pageable);
}
