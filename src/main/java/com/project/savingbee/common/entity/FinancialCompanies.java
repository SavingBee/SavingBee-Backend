package com.project.savingbee.common.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialCompanies {

  @Id
  private String finCoNo; // 금융회사 고유번호

  private String korCoNm; // 금융회사 명

  private String trnsOrgCode; // 전송기관코드

  private String orgTypeCode; // 기관유형코드

  @CreationTimestamp
  private LocalDateTime createdAt; // 등록일시

  @UpdateTimestamp
  private LocalDateTime updatedAt; // 수정일시

  // 연관관계
  @OneToMany(mappedBy = "financialCompany", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<DepositProducts> depositProducts; // 예금 상품들

  @OneToMany(mappedBy = "financialCompany", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<SavingsProducts> savingsProducts; // 적금 상품들
}