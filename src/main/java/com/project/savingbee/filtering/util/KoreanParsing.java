package com.project.savingbee.filtering.util;

import org.springframework.stereotype.Component;


@Component
public class KoreanParsing {

  /**
   * 한국어 검색어 전처리 - 특수문자 제거 - 공백 정규화
   */
  public String processKoreanText(String input) {
    if (input == null || input.trim().isEmpty()) {
      return "";
    }

    // 한글 완성형, 자음, 모음, 영문, 숫자, 공백만 허용
    String processed = input.replaceAll("[^가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9\\s]", "");

    // 연속된 공백을 하나로 변환
    processed = processed.replaceAll("\\s+", " ");

    // 앞뒤 공백 제거
    return processed.trim();
  }
}
