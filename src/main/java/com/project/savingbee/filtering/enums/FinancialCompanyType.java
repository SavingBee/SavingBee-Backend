package com.project.savingbee.filtering.enums;

import java.util.*;

/**
 * 금융회사 유형 코드 매핑 DB의 금융회사 코드와 필터링 표시명 간의 변환
 */
public enum FinancialCompanyType {
  BANK("은행", "020000"),           // 일반 은행
  SAVINGS_BANK("저축은행", "030300"), // 저축은행
  CREDIT_UNION("신협", "050000");    // 신용협동조합

  private final String displayName;   // 사용자에게 보여줄 이름
  private final String orgTypeCode;   // DB에 저장된 기관유형코드

  FinancialCompanyType(String displayName, String orgTypeCode) {
    this.displayName = displayName;
    this.orgTypeCode = orgTypeCode;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getOrgTypeCode() {
    return orgTypeCode;
  }

  /**
   * 표시명으로 기관유형코드 찾기 "은행" → "020000"
   */
  public static Optional<String> getCodeByDisplayName(String displayName) {
    return Arrays.stream(values())
        .filter(type -> type.displayName.equals(displayName))
        .map(type -> type.orgTypeCode)
        .findFirst();
  }

  /**
   * 기관유형코드로 표시명 찾기 "020000" → "은행"
   */
  public static Optional<String> getDisplayNameByCode(String orgTypeCode) {
    return Arrays.stream(values())
        .filter(type -> type.orgTypeCode.equals(orgTypeCode))
        .map(type -> type.displayName)
        .findFirst();
  }
}
