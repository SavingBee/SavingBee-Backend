package com.project.savingbee.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAlertEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;                        // key

  private Long alertSettingId;            // 알림 설정 ID

  @Enumerated(EnumType.STRING)
  private TriggerType triggerType;        // 알림 종류(상품 알림/만기 알림)

  @Enumerated(EnumType.STRING)
  private ProductKind productKind;        // 상품 종류(예금/적금), 예/적금이 별도의 테이블이므로 추가

  private String productCode;             // 상품 코드

  @Lob
  private String payloadJson;             // 알림 템플릿용 스냅샷

  @Column(unique = true)
  private String dedupeKey;               // 중복 방지 키

  @Enumerated(EnumType.STRING)
  private EventStatus status;             // 알림 상태

  private int attempts;                   // 발송 재시도 횟수

  private String lastError;               // 최근 실패 메시지

  private LocalDateTime sendNotBefore;    // 알림 발송 시간 제한용

  @CreationTimestamp
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;

  private LocalDateTime sentAt;           // 알림 발송 완료 시간

  // 외래키 관계
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "alertSettingId", referencedColumnName = "id", insertable = false, updatable = false)
  private ProductAlertSetting productAlertSetting;

  // Enum 정의
  public enum TriggerType {
    PRODUCT_CHANGE,   // 상품 알림
    MATURITY          // 만기 알림
  }

  public enum ProductKind {
    DEPOSIT,   // 예금
    SAVINGS    // 적금
  }

  public enum EventStatus {
    READY, SENDING, SENT, FAILED
  }
}
