package com.project.savingbee.domain.userproduct.service;

import com.project.savingbee.common.entity.UserProduct;
import com.project.savingbee.common.repository.UserProductRepository;
import com.project.savingbee.domain.user.entity.UserEntity;
import com.project.savingbee.domain.user.repository.UserRepository;
import com.project.savingbee.domain.userproduct.dto.UserProductPageResponseDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductRequestDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductResponseDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;

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
    private final UserRepository userRepository;
    
    /**
     * 사용자 보유 상품 등록 (username으로)
     */
    @Transactional
    public Long addUserProduct(UserProductRequestDTO request) {
        // username으로 userId 조회
        Long userId = getUserIdByUsername(request.getUsername());
        
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
        
        return savedProduct.getUserProductId();
    }
    
    /**
     * 사용자 보유 상품 등록 (기존 메서드)
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
     * 사용자 보유 상품 목록 조회 (페이징)
     */
    public UserProductPageResponseDTO getUserProducts(String username, Pageable pageable) {
        // username으로 userId 조회
        Long userId = getUserIdByUsername(username);
        
        Page<UserProduct> productPage = userProductRepository.findByUserIdAndIsActiveTrue(userId, pageable);
        
        List<UserProductResponseDTO> content = UserProductResponseDTO.fromList(productPage.getContent());
        
        return UserProductPageResponseDTO.builder()
                .content(content)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .build();
    }
    
    /**
     * 사용자 보유 상품 목록 조회 (기존 메서드)
     */
    public List<UserProductResponseDTO> getUserProductsList(Long userId, UserProductRequestDTO request) {
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
     * 사용자 보유 상품 수정 (username으로)
     */
    @Transactional
    public void updateUserProduct(Long userProductId, UserProductRequestDTO request) {
        Long userId = getUserIdByUsername(request.getUsername());
        
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
        
        userProductRepository.save(userProduct);
        log.info("사용자 보유 상품 수정: userId={}, userProductId={}", userId, userProductId);
    }
    
    /**
     * 사용자 보유 상품 수정 (기존 메서드)
     */
    @Transactional
    public UserProductResponseDTO updateUserProductById(Long userId, Long userProductId, UserProductRequestDTO request) {
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
     * 사용자 보유 상품 삭제 (username으로)
     */
    @Transactional
    public void deleteUserProduct(Long userProductId, String username) {
        Long userId = getUserIdByUsername(username);
        
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
     * 사용자 보유 상품 삭제 (기존 메서드)
     */
    @Transactional
    public void deleteUserProductById(Long userId, Long userProductId) {
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
     * 단일 보유 상품 조회 (username으로)
     */
    public UserProductResponseDTO getUserProduct(Long userProductId, String username) {
        Long userId = getUserIdByUsername(username);
        
        UserProduct userProduct = userProductRepository.findById(userProductId)
                .orElseThrow(() -> new IllegalArgumentException("보유 상품을 찾을 수 없습니다."));
        
        if (!userProduct.getUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        
        return UserProductResponseDTO.from(userProduct);
    }
    
    /**
     * 단일 보유 상품 조회 (기존 메서드)
     */
    public UserProductResponseDTO getUserProductById(Long userId, Long userProductId) {
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
    public List<UserProductResponseDTO> getMaturityProducts(int daysBefore, Long userId) {
        LocalDate targetDate = LocalDate.now().plusDays(daysBefore);
        List<UserProduct> products = userProductRepository.findByUserIdAndMaturityDateAndIsActiveTrue(userId, targetDate);
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
                        .divide(new java.math.BigDecimal(Math.max(1, activeProducts.size())), 2, RoundingMode.HALF_UP))
                .build();
    }
    
    /**
     * username으로 userId 조회하는 헬퍼 메서드
     */
    private Long getUserIdByUsername(String username) {
        UserEntity userEntity = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
        return userEntity.getUserId();
    }
}
