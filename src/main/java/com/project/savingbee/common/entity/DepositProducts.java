package com.project.savingbee.common.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import java.math.BigDecimal;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositProducts {

  @Id
  private String finPrdtCd; // 금융상품코드

  private String finPrdtNm; // 금융상품명

  private String joinWay; // 가입방법

  private String mtrtInt; // 만기 후 이자율 조건

  private String spclCnd; // 우대조건

  private String joinDeny; // 가입제한 구분

  private String joinMember; // 가입대상

  private String etcNote; // 기타유의사항

  private BigDecimal maxLimit; // 최고한도(원)

  private LocalDate dclsStrtDay; // 공시 시작일

  private LocalDate dclsEndDay; // 공시 종료일

  private Boolean isActive; // 활성 상태

  @CreationTimestamp
  private LocalDateTime createdAt; // 등록일시

  @UpdateTimestamp
  private LocalDateTime updatedAt; // 수정일시

  private String finCoNo; // 금융회사 고유번호

  private BigDecimal minAmount; // 최소 가입금액

  private BigDecimal maxAmount; // 가입 한도

  // 외래키 관계
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "finCoNo", referencedColumnName = "finCoNo",
      insertable = false, updatable = false)
  private FinancialCompanies financialCompany; // 금융회사

  // 금융회사 고유번호 가져오기
  public String getFinCoNo() {
    return financialCompany != null ? financialCompany.getFinCoNo() : null;
  }

  // 연관관계
  @OneToMany(mappedBy = "depositProduct", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<DepositInterestRates> interestRates; // 예금 금리 정보들
}
