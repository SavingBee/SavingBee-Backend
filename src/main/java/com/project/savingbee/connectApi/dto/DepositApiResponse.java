package com.project.savingbee.connectApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DepositApiResponse {

  private Result result;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Result {

    @JsonProperty("prdt_div")
    private String prdtDiv;

    @JsonProperty("total_count")
    private String totalCount;

    @JsonProperty("max_page_no")
    private String maxPageNo;

    @JsonProperty("now_page_no")
    private String nowPageNo;

    @JsonProperty("err_cd")
    private String errCd;

    @JsonProperty("err_msg")
    private String errMsg;

    @JsonProperty("baseList")
    private List<BaseListItem> baseList;  // 금융상품 리스트

    @JsonProperty("optionList")
    private List<OptionListItem> optionList;  // 금융상품 금리 옵션 리스트
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class BaseListItem {

    @JsonProperty("dcls_month")
    private String dclsMonth;

    @JsonProperty("fin_co_no")
    private String finCoNo;

    @JsonProperty("fin_prdt_cd")
    private String finPrdtCd;

    @JsonProperty("kor_co_nm")
    private String korCoNm;

    @JsonProperty("fin_prdt_nm")
    private String finPrdtNm;

    @JsonProperty("join_way")
    private String joinWay;

    @JsonProperty("mtrt_int")
    private String mtrtInt;

    @JsonProperty("spcl_cnd")
    private String spclCnd;

    @JsonProperty("join_deny")
    private String joinDeny;

    @JsonProperty("join_member")
    private String joinMember;

    @JsonProperty("etc_note")
    private String etcNote;

    @JsonProperty("max_limit")
    private String maxLimit;  // String으로 받아서 파싱

    @JsonProperty("dcls_strt_day")
    private String dclsStrtDay;

    @JsonProperty("dcls_end_day")
    private String dclsEndDay;

    @JsonProperty("fin_co_subm_day")
    private String finCoSubmDay;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OptionListItem {

    @JsonProperty("fin_prdt_cd")
    private String finPrdtCd;

    @JsonProperty("intr_rate_type")
    private String intrRateType;

    @JsonProperty("intr_rate_type_nm")
    private String intrRateTypeNm;

    @JsonProperty("save_trm")
    private String saveTrm;

    @JsonProperty("intr_rate")
    private String intrRate;

    @JsonProperty("intr_rate2")
    private String intrRate2;
  }
}
