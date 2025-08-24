package com.project.savingbee.filtering.enums;

import java.util.*;

/**
 * 이자계산방식 코드 매핑 - DB의 intrRateType과 필터링 표시명 간의 변환
 */
public enum InterestRateType {
  SIMPLE("단리", "S"),     // 단리 계산
  COMPOUND("복리", "M");   // 복리 계산

  private final String displayName;  // 사용자에게 보여줄 이름
  private final String code;         // DB에 저장된 코드

  InterestRateType(String displayName, String code) {
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
   * 표시명으로 DB 코드 찾기
   */
  public static Optional<String> getCodeByDisplayName(String displayName) {
    return Arrays.stream(values())
        .filter(type -> type.displayName.equals(displayName))
        .map(type -> type.code)
        .findFirst();
  }

  /**
   * DB 코드로 표시명 찾기
   */
  public static Optional<String> getDisplayNameByCode(String code) {
    return Arrays.stream(values())
        .filter(type -> type.code.equals(code))
        .map(type -> type.displayName)
        .findFirst();
  }
}
