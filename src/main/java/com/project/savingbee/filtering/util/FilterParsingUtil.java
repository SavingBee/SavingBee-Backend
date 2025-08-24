package com.project.savingbee.filtering.util;

import com.project.savingbee.filtering.dto.RangeFilter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class FilterParsingUtil {

  // 문자열을 List<String>으로 변환
  public static List<String> parseStringList(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  // List<Integer>로 변환
  public static List<Integer> parseIntegerList(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return Arrays.stream(value.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(Integer::parseInt)
          .toList();
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("저축기간은 숫자만 입력 가능합니다: " + value);
    }
  }

  public static RangeFilter buildRangeFilter(BigDecimal min, BigDecimal max) {
    if (min == null && max == null) {
      return null;
    }
    return RangeFilter.builder()
        .min(min)
        .max(max)
        .build();
  }
}
