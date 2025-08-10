package com.project.savingbee.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDetails {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer detailId; // 상세정보 고유ID

  private String holidayYn; // 공휴일 여부

  private String mon; // 월요일 운영시간

  private String tue; // 화요일 운영시간

  private String wed; // 수요일 운영시간

  private String thu; // 목요일 운영시간

  private String fri; // 금요일 운영시간

  private String sat; // 토요일 운영시간

  private String sun; // 일요일 운영시간

  private String astc; // 운영시간 특이사항

  private String addr; // 주소

  private String pstInfo; // 위치정보

  private String brchTelno; // 전화번호

  private String brchAstc; // 점포 특이사항

  @CreationTimestamp
  private LocalDateTime createdAt; // 등록일시

  @UpdateTimestamp
  private LocalDateTime updatedAt; // 수정일시

  private Integer branchId; // 지점 고유ID

  // 외래키 관계
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branchId", referencedColumnName = "branchId", insertable = false, updatable = false)
  private Branches branch; // 지점

}
