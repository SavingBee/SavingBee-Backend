package com.project.savingbee.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 금융회사 엔터티
 */
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