package com.project.savingbee.filtering.enums;

import java.util.*;

/**
 * 가입대상 코드 매핑 DB에 저장된 joinDeny 코드와 - 프론트에서 전달받는 표시명 간의 변환
 */

public enum JoinDenyType {
  UNRESTRICTED("제한없음", "1"),        // 누구나 가입 가능
  LOW_INCOME_ONLY("서민전용", "2"),     // 서민 전용 상품
  PARTIALLY_RESTRICTED("일부제한", "3"); // 일부 제한 (나이, 지역 등)

  private final String displayName;  // 사용자에게 보여줄 이름
  private final String code;         // DB에 저장된 코드

  JoinDenyType(String displayName, String code) {
    this.displayName = displayName;
    this.code = code;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getCode() {
    return code;
  }

  /**
   * 표시명으로 DB 코드 찾기 "제한없음" → "1"
   */
  public static Optional<String> getCodeByDisplayName(String displayName) {
    return Arrays.stream(values())
        .filter(type -> type.displayName.equals(displayName))
        .map(type -> type.code)
        .findFirst();
  }

  /**
   * DB 코드로 표시명 찾기 "1" → "제한없음"
   */
  public static Optional<String> getDisplayNameByCode(String code) {
    return Arrays.stream(values())
        .filter(type -> type.code.equals(code))
        .map(type -> type.displayName)
        .findFirst();
  }
}
