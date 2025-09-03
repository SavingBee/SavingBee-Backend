//package com.project.savingbee.domain.cart.service;
//
//import com.project.savingbee.common.entity.*;
//import com.project.savingbee.common.repository.*;
//import com.project.savingbee.domain.cart.dto.*;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.*;
//import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
//
//@ExtendWith(MockitoExtension.class)
//class CartServiceTest {
//
//    @Mock private CartRepository cartRepository;
//    @Mock private DepositProductsRepository depositRepository;
//    @Mock private SavingsProductsRepository savingsRepository;
//    @Mock private DepositInterestRatesRepository depositRatesRepository;
//    @Mock private SavingsInterestRatesRepository savingsRatesRepository;
//    @Mock private UserProductRepository userProductRepository;
//
//    @InjectMocks private CartService cartService;
//
//    // ---------- getCartItems ----------
//
//    @Test
//    @DisplayName("장바구니 목록 조회: 기본(은행 필터 없음, 페이징)")
//    void getCartItems_basic() {
//        Long userId = 1L;
//
//        Cart c1 = mock(Cart.class, RETURNS_DEEP_STUBS);
//        given(c1.getCartId()).willReturn(101L);
//        given(c1.getProductCode()).willReturn("D001");
//        given(c1.getProductType()).willReturn(Cart.ProductType.DEPOSIT);
//        given(c1.getBankName()).willReturn("국민");
//        given(c1.getProductName()).willReturn("국민 예금");
//        given(c1.getMaxInterestRate()).willReturn(BigDecimal.valueOf(3.5));
//        given(c1.getTermMonths()).willReturn(12);
//        given(c1.getCreatedAt()).willReturn(LocalDateTime.now());
//
//        var pageable = PageRequest.of(0, 10);
//        given(cartRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
//                .willReturn(new PageImpl<>(List.of(c1), pageable, 1));
//
//        CartRequestDTO req = new CartRequestDTO();
//        req.setPage(0);
//        req.setSize(10);
//
//        CartPageResponseDTO page = cartService.getCartItems(userId, req);
//
//        assertThat(page.getContent()).hasSize(1);
//        assertThat(page.getTotalElements()).isEqualTo(1);
//        assertThat(page.getContent().get(0).getProductCode()).isEqualTo("D001");
//    }
//
//    @Test
//    @DisplayName("장바구니 목록 조회: 은행명 필터 적용")
//    void getCartItems_bankFilter() {
//        Long userId = 1L;
//        String bank = "신한";
//
//        Cart c = mock(Cart.class, RETURNS_DEEP_STUBS);
//        given(c.getBankName()).willReturn("신한");
//        var pageable = PageRequest.of(0, 10);
//
//        given(cartRepository.findByUserIdAndBankNameContainingIgnoreCaseOrderByCreatedAtDesc(
//                userId, bank, pageable))
//                .willReturn(new PageImpl<>(List.of(c), pageable, 1));
//
//        CartRequestDTO req = new CartRequestDTO();
//        req.setBankName(bank);
//        req.setPage(0);
//        req.setSize(10);
//
//        CartPageResponseDTO page = cartService.getCartItems(userId, req);
//        assertThat(page.getContent()).hasSize(1);
//        assertThat(page.getContent().get(0).getBankName()).isEqualTo("신한");
//    }
//
//    // ---------- addToCart ----------
//
//    @Test
//    @DisplayName("담기: 예금 상품 성공 추가 (중복 아님)")
//    void addToCart_deposit_success() {
//        Long userId = 1L;
//
//        CartRequestDTO req = new CartRequestDTO();
//        req.setProductCode("DP001");
//        req.setProductType(Cart.ProductType.DEPOSIT);
//
//        // 중복 아님
//        given(cartRepository.existsByUserIdAndProductCodeAndProductType(userId, "DP001", Cart.ProductType.DEPOSIT))
//                .willReturn(false);
//
//        // 상품 정보 (예금)
//        DepositProducts dp = mock(DepositProducts.class, RETURNS_DEEP_STUBS);
//        given(depositRepository.findById("DP001")).willReturn(Optional.of(dp));
//        given(dp.getFinancialCompany().getKorCoNm()).willReturn("KB국민은행");
//        given(dp.getFinPrdtNm()).willReturn("국민 정기예금");
//
//        given(depositRatesRepository.findMaxInterestRateByProductCode("DP001"))
//                .willReturn(Optional.of(BigDecimal.valueOf(4.2)));
//        // 대표 기간(12개월)
//        DepositInterestRates r = mock(DepositInterestRates.class);
//        given(r.getSaveTrm()).willReturn(12);
//        given(depositRatesRepository.findByFinPrdtCd("DP001")).willReturn(List.of(r));
//
//        // 저장
//        Cart saved = mock(Cart.class, RETURNS_DEEP_STUBS);
//        given(saved.getCartId()).willReturn(10L);
//        given(saved.getProductCode()).willReturn("DP001");
//        given(saved.getProductType()).willReturn(Cart.ProductType.DEPOSIT);
//        given(saved.getBankName()).willReturn("KB국민은행");
//        given(saved.getProductName()).willReturn("국민 정기예금");
//        given(saved.getMaxInterestRate()).willReturn(BigDecimal.valueOf(4.2));
//        given(saved.getTermMonths()).willReturn(12);
//
//        given(cartRepository.save(any(Cart.class))).willReturn(saved);
//
//        CartResponseDTO dto = cartService.addToCart(userId, req);
//
//        assertThat(dto.getCartId()).isEqualTo(10L);
//        assertThat(dto.getProductCode()).isEqualTo("DP001");
//        assertThat(dto.getBankName()).isEqualTo("KB국민은행");
//        assertThat(dto.getMaxInterestRate()).isEqualByComparingTo("4.2");
//        assertThat(dto.getTermMonths()).isEqualTo(12);
//    }
//
//    @Test
//    @DisplayName("담기: 이미 담긴 상품이면 예외")
//    void addToCart_duplicate_throws() {
//        Long userId = 1L;
//        CartRequestDTO req = new CartRequestDTO();
//        req.setProductCode("DP001");
//        req.setProductType(Cart.ProductType.DEPOSIT);
//
//        given(cartRepository.existsByUserIdAndProductCodeAndProductType(userId, "DP001", Cart.ProductType.DEPOSIT))
//                .willReturn(true);
//
//        assertThatThrownBy(() -> cartService.addToCart(userId, req))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("이미 장바구니에 담긴 상품");
//    }
//
//    // ---------- removeFromCart ----------
//
//    @Test
//    @DisplayName("삭제(단건): 본인 소유 → 삭제 성공")
//    void removeFromCart_success() {
//        Long userId = 1L;
//        Long cartId = 100L;
//
//        Cart c = mock(Cart.class);
//        given(c.getUserId()).willReturn(userId);
//        given(cartRepository.findById(cartId)).willReturn(Optional.of(c));
//
//        cartService.removeFromCart(userId, cartId);
//
//        then(cartRepository).should().delete(c);
//    }
//
//    @Test
//    @DisplayName("삭제(단건): 본인 아님 → 권한 예외")
//    void removeFromCart_unauthorized() {
//        Long userId = 1L;
//        Long cartId = 100L;
//
//        Cart c = mock(Cart.class);
//        given(c.getUserId()).willReturn(999L);
//        given(cartRepository.findById(cartId)).willReturn(Optional.of(c));
//
//        assertThatThrownBy(() -> cartService.removeFromCart(userId, cartId))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("권한이 없습니다");
//    }
//
//    // ---------- removeMultipleFromCart / clearCart ----------
//
//    @Test
//    @DisplayName("선택 삭제: 삭제 개수 반환")
//    void removeMultipleFromCart_count() {
//        Long userId = 1L;
//        CartRequestDTO req = new CartRequestDTO();
//        req.setCartIds(List.of(1L, 2L, 3L));
//
//        given(cartRepository.deleteByCartIdsAndUserId(req.getCartIds(), userId)).willReturn(3);
//
//        int count = cartService.removeMultipleFromCart(userId, req);
//        assertThat(count).isEqualTo(3);
//    }
//
//    @Test
//    @DisplayName("전체 비우기: 삭제 개수 반환")
//    void clearCart_count() {
//        Long userId = 1L;
//        given(cartRepository.deleteAllByUserId(userId)).willReturn(7);
//
//        int count = cartService.clearCart(userId);
//        assertThat(count).isEqualTo(7);
//    }
//
//    // ---------- getCartSummary ----------
//
/// /    @Test /    @DisplayName("요약/통계: 총/유형별/최대/평균 금리 계산") /    void getCartSummary_ok() { /
///  Long userId = 1L; / /        given(cartRepository.countByUserId(userId)).willReturn(5L); /
///   given(cartRepository.findByUserIdAndProductType(userId, Cart.ProductType.DEPOSIT)) /
///      .willReturn(List.of(mock(Cart.class))); /
/// given(cartRepository.findByUserIdAndProductType(userId, Cart.ProductType.SAVINGS)) /
///    .willReturn(List.of(mock(Cart.class), mock(Cart.class))); / /
/// given(cartRepository.findMaxInterestRateByUserId(userId)) /
/// .willReturn(Optional.of(BigDecimal.valueOf(4.5))); / /        Cart top = mock(Cart.class); /
///    given(top.getProductName()).willReturn("최고금리 상품"); /
/// given(top.getMaxInterestRate()).willReturn(BigDecimal.valueOf(4.5)); / /        Cart c2 =
/// mock(Cart.class); /        given(c2.getMaxInterestRate()).willReturn(BigDecimal.valueOf(3.5)); /
/// /        given(cartRepository.findByUserIdOrderByMaxInterestRateDesc(userId)) /
/// .willReturn(List.of(top, c2)); / /        CartSummaryDTO sum =
/// cartService.getCartSummary(userId); / /        assertThat(sum.getTotalCount()).isEqualTo(5); /
///      assertThat(sum.getDepositCount()).isEqualTo(1); /
/// assertThat(sum.getSavingsCount()).isEqualTo(2); /
/// assertThat(sum.getMaxInterestRate()).isEqualByComparingTo("4.5"); /
/// assertThat(sum.getMaxInterestProductName()).isEqualTo("최고금리 상품"); /        // 평균 (4.5 + 3.5) / 2
/// = 4.00 /        assertThat(sum.getAvgInterestRate()).isEqualByComparingTo("4.00"); /    }
//
//    // ---------- compareWithUserProducts ----------
////
////    @Test
////    @DisplayName("보유상품 없음: NEW 타입으로 비교 결과 생성")
////    void compareWithUserProducts_noUserProducts() {
////        Long userId = 1L;
////
////        Cart cart = mock(Cart.class);
////        given(cart.getProductType()).willReturn(Cart.ProductType.DEPOSIT);
////        given(cart.getProductCode()).willReturn("DP001");
////        given(cart.getProductName()).willReturn("국민 예금");
////        given(cart.getBankName()).willReturn("국민");
////        given(cart.getMaxInterestRate()).willReturn(BigDecimal.valueOf(4.0));
////
////        given(cartRepository.findByUserIdOrderByMaxInterestRateDesc(userId))
////                .willReturn(List.of(cart));
////        given(userProductRepository.findByUserIdAndIsActiveTrue(userId))
////                .willReturn(List.of()); // 보유 없음
////
////        var list = cartService.compareWithUserProducts(userId, 5);
////
////        assertThat(list).hasSize(1);
////        CartComparisonDTO cmp = list.get(0);
////        assertThat(cmp.getComparisonType()).isEqualTo("NEW");
////        assertThat(cmp.getUserProductName()).isEqualTo("보유 상품 없음");
////        // 기준 1000만원 * 4% = 400,000
////        assertThat(cmp.getEstimatedExtraInterest()).isEqualByComparingTo("400000");
////    }
//
//    @Test
//    @DisplayName("보유상품 존재: 같은 타입 중 가장 유사 금리와 비교 → BETTER/SAME/WORSE 판단 및 메시지")
//    void compareWithUserProducts_betterCase() {
//        Long userId = 1L;
//
//        // 장바구니: SAVINGS 4.0%
//        Cart cart = mock(Cart.class);
//        given(cart.getProductType()).willReturn(Cart.ProductType.SAVINGS);
//        given(cart.getProductCode()).willReturn("SV001");
//        given(cart.getProductName()).willReturn("장바구니 적금");
//        given(cart.getBankName()).willReturn("신한");
//        given(cart.getMaxInterestRate()).willReturn(BigDecimal.valueOf(4.0));
//
//        given(cartRepository.findByUserIdOrderByMaxInterestRateDesc(userId))
//                .willReturn(List.of(cart));
//
//        // 보유상품 2개 (둘 다 SAVINGS) → 3.0%와 3.8% 중 3.8%가 더 유사하지만
//        // 서비스 로직은 "cart와의 절대 차이가 더 작은 것"을 선택함.
//        UserProduct up1 = UserProduct.builder()
//                .userProductId(1L)
//                .userId(userId)
//                .productType(UserProduct.ProductType.SAVINGS)
//                .productName("내 적금 A")
//                .interestRate(BigDecimal.valueOf(3.0))
//                .depositAmount(BigDecimal.valueOf(1_000_000))
//                .isActive(true)
//                .build();
//
//        UserProduct up2 = UserProduct.builder()
//                .userProductId(2L)
//                .userId(userId)
//                .productType(UserProduct.ProductType.SAVINGS)
//                .productName("내 적금 B")
//                .interestRate(BigDecimal.valueOf(3.8))
//                .depositAmount(BigDecimal.valueOf(1_000_000))
//                .isActive(true)
//                .build();
//
//        given(userProductRepository.findByUserIdAndIsActiveTrue(userId))
//                .willReturn(List.of(up1, up2));
//
//        var list = cartService.compareWithUserProducts(userId, 5);
//
//        assertThat(list).hasSize(1);
//        CartComparisonDTO cmp = list.get(0);
//        // 가장 유사: 3.8% (차이 0.2) vs 3.0% (차이 1.0) → up2가 매칭
//        assertThat(cmp.getUserProductName()).isEqualTo("내 적금 B");
//        assertThat(cmp.getUserProductRate()).isEqualByComparingTo("3.8");
//        // rateDiff = 4.0 - 3.8 = 0.2
//        assertThat(cmp.getRateDifference()).isEqualByComparingTo("0.2");
//        // 예상 추가이자 = (1000만원 * 0.2%) = 20,000
//        assertThat(cmp.getEstimatedExtraInterest()).isEqualByComparingTo("20000");
//        assertThat(cmp.getComparisonType()).isEqualTo("BETTER");
//        assertThat(cmp.getRecommendation()).contains("더 높은 금리").contains("20,000원");
//    }
//
//    // ---------- 단순 조회 편의 API ----------
//
//    @Test
//    @DisplayName("최근 담은 순 목록")
//    void getCartItemsByRecent() {
//        Long userId = 1L;
//
//        Cart c = mock(Cart.class);
//        given(c.getProductCode()).willReturn("DP001");
//        given(cartRepository.findByUserIdOrderByCreatedAtDesc(userId))
//                .willReturn(List.of(c));
//
//        var list = cartService.getCartItemsByRecent(userId);
//        assertThat(list).hasSize(1);
//        assertThat(list.get(0).getProductCode()).isEqualTo("DP001");
//    }
//
//    @Test
//    @DisplayName("은행명 필터 목록")
//    void getCartItemsByBank() {
//        Long userId = 1L;
//
//        Cart c = mock(Cart.class);
//        given(c.getBankName()).willReturn("하나");
//        given(cartRepository.findByUserIdAndBankNameContainingIgnoreCase(userId, "하나"))
//                .willReturn(List.of(c));
//
//        var list = cartService.getCartItemsByBank(userId, "하나");
//        assertThat(list).hasSize(1);
//        assertThat(list.get(0).getBankName()).isEqualTo("하나");
//    }
//}
