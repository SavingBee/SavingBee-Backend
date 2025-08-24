package com.project.savingbee.filtering.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상품 상세 정보 응답
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailResponse {

  @JsonProperty("fin_prdt_cd")
  private String finPrdtCd;             // 금융상품 코드

  @JsonProperty("fin_prdt_nm")
  private String finPrdtNm;             // 금융상품명

  @JsonProperty("product_type")
  private String productType;           // 상품 타입 ("deposit" or "saving")

  @JsonProperty("fin_co_no")
  private String finCoNo;               // 금융회사 고유번호

  @JsonProperty("kor_co_nm")
  private String korCoNm;               // 금융회사명

  @JsonProperty("join_way")
  private String joinWay;               // 가입 방법

  @JsonProperty("join_deny_nm")
  private String joinDenyNm;            // 가입제한 이름

  @JsonProperty("join_member")
  private String joinMember;            // 가입대상

  @JsonProperty("max_limit")
  private BigDecimal maxLimit;          // 가입한도 (예금: 최고한도, 적금: 월 가입한도)

  @JsonProperty("spcl_cnd")
  private String spclCnd;               // 우대조건

  @JsonProperty("mtrt_int")
  private String mtrtInt;               // 만기 후 이자율

  @JsonProperty("etc_note")
  private String etcNote;               // 기타 유의사항

  @JsonProperty("dcls_strt_day")
  private String dclsStrtDay;           // 공시 시작일

  @JsonProperty("dcls_end_day")
  private String dclsEndDay;            // 공시 종료일

  @JsonProperty("interest_rates")
  private List<InterestRateOption> interestRates;  // 금리 옵션들

  // 금리 옵션 정보

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class InterestRateOption {

    @JsonProperty("save_trm")
    private Integer saveTrm;              // 저축 기간

    @JsonProperty("intr_rate_type_nm")
    private String intrRateTypeNm;        // 이자계산 방식 표시명

    @JsonProperty("intr_rate")
    private BigDecimal intrRate;          // 기본 금리

    @JsonProperty("intr_rate2")
    private BigDecimal intrRate2;         // 우대 금리

    // 적금 전용 필드
    @JsonProperty("rsrv_type_nm")
    private String rsrvTypeNm;            // 적립 방식 표시명

    private BigDecimal totalMaxLimit;     // 총 저축금 (저축기간 * 해당 기간의 월 저축 한도)
  }
}
