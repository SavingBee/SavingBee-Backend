package com.project.savingbee.domain.notification.dto;

import com.project.savingbee.domain.recommendation.dto.RecommendationResponseDTO;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

// 만기 알림 DTO
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaturityNotificationDTO {
    private Long userProductId; // 만기 임박 상품 ID
    private String productName; // 상품명
    private String bankName; // 은행명
    private BigDecimal currentRate; // 현재 금리
    private BigDecimal depositAmount; // 가입 금액
    private String maturityDate; // 만기일
    private Integer daysToMaturity; // 만기까지 남은 일수
    private List<RecommendationResponseDTO> alternativeProducts; // 대안 상품들
    private String notificationMessage; // 알림 메시지
}
