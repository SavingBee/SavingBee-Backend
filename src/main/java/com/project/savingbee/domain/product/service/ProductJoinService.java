package com.project.savingbee.domain.product.service;

import com.project.savingbee.common.entity.*;
import com.project.savingbee.common.repository.*;
import com.project.savingbee.domain.cart.service.CartService;
import com.project.savingbee.domain.product.dto.ProductJoinRequestDTO;
import com.project.savingbee.domain.product.dto.ProductJoinResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 상품 가입 서비스
 * 예적금 상품 가입 처리 및 보유 상품으로 등록
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductJoinService {

    private final UserProductRepository userProductRepository;
    private final DepositProductsRepository depositProductsRepository;
    private final SavingsProductsRepository savingsProductsRepository;
    private final DepositInterestRatesRepository depositInterestRatesRepository;
    private final SavingsInterestRatesRepository savingsInterestRatesRepository;
    private final CartService cartService;

    /**
     * 상품 가입 처리
     * 1. 상품 정보 조회
     * 2. 금리 정보 조회 (사용자가 지정하지 않은 경우)
     * 3. 만기일 계산
     * 4. UserProduct 생성 및 저장
     * 5. 장바구니에서 제거 (선택사항)
     */
    @Transactional
    public ProductJoinResponseDTO joinProduct(Long userId, ProductJoinRequestDTO request) {
        // 1. 상품 정보 조회
        String bankName;
        String productName;
        BigDecimal interestRate = request.getInterestRate();

        if (request.getProductType() == UserProduct.ProductType.DEPOSIT) {
            // 예금 상품 조회
            DepositProducts depositProduct = depositProductsRepository.findById(request.getProductCode())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예금 상품입니다: " + request.getProductCode()));
            
            bankName = depositProduct.getFinancialCompany().getKorCoNm(); // 은행명
            productName = depositProduct.getFinPrdtNm(); // 상품명
            
            // 금리 정보가 없는 경우 자동 조회
            if (interestRate == null) {
                interestRate = getDepositInterestRate(request.getProductCode(), request.getTermMonths());
            }
            
        } else {
            // 적금 상품 조회
            SavingsProducts savingsProduct = savingsProductsRepository.findById(request.getProductCode())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 적금 상품입니다: " + request.getProductCode()));
            
            bankName = savingsProduct.getFinancialCompany().getKorCoNm(); // 은행명
            productName = savingsProduct.getFinPrdtNm(); // 상품명
            
            // 금리 정보가 없는 경우 자동 조회
            if (interestRate == null) {
                interestRate = getSavingsInterestRate(request.getProductCode(), request.getTermMonths());
            }
        }

        // 2. 만기일 계산
        LocalDate maturityDate = request.getJoinDate().plusMonths(request.getTermMonths());

        // 3. UserProduct 생성 및 저장
        UserProduct userProduct = UserProduct.builder()
                .userId(userId)
                .bankName(bankName)
                .productName(productName)
                .productType(request.getProductType())
                .interestRate(interestRate)
                .depositAmount(request.getDepositAmount())
                .termMonths(request.getTermMonths())
                .joinDate(request.getJoinDate())
                .maturityDate(maturityDate)
                .specialConditions(request.getSpecialConditions())
                .isActive(true)
                .build();

        UserProduct savedProduct = userProductRepository.save(userProduct);
        log.info("상품 가입 완료: userId={}, productCode={}, productName={}", 
                userId, request.getProductCode(), productName);

        // 4. 장바구니에서 제거 (선택사항)
        if (request.getCartId() != null) {
            try {
                cartService.removeFromCart(userId, request.getCartId());
                log.info("장바구니에서 가입 완료된 상품 제거: cartId={}", request.getCartId());
            } catch (Exception e) {
                log.warn("장바구니 상품 제거 실패: cartId={}, error={}", request.getCartId(), e.getMessage());
                // 장바구니 제거 실패해도 가입 자체는 성공 처리
            }
        }

        return ProductJoinResponseDTO.from(savedProduct);
    }

    /**
     * 예금 상품의 금리 조회
     */
    private BigDecimal getDepositInterestRate(String productCode, Integer termMonths) {
        // 해당 상품의 기간별 금리 조회 (우대금리 우선)
        Optional<DepositInterestRates> interestRateOpt = depositInterestRatesRepository
                .findTopByFinPrdtCdAndSaveTrmOrderByIntrRate2DescIntrRateDesc(productCode, termMonths);
        
        if (interestRateOpt.isPresent()) {
            DepositInterestRates interestRate = interestRateOpt.get();
            // 우대금리가 있으면 우대금리, 없으면 기본금리
            BigDecimal rate = interestRate.getIntrRate2();
            return rate != null ? rate : interestRate.getIntrRate();
        }
        
        // 기본값 (실제로는 에러를 던지는 것이 좋을 수 있음)
        log.warn("금리 정보를 찾을 수 없습니다: productCode={}, termMonths={}", productCode, termMonths);
        return BigDecimal.valueOf(2.0); // 기본 2% 금리
    }

    /**
     * 적금 상품의 금리 조회
     */
    private BigDecimal getSavingsInterestRate(String productCode, Integer termMonths) {
        // 해당 상품의 기간별 금리 조회 (우대금리 우선)
        Optional<SavingsInterestRates> interestRateOpt = savingsInterestRatesRepository
                .findTopByFinPrdtCdAndSaveTrmOrderByIntrRate2DescIntrRateDesc(productCode, termMonths);
        
        if (interestRateOpt.isPresent()) {
            SavingsInterestRates interestRate = interestRateOpt.get();
            // 우대금리가 있으면 우대금리, 없으면 기본금리
            BigDecimal rate = interestRate.getIntrRate2();
            return rate != null ? rate : interestRate.getIntrRate();
        }
        
        // 기본값 (실제로는 에러를 던지는 것이 좋을 수 있음)
        log.warn("금리 정보를 찾을 수 없습니다: productCode={}, termMonths={}", productCode, termMonths);
        return BigDecimal.valueOf(2.5); // 기본 2.5% 금리
    }
}
