package com.project.savingbee.domain.userproduct.service;

import com.project.savingbee.common.entity.UserProduct;
import com.project.savingbee.common.repository.UserProductRepository;
import com.project.savingbee.domain.userproduct.dto.UserProductRequestDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;



@ExtendWith(MockitoExtension.class)
class UserProductServiceTest {

    @Mock
    private UserProductRepository userProductRepository;

    @InjectMocks
    private UserProductService userProductService;

    @Test
    @DisplayName("보유 상품 등록 성공")
    void registerUserProduct_success() {
        Long productId = 10L;
        // given
        Long userId = 1L;
        UserProductRequestDTO request = new UserProductRequestDTO();
        request.setBankName("KB국민은행");
        request.setProductName("정기예금");
        request.setProductType(UserProduct.ProductType.DEPOSIT);
        request.setInterestRate(BigDecimal.valueOf(3.2));
        request.setDepositAmount(BigDecimal.valueOf(1_000_000));
        request.setTermMonths(12);
        request.setJoinDate(LocalDate.of(2025, 1, 1));
        request.setMaturityDate(LocalDate.of(2026, 1, 1));
        request.setSpecialConditions("우대조건");

        UserProduct saved = UserProduct.builder()
                .userProductId(productId)
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

        given(userProductRepository.save(any(UserProduct.class))).willReturn(saved);

        // when
        UserProductResponseDTO response = userProductService.registerUserProduct(userId, request);

        // then
        assertThat(response.getUserProductId()).isEqualTo(10L);
        assertThat(response.getProductName()).isEqualTo("정기예금");
        then(userProductRepository).should(times(1)).save(any(UserProduct.class));
    }

    @Test
    @DisplayName("보유 상품 등록 실패 - 만기일 < 가입일")
    void registerUserProduct_fail_dueToInvalidDates() {
        // given
        UserProductRequestDTO request = new UserProductRequestDTO();
        request.setJoinDate(LocalDate.of(2025, 10, 1));
        request.setMaturityDate(LocalDate.of(2025, 9, 30));

        // when & then
        assertThatThrownBy(() -> userProductService.registerUserProduct(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만기일은 가입일보다 이후여야 합니다.");
    }

    @Test
    @DisplayName("보유 상품 수정 성공")
    void updateUserProduct_success() {
        // given
        Long userId = 1L;
        Long productId = 10L;
        UserProduct existing = UserProduct.builder()
                .userProductId(productId)
                .userId(userId)
                .bankName("기존은행")
                .productName("기존상품")
                .productType(UserProduct.ProductType.DEPOSIT)
                .interestRate(BigDecimal.valueOf(3.0))
                .depositAmount(BigDecimal.valueOf(500_000))
                .joinDate(LocalDate.of(2024, 1, 1))
                .maturityDate(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build();

        UserProductRequestDTO request = new UserProductRequestDTO();
        request.setBankName("신한은행");
        request.setProductName("스마트적금");
        request.setProductType(UserProduct.ProductType.SAVINGS);
        request.setInterestRate(BigDecimal.valueOf(4.0));
        request.setDepositAmount(BigDecimal.valueOf(2_000_000));
        request.setJoinDate(LocalDate.of(2025, 1, 1));
        request.setMaturityDate(LocalDate.of(2026, 1, 1));

        given(userProductRepository.findById(productId)).willReturn(Optional.of(existing));
        given(userProductRepository.save(any(UserProduct.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        UserProductResponseDTO response = userProductService.updateUserProductById(userId, productId, request);

        // then
        assertThat(response.getBankName()).isEqualTo("신한은행");
        assertThat(response.getProductType()).isEqualTo(UserProduct.ProductType.SAVINGS);
    }

    @Test
    @DisplayName("보유 상품 삭제 성공 (isActive=false)")
    void deleteUserProduct_success() {
        // given
        Long userId = 1L;
        Long productId = 10L;
        UserProduct existing = UserProduct.builder()
                .userProductId(productId)
                .userId(userId)
                .isActive(true)
                .build();

        given(userProductRepository.findById(productId)).willReturn(Optional.of(existing));
        given(userProductRepository.save(any(UserProduct.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        userProductService.deleteUserProductById(userId, productId);

        // then
        assertThat(existing.getIsActive()).isFalse();
        then(userProductRepository).should().save(existing);
    }

    @Test
    @DisplayName("단일 상품 조회 성공")
    void getUserProduct_success() {
        // given
        Long userId = 1L;
        Long productId = 10L;
        UserProduct product = UserProduct.builder()
                .userProductId(productId)
                .userId(userId)
                .bankName("농협")
                .productName("NH예금")
                .isActive(true)
                .build();

        given(userProductRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        UserProductResponseDTO response = userProductService.getUserProductById(userId, productId);

        // then
        assertThat(response.getProductName()).isEqualTo("NH예금");
    }

    @Test
    @DisplayName("만기 임박 상품 조회 성공")
    void getMaturityProducts_success() {
        Long productId = 100L;
        // given
        LocalDate target = LocalDate.now().plusDays(7);
        UserProduct product = UserProduct.builder()
                .userProductId(productId)
                .userId(1L)
                .maturityDate(target)
                .productName("7일만기 상품")
                .build();

        given(userProductRepository.findByMaturityDate(target)).willReturn(List.of(product));

        // when
        var result = userProductService.getMaturityProducts(7);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductName()).isEqualTo("7일만기 상품");
    }
}
