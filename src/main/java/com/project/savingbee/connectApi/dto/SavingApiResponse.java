package com.project.savingbee.connectApi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingApiResponse {

  @JsonProperty("result")
  private SavingResult result;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class SavingResult {

    @JsonProperty("prdt_div")
    private String productDivision; // 상품구분

    @JsonProperty("total_count")
    private String totalCount; // 총 개수

    @JsonProperty("max_page_no")
    private String maxPageNo; // 최대 페이지 번호

    @JsonProperty("now_page_no")
    private String nowPageNo; // 현재 페이지 번호

    @JsonProperty("err_cd")
    private String errorCode; // 에러 코드

    @JsonProperty("err_msg")
    private String errorMessage; // 에러 메시지

    @JsonProperty("baseList")
    private List<SavingBaseInfo> baseList; // 기본 상품 정보 목록

    @JsonProperty("optionList")
    private List<SavingOptionInfo> optionList; // 옵션 정보 목록
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class SavingBaseInfo {

    @JsonProperty("dcls_month")
    private String disclosureMonth; // 공시월

    @JsonProperty("fin_co_no")
    private String finCoNo; // 금융회사 고유번호

    @JsonProperty("fin_prdt_cd")
    private String finPrdtCd; // 금융상품 코드

    @JsonProperty("kor_co_nm")
    private String korCoNm; // 금융회사명

    @JsonProperty("fin_prdt_nm")
    private String finPrdtNm; // 금융상품명

    @JsonProperty("join_way")
    private String joinWay; // 가입방법

    @JsonProperty("mtrt_int")
    private String mtrtInt; // 만기 후 이자율

    @JsonProperty("spcl_cnd")
    private String spclCnd; // 우대조건

    @JsonProperty("join_deny")
    private String joinDeny; // 가입제한

    @JsonProperty("join_member")
    private String joinMember; // 가입대상

    @JsonProperty("etc_note")
    private String etcNote; // 기타유의사항

    @JsonProperty("max_limit")
    private Long maxLimit; // 월 가입한도(원)

    @JsonProperty("dcls_strt_day")
    private String dclsStrtDay; // 공시 시작일

    @JsonProperty("dcls_end_day")
    private String dclsEndDay; // 공시 종료일

    @JsonProperty("fin_co_subm_day")
    private String finCoSubmDay; // 금융회사 제출일
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class SavingOptionInfo {

    @JsonProperty("dcls_month")
    private String disclosureMonth; // 공시월

    @JsonProperty("fin_co_no")
    private String finCoNo; // 금융회사 고유번호

    @JsonProperty("fin_prdt_cd")
    private String finPrdtCd; // 금융상품 코드

    @JsonProperty("intr_rate_type")
    private String intrRateType; // 이자율유형(S:단리, M:복리)

    @JsonProperty("intr_rate_type_nm")
    private String intrRateTypeNm; // 이자율유형명

    @JsonProperty("rsrv_type")
    private String rsrvType; // 적립유형(S:정액적립식, F:자유적립식)

    @JsonProperty("rsrv_type_nm")
    private String rsrvTypeNm; // 적립유형명

    @JsonProperty("save_trm")
    private Integer saveTrm; // 저축기간(월)

    @JsonProperty("intr_rate")
    private BigDecimal intrRate; // 기본금리(%)

    @JsonProperty("intr_rate2")
    private BigDecimal intrRate2; // 최고우대금리(%)
  }
}