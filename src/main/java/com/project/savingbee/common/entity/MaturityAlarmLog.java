package com.project.savingbee.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaturityAlarmLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long userProductId;

  private Long id; // 사용자ID

  private LocalDateTime notifiedAt; // 알림 전송 시각

  private String daysBeforeMaturity; // 알림 종류

  private Boolean sent; // 알림 전송 성공 여부

  private Long userId; // 예적금 상품을 등록한 사용자ID

  // 외래키 관계
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "userId", referencedColumnName = "userId", insertable = false, updatable = false)
  private UserProduct userProduct; // 사용자 상품
}
