package com.project.savingbee.common.entity;

import com.project.savingbee.domain.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaturityAlarmLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long alarmId; //알림 로그 한 건

  private LocalDateTime notifiedAt; // 알림 전송 시각

  private String daysBeforeMaturity; // 알림 종류 (D-30, D-7, D-1 등)

  private Boolean sent; // 알림 전송 성공 여부

  @CreationTimestamp
  private LocalDateTime createdAt; // 로그 생성 시각(등록 시간)

  @Column(name = "user_product_id", insertable = false, updatable = false)
  private Long userProductId; //사용자 보유 상품 ID

  @Column(name = "user_id", insertable = false, updatable = false)
  private Long userId; //  사용자ID

  // 외래키 관계
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_product_id", referencedColumnName = "user_product_id", insertable = false, updatable = false)
  private UserProduct userProduct; // 사용자 보유 상품

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
  private UserEntity user; // 사용자
}
