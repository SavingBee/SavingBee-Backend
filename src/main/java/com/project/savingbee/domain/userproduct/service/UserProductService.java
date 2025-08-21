package com.project.savingbee.domain.userproduct.service;

import com.project.savingbee.common.entity.UserProduct;
import com.project.savingbee.common.repository.UserProductRepository;
import com.project.savingbee.domain.userproduct.dto.UserProductRequestDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductResponseDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 사용자 보유 예적금 상품 관리 서비스
 * 실제 보유 상품 CRUD 및 추천 시스템의 기준 데이터 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserProductService {
    
    private final UserProductRepository userProductRepository;
    
    /**
     * 사용자 보유 상품 등록
     * 예) "KB국민은행 / 정기예금 / 가입 기간 12개월 / 금리 3.2% / 가입일 2025-10-01 / 만기일 2026-10-01"
     */
    @Transactional
    public UserProductResponseDTO registerUserProduct(Long userId, UserProductRequestDTO request) {
        // 만기일 검증
        if (request.getMaturityDate().isBefore(request.getJoinDate())) {
            throw new IllegalArgumentException("만기일은 가입일보다 이후여야 합니다.");
        }
        
        UserProduct userProduct = UserProduct.builder()
                .userId(userId)
                .bankName(request.getBankName())
                .productName(request.getProductName())
                .productType(request.getProductType())
                .interestRate(request.getInterestRate())
                .depositAmount(request.getDepositAmount())
                .termMonths(request.getTermMonths())
                .joinDate(request.getJoinDate())
                .maturityDate(request.getMaturityDate())
                .specialConditions(request.getSpecialConditions())
                .isActive(true)
                .build();
        
        UserProduct savedProduct = userProductRepository.save(userProduct);
        log.info("사용자 보유 상품 등록: userId={}, productName={}", userId, request.getProductName());
        
        return UserProductResponseDTO.from(savedProduct);
    }
    
    /**
     * 사용자 보유 상품 목록 조회
     */
    public List<UserProductResponseDTO> getUserProducts(Long userId, UserProductRequestDTO request) {
        List<UserProduct> products;
        
        // 필터링 조건에 따른 조회
        if (request.getFilterProductType() != null) {
            products = userProductRepository.findByUserIdAndProductTypeAndIsActiveTrue(
                    userId, request.getFilterProductType());
        } else if (request.getFilterBankName() != null && !request.getFilterBankName().trim().isEmpty()) {
            products = userProductRepository.findByUserIdAndBankNameContainingIgnoreCaseAndIsActiveTrue(
                    userId, request.getFilterBankName());
        } else if (request.getIncludeInactive()) {
            products = userProductRepository.findByUserId(userId);
        } else {
            products = userProductRepository.findByUserIdAndIsActiveTrue(userId);
        }
        
        return UserProductResponseDTO.fromList(products);
    }
    
    /**
     * 사용자 보유 상품 수정
     */
    @Transactional
    public UserProductResponseDTO updateUserProduct(Long userId, Long userProductId, UserProductRequestDTO request) {
        UserProduct userProduct = userProductRepository.findById(userProductId)
                .orElseThrow(() -> new IllegalArgumentException("보유 상품을 찾을 수 없습니다."));
        
        if (!userProduct.getUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        
        // 업데이트
        userProduct.setBankName(request.getBankName());
        userProduct.setProductName(request.getProductName());
        userProduct.setProductType(request.getProductType());
        userProduct.setInterestRate(request.getInterestRate());
        userProduct.setDepositAmount(request.getDepositAmount());
        userProduct.setTermMonths(request.getTermMonths());
        userProduct.setJoinDate(request.getJoinDate());
        userProduct.setMaturityDate(request.getMaturityDate());
        userProduct.setSpecialConditions(request.getSpecialConditions());
        
        UserProduct updatedProduct = userProductRepository.save(userProduct);
        log.info("사용자 보유 상품 수정: userId={}, userProductId={}", userId, userProductId);
        
        return UserProductResponseDTO.from(updatedProduct);
    }
    
    /**
     * 사용자 보유 상품 삭제 (비활성화)
     */
    @Transactional
    public void deleteUserProduct(Long userId, Long userProductId) {
        UserProduct userProduct = userProductRepository.findById(userProductId)
                .orElseThrow(() -> new IllegalArgumentException("보유 상품을 찾을 수 없습니다."));
        
        if (!userProduct.getUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        
        userProduct.setIsActive(false);
        userProductRepository.save(userProduct);
        log.info("사용자 보유 상품 삭제: userId={}, userProductId={}", userId, userProductId);
    }
    
    /**
     * 단일 보유 상품 조회
     */
    public UserProductResponseDTO getUserProduct(Long userId, Long userProductId) {
        UserProduct userProduct = userProductRepository.findById(userProductId)
                .orElseThrow(() -> new IllegalArgumentException("보유 상품을 찾을 수 없습니다."));
        
        if (!userProduct.getUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        
        return UserProductResponseDTO.from(userProduct);
    }
    
    /**
     * 만기 임박 상품 조회 (D-30, D-7, D-1)
     */
    public List<UserProductResponseDTO> getMaturityProducts(int daysBefore) {
        LocalDate targetDate = LocalDate.now().plusDays(daysBefore);
        List<UserProduct> products = userProductRepository.findByMaturityDate(targetDate);
        return UserProductResponseDTO.fromList(products);
    }
    
    /**
     * 사용자 보유 상품 요약 정보
     */
    public UserProductSummaryDTO getUserProductSummary(Long userId) {
        List<UserProduct> allProducts = userProductRepository.findByUserId(userId);
        List<UserProduct> activeProducts = userProductRepository.findByUserIdAndIsActiveTrue(userId);
        
        long nearMaturityCount = activeProducts.stream()
                .mapToLong(product -> {
                    LocalDate now = LocalDate.now();
                    long daysToMaturity = now.until(product.getMaturityDate()).getDays();
                    return daysToMaturity <= 30 && daysToMaturity >= 0 ? 1 : 0;
                })
                .sum();
        
        return UserProductSummaryDTO.builder()
                .totalProducts(allProducts.size())
                .activeProducts(activeProducts.size())
                .nearMaturityProducts(nearMaturityCount)
                .totalDepositAmount(activeProducts.stream()
                        .map(UserProduct::getDepositAmount)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
                .averageInterestRate(activeProducts.stream()
                        .map(UserProduct::getInterestRate)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                        .divide(new java.math.BigDecimal(Math.max(1, activeProducts.size())), 2, java.math.BigDecimal.ROUND_HALF_UP))
                .build();
    }
}
