package com.project.savingbee.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branches {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer branchId; // 지점고유ID

  private String trnsOrgCode; // 전송기관코드

  private String orgTypeCode; // 기관유형코드

  private String brchCode; // 지점코드

  private String dupBrchCode; // 중복지점코드

  private String brchName; // 지점명

  @Column(precision = 10, scale = 8)
  private BigDecimal brchLatitude; // 위도

  @Column(precision = 11, scale = 8)
  private BigDecimal brchLongitude; // 경도

  private String clseSchdYn; // 폐쇄예정여부

  private LocalDate clseSchdDt; // 폐쇄예정일

  private String sbstBrch1Code; // 대체지점1코드

  @Column(precision = 10, scale = 8)
  private BigDecimal sbstBrch1Latitude; // 대체지점1위도

  @Column(precision = 11, scale = 8)
  private BigDecimal sbstBrch1Longitude; // 대체지점1경도

  @CreationTimestamp
  private LocalDateTime createdAt; // 등록일시

  @UpdateTimestamp
  private LocalDateTime updatedAt; // 수정일시

  private String finCoNo; // 금융회사 고유번호

  // 외래키 관계
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "finCoNo", referencedColumnName = "finCoNo", insertable = false, updatable = false)
  private FinancialCompanies financialCompany; // 금융회사

  // 연관관계
  @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<BranchDetails> branchDetails; // 지점 상세 정보들
}
