package com.project.savingbee.domain.cart.service;

import com.project.savingbee.common.entity.Cart;
import com.project.savingbee.common.entity.DepositInterestRates;
import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.SavingsInterestRates;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.repository.CartRepository;
import com.project.savingbee.common.repository.DepositInterestRatesRepository;
import com.project.savingbee.common.repository.DepositProductsRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.domain.cart.dto.CartPageResponseDTO;
import com.project.savingbee.domain.cart.dto.CartRequestDTO;
import com.project.savingbee.domain.cart.dto.CartResponseDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장바구니 관리 서비스 관심 상품 담기, 조회, 삭제 기능 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CartService {

  private final CartRepository cartRepository;
  private final DepositProductsRepository depositRepository;
  private final SavingsProductsRepository savingsRepository;
  private final DepositInterestRatesRepository depositRatesRepository;
  private final SavingsInterestRatesRepository savingsRatesRepository;

  /**
   * 1. 목록조회 (필터/페이징): 사용자의 장바구니 상품 목록 조회 - 은행명 필터링 지원 - 페이징 처리
   */
  public CartPageResponseDTO getCartItems(Long userId, CartRequestDTO request) {
    log.info("=== CartService.getCartItems Debug ===");
    log.info("Input parameters: userId={}, bankName={}, page={}, size={}",
        userId, request.getBankName(), request.getPage(), request.getSize());

    Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
    Page<Cart> cartPage;

    String bankName = request.getBankName();
    boolean hasBankFilter = bankName != null && !bankName.trim().isEmpty();
    log.info("Bank filter active: {}, bankName: '{}'", hasBankFilter, bankName);

    // 은행명 필터링 여부에 따른 쿼리 선택
    if (hasBankFilter) {
      log.info("Executing filtered query with bankName: {}", bankName);
      // 은행명 필터링
      cartPage = cartRepository.findByUserIdAndBankNameContainingIgnoreCaseOrderByCreatedAtDesc(
          userId, bankName, pageable);
    } else {
      log.info("Executing unfiltered query for userId: {}", userId);
      // 필터링 없음 - 기본 정렬(최근 담은 순)
      cartPage = cartRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    log.info("Query result: totalElements={}, totalPages={}, currentPage={}",
        cartPage.getTotalElements(), cartPage.getTotalPages(), cartPage.getNumber());
    log.info("Cart items found: {}", cartPage.getContent().size());

    // 각 Cart 항목의 상세 정보 로깅
    cartPage.getContent().forEach(cart -> {
      log.info("Cart item: id={}, userId={}, productCode={}, bankName={}, productName={}",
          cart.getCartId(), cart.getUserId(), cart.getProductCode(),
          cart.getBankName(), cart.getProductName());
    });

    List<CartResponseDTO> content = cartPage.getContent().stream()
        .map(CartResponseDTO::from)
        .collect(Collectors.toList());

    return CartPageResponseDTO.builder()
        .content(content)
        .page(cartPage.getNumber())
        .size(cartPage.getSize())
        .totalElements(cartPage.getTotalElements())
        .totalPages(cartPage.getTotalPages())
        .first(cartPage.isFirst())
        .last(cartPage.isLast())
        .build();
  }

  /**
   * 2. 담기: 상품을 장바구니에 추가
   */
  @Transactional
  public CartResponseDTO addToCart(Long userId, CartRequestDTO request) {
    // 중복 체크
    if (cartRepository.existsByUserIdAndProductCodeAndProductType(
        userId, request.getProductCode(), request.getProductType())) {
      throw new IllegalArgumentException("이미 장바구니에 담긴 상품입니다.");
    }

    // 상품 정보 조회
    ProductInfo productInfo = getProductInfo(request.getProductCode(), request.getProductType());

    Cart cart = Cart.builder()
        .userId(userId)
        .productCode(request.getProductCode())
        .productType(request.getProductType())
        .bankName(productInfo.getBankName())
        .productName(productInfo.getProductName())
        .maxInterestRate(productInfo.getMaxInterestRate())
        .termMonths(productInfo.getTermMonths())
        .build();

    Cart savedCart = cartRepository.save(cart);
    log.info("장바구니에 상품 추가: userId={}, productCode={}", userId, request.getProductCode());

    return CartResponseDTO.from(savedCart);
  }

  /**
   * 3. 삭제(단건): 장바구니에서 해당 항목 제거
   */
  @Transactional
  public void removeFromCart(Long userId, Long cartId) {
    Cart cart = cartRepository.findById(cartId)
        .orElseThrow(() -> new IllegalArgumentException("장바구니 항목을 찾을 수 없습니다."));

    if (!cart.getUserId().equals(userId)) {
      throw new IllegalArgumentException("권한이 없습니다.");
    }

    cartRepository.delete(cart);
    log.info("장바구니에서 상품 삭제: userId={}, cartId={}", userId, cartId);
  }

  /**
   * 4. 선택 삭제(복수): 선택 항목 일괄 삭제
   */
  @Transactional
  public int removeMultipleFromCart(Long userId, CartRequestDTO request) {
    int deletedCount = cartRepository.deleteByCartIdsAndUserId(request.getCartIds(), userId);
    log.info("장바구니에서 복수 상품 삭제: userId={}, count={}", userId, deletedCount);
    return deletedCount;
  }

  /**
   * 5. 전체 비우기: 장바구니 초기화
   */
  @Transactional
  public int clearCart(Long userId) {
    int deletedCount = cartRepository.deleteAllByUserId(userId);
    log.info("장바구니 전체 비우기: userId={}, count={}", userId, deletedCount);
    return deletedCount;
  }


  // 헬퍼 메서드들
  private ProductInfo getProductInfo(String productCode, Cart.ProductType productType) {
    if (productType == Cart.ProductType.DEPOSIT) {
      DepositProducts product = depositRepository.findById(productCode)
          .orElseThrow(() -> new IllegalArgumentException("예금 상품을 찾을 수 없습니다."));

      BigDecimal maxRate = depositRatesRepository.findMaxInterestRateByProductCode(productCode)
          .orElse(BigDecimal.ZERO);

      // 대표 기간 조회 (가장 일반적인 12개월로 설정)
      Integer termMonths = depositRatesRepository.findByFinPrdtCd(productCode).stream()
          .map(DepositInterestRates::getSaveTrm)
          .findFirst()
          .orElse(12);

      return new ProductInfo(
          product.getFinancialCompany().getKorCoNm(),
          product.getFinPrdtNm(),
          maxRate,
          termMonths
      );
    } else {
      SavingsProducts product = savingsRepository.findById(productCode)
          .orElseThrow(() -> new IllegalArgumentException("적금 상품을 찾을 수 없습니다."));

      BigDecimal maxRate = savingsRatesRepository.findMaxInterestRateByProductCode(productCode)
          .orElse(BigDecimal.ZERO);

      Integer termMonths = savingsRatesRepository.findByFinPrdtCd(productCode).stream()
          .map(SavingsInterestRates::getSaveTrm)
          .findFirst()
          .orElse(12);

      return new ProductInfo(
          product.getFinancialCompany().getKorCoNm(),
          product.getFinPrdtNm(),
          maxRate,
          termMonths
      );
    }
  }


  // 내부 클래스
  @lombok.Getter
  @lombok.AllArgsConstructor
  private static class ProductInfo {

    private String bankName;
    private String productName;
    private BigDecimal maxInterestRate;
    private Integer termMonths;
  }
}
