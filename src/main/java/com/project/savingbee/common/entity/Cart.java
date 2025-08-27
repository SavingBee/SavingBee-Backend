package com.project.savingbee.common.entity;

import com.project.savingbee.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 사용자의 장바구니 정보를 저장하는 엔티티
 * 관심 있는 예적금 상품들을 임시로 저장하고 비교 분석에 활용
 */
@Entity
@Table(name = "cart", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "productCode", "productType"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId; // 사용자 ID
    
    private String productCode; // 상품코드 (finPrdtCd)
    
    @Enumerated(EnumType.STRING)
    private ProductType productType; // 상품 유형 (DEPOSIT/SAVINGS)
    
    private String bankName; // 은행명 (조회 최적화용)
    
    private String productName; // 상품명 (조회 최적화용)
    
    @Column(precision = 5, scale = 2)
    private BigDecimal maxInterestRate; // 최고우대금리 (조회 최적화용)
    
    private Integer termMonths; // 기간 (개월)
    
    @CreationTimestamp
    private LocalDateTime createdAt; // 담은 날짜
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // 외래키 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private UserEntity user;
    
    // 상품 타입 Enum
    public enum ProductType {
        DEPOSIT, SAVINGS
    }
}
