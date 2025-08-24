package com.project.savingbee.filtering.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 적립 방식 매핑 - DB의 적립 방식 코드와 필터링 표시명 간의 변환
 */
public enum ReserveType {
  FIXED("정액적립식", "S"),     // 정액적립식
  FREE("자유적립식", "F");      // 자유적립식

  private final String displayName;  // 필터링 저장명
  private final String code;         // DB에 저장된 코드

  ReserveType(String displayName, String code) {
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
