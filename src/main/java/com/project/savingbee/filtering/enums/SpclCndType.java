package com.project.savingbee.filtering.enums;

import java.util.*;
import java.util.stream.*;

/**
 * 우대조건 검색 필드
 */
public enum SpclCndType {
  NONE("우대조건없음", Arrays.asList("없음", "우대조건 없음", "해당사항 없음", "해당사항없음")),
  NON_FACE_TO_FACE("비대면가입", Arrays.asList("비대면", "인터넷", "스마트", "온라인", "스마트폰")),
  REDEPOSIT("재예치", Arrays.asList("재예치", "자동재예치")),
  FIRST_TRANSACTION("첫거래", Arrays.asList("첫거래", "최초가입", "신규가입", "신규고객")),
  AGE_BENEFIT("연령우대", Arrays.asList("만60세", "만62세", "고령자", "만65세", "만18세")),
  TRANSACTION_PERFORMANCE("거래실적", Arrays.asList("거래실적", "실적우대", "평잔", "거래", "카드", "신용카드", "체크카드"));

  private final String displayName;
  private final List<String> keywords;

  SpclCndType(String displayName, List<String> keywords) {
    this.displayName = displayName;
    this.keywords = keywords;
  }

  public String getDisplayName() {
    return displayName;
  }

  public List<String> getKeywords() {
    return keywords;
  }

  /**
   * 우대조건 텍스트에서 해당 조건이 포함되어 있는지 확인
   */
  public boolean matchesKeywords(String spclCnd) {
    if (spclCnd == null || spclCnd.trim().isEmpty()) {
      return this == NONE;
    }

    String normalizedText = spclCnd.toLowerCase().replaceAll("\\s+", "");

    return keywords.stream()
        .anyMatch(keyword -> normalizedText.contains(keyword.toLowerCase()));
  }

  /**
   * 우대조건 텍스트를 파싱하여 해당하는 조건들 반환
   */
  public static Set<SpclCndType> parseConditions(String spclCnd) {
    if (spclCnd == null || spclCnd.trim().isEmpty()) {
      return Set.of(NONE);
    }

    // "없음" 계열 체크 먼저
    if (NONE.matchesKeywords(spclCnd)) {
      return Set.of(NONE);
    }

    Set<SpclCndType> conditions = new HashSet<>();

    for (SpclCndType type : values()) {
      if (type != NONE && type.matchesKeywords(spclCnd)) {
        conditions.add(type);
      }
    }

    return conditions.isEmpty() ? Set.of(NONE) : conditions;
  }
}
