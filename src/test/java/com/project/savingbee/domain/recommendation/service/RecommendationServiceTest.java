package com.project.savingbee.domain.recommendation.service;

import com.project.savingbee.common.entity.Cart;
import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.entity.UserProduct;
import com.project.savingbee.common.repository.CartRepository;
import com.project.savingbee.common.repository.DepositInterestRatesRepository;
import com.project.savingbee.common.repository.DepositProductsRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.common.repository.UserProductRepository;
import com.project.savingbee.domain.cart.dto.CartComparisonDTO;
import com.project.savingbee.domain.notification.dto.MaturityNotificationDTO;
import com.project.savingbee.domain.recommendation.dto.RecommendationResponseDTO;
import com.project.savingbee.domain.recommendation.dto.RecommendationSummaryDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

/**
 * RecommendationService 단위 테스트 (Deep Stubs 적용)
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock UserProductRepository userProductRepository;
    @Mock DepositProductsRepository depositRepository;
    @Mock SavingsProductsRepository savingsRepository;
    @Mock DepositInterestRatesRepository depositRatesRepository;
    @Mock SavingsInterestRatesRepository savingsRatesRepository;
    @Mock CartRepository cartRepository;

    @InjectMocks RecommendationService service;

    @Test
    @DisplayName("보유 DEPOSIT 기준 더 높은 금리의 예금 추천 반환")
    void getRecommendationsForUser_depositPath() {
        Long userId = 1L;

        // 보유 상품(예금, 금리 3.0%, 금액 1,000,000원)
        UserProduct up = UserProduct.builder()
                .userProductId(10L)
                .userId(userId)
                .productName("내 예금")
                .productType(UserProduct.ProductType.DEPOSIT)
                .interestRate(BigDecimal.valueOf(3.0))
                .depositAmount(BigDecimal.valueOf(1_000_000))
                .isActive(true)
                .build();

        given(userProductRepository.findByUserIdAndIsActiveTrue(userId))
                .willReturn(List.of(up));

        // 활성 예금 상품 1개 (Deep stubs로 중첩 getter 체인 스텁)
        DepositProducts dp = mock(DepositProducts.class, RETURNS_DEEP_STUBS);
        given(depositRepository.findByIsActiveTrue()).willReturn(List.of(dp));

        given(dp.getFinPrdtCd()).willReturn("DP001");
        given(dp.getFinPrdtNm()).willReturn("슈퍼예금");
        // 중첩 체인 한 줄로 가능
        given(dp.getFinancialCompany().getKorCoNm()).willReturn("KB국민은행");

        // 해당 상품의 최고 금리 4.0%
        given(depositRatesRepository.findMaxInterestRateByProductCode("DP001"))
                .willReturn(Optional.of(BigDecimal.valueOf(4.0)));

        // when
        List<RecommendationResponseDTO> result = service.getRecommendationsForUser(userId);

        // then
        assertThat(result).hasSize(1);
        RecommendationResponseDTO r = result.get(0);
        assertThat(r.getProductType()).isEqualTo("DEPOSIT");
        assertThat(r.getProductName()).isEqualTo("슈퍼예금");
        assertThat(r.getBankName()).isEqualTo("KB국민은행");
        assertThat(r.getRateDifference()).isEqualByComparingTo("1.0"); // 4.0 - 3.0
        // 추가이자 = 1,000,000 * 1% = 10,000 (소수 0자리 반올림)
        assertThat(r.getEstimatedExtraInterest()).isEqualByComparingTo("10000");
        // 우선순위 계산(= 10000은 >10000이 아니므로 5)
        assertThat(r.getPriority()).isEqualTo(5);

        then(depositRepository).should().findByIsActiveTrue();
        then(depositRatesRepository).should().findMaxInterestRateByProductCode("DP001");
    }

    @Test
    @DisplayName("장바구니 vs 보유상품 비교: 장바구니 금리가 더 높으면 BETTER & 예상 추가이자 계산")
    void compareCartWithUserProducts_better() {
        Long userId = 1L;

        // 보유 상품: SAVINGS 3.0%, 1,000,000원
        UserProduct up = UserProduct.builder()
                .userProductId(20L)
                .userId(userId)
                .productName("내 적금")
                .productType(UserProduct.ProductType.SAVINGS)
                .interestRate(BigDecimal.valueOf(3.0))
                .depositAmount(BigDecimal.valueOf(1_000_000))
                .isActive(true)
                .build();

        given(userProductRepository.findByUserIdAndIsActiveTrue(userId))
                .willReturn(List.of(up));

        // 장바구니 아이템: 같은 타입 SAVINGS, 최고금리 4.0%
        Cart cart = mock(Cart.class, RETURNS_DEEP_STUBS); // 필요시 체인 대응
        given(cartRepository.findByUserIdOrderByMaxInterestRateDesc(userId))
                .willReturn(List.of(cart));

        given(cart.getProductType()).willReturn(Cart.ProductType.SAVINGS);
        given(cart.getMaxInterestRate()).willReturn(BigDecimal.valueOf(4.0));
        given(cart.getProductName()).willReturn("장바구니 적금");
        given(cart.getBankName()).willReturn("신한은행");
        given(cart.getProductCode()).willReturn("SV001");

        // when
        List<com.project.savingbee.domain.cart.dto.CartComparisonDTO> list =
                service.compareCartWithUserProducts(userId, 5);

        // then
        assertThat(list).hasSize(1);
        com.project.savingbee.domain.cart.dto.CartComparisonDTO cmp = list.get(0);
        assertThat(cmp.getComparisonType()).isEqualTo("BETTER");
        assertThat(cmp.getRateDifference()).isEqualByComparingTo("1.0");
        assertThat(cmp.getEstimatedExtraInterest()).isEqualByComparingTo("10000"); // 1% of 1,000,000
        assertThat(cmp.getRecommendation()).contains("연 이자 10,000원");
        assertThat(cmp.getUserProductName()).isEqualTo("내 적금");
        assertThat(cmp.getCartProduct().getProductCode()).isEqualTo("SV001");

        then(cartRepository).should().findByUserIdOrderByMaxInterestRateDesc(userId);
    }

    @Test
    @DisplayName("만기 알림 DTO 생성: D-7 대상 조회 및 daysToMaturity/날짜 문자열 생성 확인")
    void getMaturityNotifications_basic() {
        int days = 7;
        LocalDate target = LocalDate.now().plusDays(days);

        UserProduct up = UserProduct.builder()
                .userProductId(30L)
                .productName("7일만기 예금")
                .bankName("농협")
                .interestRate(BigDecimal.valueOf(3.2))
                .depositAmount(BigDecimal.valueOf(2_000_000))
                .maturityDate(target)
                .productType(UserProduct.ProductType.DEPOSIT)
                .build();

        given(userProductRepository.findByMaturityDate(target))
                .willReturn(List.of(up));

        // 내부 findBetterProducts()에서 저장소를 조회하므로, 빈 목록 반환하여 대안 0개로 처리
        given(depositRepository.findByIsActiveTrue()).willReturn(List.of());


        // when
        List<MaturityNotificationDTO> list = service.getMaturityNotifications(days);

        // then
        assertThat(list).hasSize(1);
        MaturityNotificationDTO dto = list.get(0);
        assertThat(dto.getUserProductId()).isEqualTo(30L);
        assertThat(dto.getProductName()).isEqualTo("7일만기 예금");
        assertThat(dto.getBankName()).isEqualTo("농협");
        assertThat(dto.getDaysToMaturity()).isEqualTo(days);
        assertThat(dto.getMaturityDate()).isEqualTo(target.toString());
        assertThat(dto.getAlternativeProducts()).isEmpty();

        then(userProductRepository).should().findByMaturityDate(target);
    }

    @Test
    @DisplayName("추천 요약: 총/최대/합계 수익 및 상위 3개 묶음 생성")
    void getRecommendationSummary_aggregates() {
        Long userId = 1L;

        // Spy로 getRecommendationsForUser만 스텁
        RecommendationService spy = Mockito.spy(new RecommendationService(
                userProductRepository, depositRepository, savingsRepository,
                depositRatesRepository, savingsRatesRepository, cartRepository
        ));

        RecommendationResponseDTO r1 = RecommendationResponseDTO.builder()
                .productCode("A")
                .productName("추천A")
                .bankName("은행A")
                .productType("DEPOSIT")
                .rateDifference(BigDecimal.valueOf(1.5))
                .maxInterestRate(BigDecimal.valueOf(4.5))
                .estimatedExtraInterest(BigDecimal.valueOf(100_000))
                .priority(2)
                .build();

        RecommendationResponseDTO r2 = RecommendationResponseDTO.builder()
                .productCode("B")
                .productName("추천B")
                .bankName("은행B")
                .productType("SAVINGS")
                .rateDifference(BigDecimal.valueOf(0.5))
                .maxInterestRate(BigDecimal.valueOf(3.5))
                .estimatedExtraInterest(BigDecimal.valueOf(20_000))
                .priority(3)
                .build();

        doReturn(List.of(r1, r2)).when(spy).getRecommendationsForUser(userId);

        // when
        RecommendationSummaryDTO summary = spy.getRecommendationSummary(userId);

        // then
        assertThat(summary.getTotalRecommendations()).isEqualTo(2);
        assertThat(summary.getMaxPotentialGain()).isEqualByComparingTo("100000");
        assertThat(summary.getTotalPotentialGain()).isEqualByComparingTo("120000");
        assertThat(summary.getTopRecommendations()).hasSize(2);
        assertThat(summary.getSummaryMessage())
                .contains("총 2개의 더 나은 상품")
                .contains("최대 연 100,000원");
    }
}
