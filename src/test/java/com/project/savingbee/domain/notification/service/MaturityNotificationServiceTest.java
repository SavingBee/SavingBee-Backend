package com.project.savingbee.domain.notification.service;

import com.project.savingbee.common.entity.UserProduct;
import com.project.savingbee.common.repository.UserProductRepository;
import com.project.savingbee.domain.notification.dto.MaturityNotificationDTO;
import com.project.savingbee.domain.recommendation.dto.RecommendationResponseDTO;
import com.project.savingbee.domain.recommendation.service.RecommendationService;
import com.project.savingbee.domain.user.entity.UserEntity;
import com.project.savingbee.domain.user.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MaturityNotificationServiceTest {

    @Mock UserProductRepository userProductRepository;
    @Mock RecommendationService recommendationService;
    @Mock EmailService emailService;

    @InjectMocks MaturityNotificationService service;

    @Test
    @DisplayName("수동 만기 알림 전송: daysBefore=7 → 이메일 전송까지 호출된다")
    void sendManualMaturityNotifications_sendsEmail() {
        // given
        int daysBefore = 7;
        String maturityDateStr = LocalDate.now().plusDays(daysBefore).toString();

        MaturityNotificationDTO dto = MaturityNotificationDTO.builder()
                .userProductId(11L)
                .productName("스마트적금")
                .bankName("신한은행")
                .currentRate(BigDecimal.valueOf(4.20))
                .depositAmount(BigDecimal.valueOf(2_000_000))
                .maturityDate(maturityDateStr)        // String 타입!
                .daysToMaturity(daysBefore)           // Integer 타입
                .alternativeProducts(List.of())       // 비어 있어도 OK
                .build();

        given(recommendationService.getMaturityNotifications(daysBefore))
                .willReturn(List.of(dto));

        UserEntity user = mock(UserEntity.class);
        given(user.getEmail()).willReturn("user@example.com");
        given(user.getUsername()).willReturn("kimhs");

        UserProduct userProduct = UserProduct.builder()
                .userProductId(11L)
                .userId(1L)
                .productName("스마트적금")
                .userEntity(user)
                .build();

        given(userProductRepository.findById(11L)).willReturn(Optional.of(userProduct));

        ArgumentCaptor<String> emailArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nameArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentArg = ArgumentCaptor.forClass(String.class);

        // when
        service.sendManualMaturityNotifications(daysBefore);

        // then
        then(recommendationService).should(times(1)).getMaturityNotifications(daysBefore);
        then(userProductRepository).should(times(1)).findById(11L);
        then(emailService).should(times(1))
                .sendMaturityNotificationEmail(emailArg.capture(), nameArg.capture(), subjectArg.capture(), contentArg.capture());

        assertThat(emailArg.getValue()).isEqualTo("user@example.com");
        assertThat(nameArg.getValue()).isEqualTo("kimhs");
        assertThat(subjectArg.getValue())
                .contains("[SavingBee]")
                .contains("스마트적금")
                .contains("7일 후"); // 제목 규칙 점검
        assertThat(contentArg.getValue())
                .contains("만기 임박 상품 정보")
                .contains("상품명: 스마트적금")
                .contains("은행명: 신한은행")
                .contains("만기까지: 7일");
    }

    @Test
    @DisplayName("스케줄 메서드: D-30, D-7, D-1 각각 조회 호출된다")
    void scheduledMethod_callsThreeWindows() {
        // given
        given(recommendationService.getMaturityNotifications(anyInt())).willReturn(List.of());

        // when
        service.sendMaturityNotifications();

        // then
        then(recommendationService).should(times(1)).getMaturityNotifications(30);
        then(recommendationService).should(times(1)).getMaturityNotifications(7);
        then(recommendationService).should(times(1)).getMaturityNotifications(1);
        then(emailService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("특정 사용자 만기 임박 목록: 30일 이내만 필터링되고 해당 productId DTO만 반환된다")
    void getUserMaturityNotifications_filtersAndMaps() {
        // given
        Long userId = 1L;
        LocalDate now = LocalDate.now();

        UserProduct p1 = UserProduct.builder()
                .userProductId(101L)
                .userId(userId)
                .maturityDate(now.plusDays(5))
                .productName("5일만기")
                .isActive(true)
                .build();

        UserProduct p2 = UserProduct.builder()
                .userProductId(202L)
                .userId(userId)
                .maturityDate(now.plusDays(40))
                .productName("40일만기")
                .isActive(true)
                .build();

        given(userProductRepository.findByUserIdAndIsActiveTrue(userId))
                .willReturn(List.of(p1, p2));

        MaturityNotificationDTO dto5 = MaturityNotificationDTO.builder()
                .userProductId(101L)
                .productName("5일만기")
                .bankName("KB국민")
                .currentRate(BigDecimal.valueOf(3.5))
                .depositAmount(BigDecimal.valueOf(1_000_000))
                .maturityDate(now.plusDays(5).toString()) // String
                .daysToMaturity(5)                         // Integer
                .alternativeProducts(List.of())            // 대안 없음
                .build();

        given(recommendationService.getMaturityNotifications(5))
                .willReturn(List.of(dto5));

        // when
        List<MaturityNotificationDTO> result = service.getUserMaturityNotifications(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserProductId()).isEqualTo(101L);
        then(recommendationService).should(times(1)).getMaturityNotifications(5);
        then(recommendationService).should(never()).getMaturityNotifications(40); // 30일 초과는 내부 필터에서 제외
    }
}
