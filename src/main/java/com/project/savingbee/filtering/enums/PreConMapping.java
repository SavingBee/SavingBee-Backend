package com.project.savingbee.filtering.enums;

import java.util.*;

/**
 * 우대조건 매핑 조건명과 DB TEXT 검색용 키워드들 간의 매핑 SpclCndType과 연동하여 TEXT 검색에 사용
 */
public enum PreConMapping {
  NON_FACE_TO_FACE("비대면가입",
      Arrays.asList("비대면", "인터넷", "스마트", "온라인", "스마트폰")),

  REDEPOSIT("재예치",
      Arrays.asList("재예치", "자동재예치")),

  FIRST_TRANSACTION("첫거래",
      Arrays.asList("첫거래", "최초가입", "신규가입", "신규고객", "신규")),

  AGE_BENEFIT("연령우대",
      Arrays.asList("만60세", "만62세", "고령자", "만65세", "만18세")),

  TRANSACTION_PERFORMANCE("거래실적",
      Arrays.asList("거래실적", "실적우대", "평잔", "거래", "카드", "신용카드", "체크카드"));

  private final String displayName;        // 사용자 입력 조건명
  private final List<String> searchKeywords; // DB TEXT 검색용 키워드들

  PreConMapping(String displayName, List<String> searchKeywords) {
    this.displayName = displayName;
    this.searchKeywords = searchKeywords;
  }

  public String getDisplayName() {
    return displayName;
  }

  public List<String> getSearchKeywords() {
    return searchKeywords;
  }

  /**
   * 표시명으로 검색 키워드들 찾기
   */
  public static Optional<List<String>> getKeywordsByDisplayName(String displayName) {
    return Arrays.stream(values())
        .filter(mapping -> mapping.displayName.equals(displayName))
        .map(mapping -> mapping.searchKeywords)
        .findFirst();
  }

  /**
   * 검색 키워드로 표시명 찾기
   */
  public static Optional<String> getDisplayNameByKeyword(String keyword) {
    return Arrays.stream(values())
        .filter(mapping -> mapping.searchKeywords.contains(keyword))
        .map(mapping -> mapping.displayName)
        .findFirst();
  }
}
