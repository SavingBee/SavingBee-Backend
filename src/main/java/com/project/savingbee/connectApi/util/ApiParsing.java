package com.project.savingbee.connectApi.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API 데이터 파싱 유틸리티 클래스
 * 예금, 적금 API 데이터 파싱 시 공통으로 사용
 */
@Slf4j
public class ApiParsing {

  // 날짜 포맷터 (yyyyMMdd)
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  // 금액 파싱용 정규식 패턴들
  private static final Pattern MONTHLY_LIMIT_PATTERN = Pattern.compile(
      "월\\s*(?:적립액|가입한도|한도)\\s*(?::|：)?\\s*(?:최?소?\\s*)?([0-9]+(?:만원?|원?)?)\\s*(?:이상\\s*)?(?:~|-)??\\s*(?:최?대?\\s*)?([0-9]+(?:만원?|원?)?)?"
  );

  private static final Pattern MIN_MAX_PATTERN = Pattern.compile(
      "(?:최소|최저)\\s*([0-9]+(?:만원?|원?))\\s*이상.*?(?:최대|최고)\\s*([0-9]+(?:만원?|원?))"
  );

  /**
   * 날짜 문자열을 LocalDate로 파싱
   * @param dateStr 날짜 문자열 (yyyyMMdd 형식)
   * @return LocalDate 객체 또는 null
   */
  public static LocalDate parseDate(String dateStr) {
    if (dateStr == null || dateStr.trim().isEmpty()) {
      return null;
    }
    try {
      return LocalDate.parse(dateStr, DATE_FORMATTER);
    } catch (Exception e) {
      log.warn("날짜 파싱 실패: {}", dateStr);
      return null;
    }
  }

  /**
   * 문자열을 Integer로 파싱
   * @param value 파싱할 값 (String 또는 Number)
   * @return Integer 객체 또는 null
   */
  public static Integer parseInteger(Object value) {
    if (value == null) {
      return null;
    }
    try {
      if (value instanceof Number) {
        return ((Number) value).intValue();
      }
      String strValue = value.toString().trim();
      if (strValue.isEmpty()) {
        return null;
      }
      return Integer.valueOf(strValue);
    } catch (NumberFormatException e) {
      log.warn("정수 파싱 실패: {}", value);
      return null;
    }
  }

  /**
   * 값을 BigDecimal로 파싱
   * @param value 파싱할 값 (String, Number, Long 등)
   * @return BigDecimal 객체 또는 null
   */
  public static BigDecimal parseBigDecimal(Object value) {
    if (value == null) {
      return null;
    }
    try {
      if (value instanceof Number) {
        return BigDecimal.valueOf(((Number) value).doubleValue());
      }
      String strValue = value.toString().trim();
      if (strValue.isEmpty()) {
        return null;
      }
      return new BigDecimal(strValue);
    } catch (NumberFormatException e) {
      log.warn("BigDecimal 파싱 실패: {}", value);
      return null;
    }
  }

  /**
   * Long 값을 BigDecimal로 파싱 (maxLimit 등의 큰 숫자 처리용)
   * @param value Long 값
   * @return BigDecimal 객체 또는 null
   */
  public static BigDecimal parseBigDecimalFromLong(Long value) {
    if (value == null) {
      return null;
    }
    try {
      return BigDecimal.valueOf(value);
    } catch (Exception e) {
      log.warn("Long to BigDecimal 파싱 실패: {}", value);
      return null;
    }
  }

  /**
   * 기타유의사항 텍스트에서 월 최소 적립금액 파싱
   * @param etcNote 기타유의사항 텍스트
   * @return 월 최소 적립금액 (원 단위)
   */
  public static BigDecimal parseMonthlyLimitMin(String etcNote) {
    if (etcNote == null || etcNote.trim().isEmpty()) {
      return null;
    }

    try {
      // 패턴 1: "월 적립액 5만원이상 1천만원이하"
      Matcher matcher = MONTHLY_LIMIT_PATTERN.matcher(etcNote);
      if (matcher.find()) {
        String minAmount = matcher.group(1);
        if (minAmount != null) {
          return parseAmountString(minAmount);
        }
      }

      // 패턴 2: "최소 10만원 이상 최대 100만원 이하"
      Matcher minMaxMatcher = MIN_MAX_PATTERN.matcher(etcNote);
      if (minMaxMatcher.find()) {
        String minAmount = minMaxMatcher.group(1);
        if (minAmount != null) {
          return parseAmountString(minAmount);
        }
      }

      return null;
    } catch (Exception e) {
      log.warn("월 최소 적립금액 파싱 실패: {}", etcNote);
      return null;
    }
  }

  /**
   * 기타유의사항 텍스트에서 월 최대 적립금액 파싱
   * @param etcNote 기타유의사항 텍스트
   * @return 월 최대 적립금액 (원 단위)
   */
  public static BigDecimal parseMonthlyLimitMax(String etcNote) {
    if (etcNote == null || etcNote.trim().isEmpty()) {
      return null;
    }

    try {
      // 패턴 1: "월 적립액 5만원이상 1천만원이하"
      Matcher matcher = MONTHLY_LIMIT_PATTERN.matcher(etcNote);
      if (matcher.find()) {
        String maxAmount = matcher.group(2);
        if (maxAmount != null) {
          return parseAmountString(maxAmount);
        }
      }

      // 패턴 2: "최소 10만원 이상 최대 100만원 이하"
      Matcher minMaxMatcher = MIN_MAX_PATTERN.matcher(etcNote);
      if (minMaxMatcher.find()) {
        String maxAmount = minMaxMatcher.group(2);
        if (maxAmount != null) {
          return parseAmountString(maxAmount);
        }
      }

      return null;
    } catch (Exception e) {
      log.warn("월 최대 적립금액 파싱 실패: {}", etcNote);
      return null;
    }
  }

  /**
   * 금액 문자열을 BigDecimal로 변환 ("100만원" -> 1000000)
   * @param amountStr 금액 문자열
   * @return BigDecimal 금액 (원 단위)
   */
  private static BigDecimal parseAmountString(String amountStr) {
    if (amountStr == null || amountStr.trim().isEmpty()) {
      return null;
    }

    try {
      // 숫자와 단위 분리
      String cleanStr = amountStr.replaceAll("[^0-9만천백십억]", "");

      if (cleanStr.contains("만")) {
        // "100만" -> 100 * 10000
        String number = cleanStr.replace("만", "");
        if (!number.isEmpty()) {
          return new BigDecimal(number).multiply(BigDecimal.valueOf(10000));
        }
      } else if (cleanStr.contains("천")) {
        // "500천" -> 500 * 1000
        String number = cleanStr.replace("천", "");
        if (!number.isEmpty()) {
          return new BigDecimal(number).multiply(BigDecimal.valueOf(1000));
        }
      } else if (cleanStr.contains("백")) {
        // "5백" -> 5 * 100
        String number = cleanStr.replace("백", "");
        if (!number.isEmpty()) {
          return new BigDecimal(number).multiply(BigDecimal.valueOf(100));
        }
      } else {
        // 순수 숫자
        if (!cleanStr.isEmpty()) {
          return new BigDecimal(cleanStr);
        }
      }

      return null;
    } catch (Exception e) {
      log.warn("금액 문자열 파싱 실패: {}", amountStr);
      return null;
    }
  }
}