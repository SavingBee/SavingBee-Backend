package com.project.savingbee.filtering.util;

import com.project.savingbee.filtering.enums.*;
import lombok.extern.slf4j.Slf4j;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class FilterMappingUtil {

  private FilterMappingUtil() {
    // Utility class - 인스턴스 생성 방지
  }

  /**
   * 가입대상 표시명을 코드로 변환
   * ["제한없음", "서민전용"] → ["1", "2"]
   */
  public static List<String> convertJoinDenyNamesToCodes(List<String> displayNames) {
    if (displayNames == null || displayNames.isEmpty()) {
      return new ArrayList<>();
    }

    List<String> codes = new ArrayList<>();
    for (String displayName : displayNames) {
      Optional<String> code = JoinDenyType.getCodeByDisplayName(displayName);
      if (code.isPresent()) {
        codes.add(code.get());
        log.debug("가입대상 변환: '{}' → '{}'", displayName, code.get());
      } else {
        log.warn("알 수 없는 가입대상: '{}'", displayName);
        // 알 수 없는 값은 그대로 추가 (혹시 코드값으로 직접 온 경우)
        codes.add(displayName);
      }
    }
    return codes;
  }

  /**
   * 금융회사 유형 표시명을 기관유형코드로 변환
   * ["은행", "저축은행"] → ["020000", "030300"]
   */
  public static List<String> convertFinancialCompanyNamesToCodes(List<String> displayNames) {
    if (displayNames == null || displayNames.isEmpty()) {
      return new ArrayList<>();
    }

    List<String> codes = new ArrayList<>();
    for (String displayName : displayNames) {
      Optional<String> code = FinancialCompanyType.getCodeByDisplayName(displayName);
      if (code.isPresent()) {
        codes.add(code.get());
        log.debug("금융회사유형 변환: '{}' → '{}'", displayName, code.get());
      } else {
        log.warn("알 수 없는 금융회사유형: '{}'", displayName);
        codes.add(displayName);
      }
    }
    return codes;
  }

  /**
   * 이자계산방식 표시명을 코드로 변환
   * ["단리", "복리"] → ["S", "M"]
   */
  public static List<String> convertInterestRateNamesToCodes(List<String> displayNames) {
    if (displayNames == null || displayNames.isEmpty()) {
      return new ArrayList<>();
    }

    List<String> codes = new ArrayList<>();
    for (String displayName : displayNames) {
      Optional<String> code = InterestRateType.getCodeByDisplayName(displayName);
      if (code.isPresent()) {
        codes.add(code.get());
        log.debug("이자계산방식 변환: '{}' → '{}'", displayName, code.get());
      } else {
        log.warn("알 수 없는 이자계산방식: '{}'", displayName);
        codes.add(displayName);
      }
    }
    return codes;
  }

  /**
   * 적립방식 표시명을 코드로 변환
   * ["정액적립식", "자유적립식"] → ["S", "F"]
   */
  public static List<String> convertReserveTypeNamesToCodes(List<String> displayNames) {
    if (displayNames == null || displayNames.isEmpty()) {
      return new ArrayList<>();
    }

    List<String> codes = new ArrayList<>();
    for (String displayName : displayNames) {
      Optional<String> code = ReserveType.getCodeByDisplayName(displayName);
      if (code.isPresent()) {
        codes.add(code.get());
        log.debug("적립방식 변환: '{}' → '{}'", displayName, code.get());
      } else {
        log.warn("알 수 없는 적립방식: '{}'", displayName);
        codes.add(displayName);
      }
    }
    return codes;
  }

  /**
   * 우대조건 표시명을 TEXT 검색 키워드들로 변환
   * ["비대면가입", "재예치"] → [["비대면", "인터넷", "스마트"], ["재예치", "자동재예치"]]
   */
  public static List<List<String>> convertPreferentialNameToKeywords(List<String> displayNames) {
    if (displayNames == null || displayNames.isEmpty()) {
      return new ArrayList<>();
    }

    List<List<String>> keywordsList = new ArrayList<>();
    for (String displayName : displayNames) {
      Optional<List<String>> keywords = PreConMapping.getKeywordsByDisplayName(displayName);
      if (keywords.isPresent()) {
        keywordsList.add(keywords.get());
        log.debug("우대조건 변환: '{}' → {}", displayName, keywords.get());
      } else {
        log.warn("알 수 없는 우대조건: '{}'", displayName);
        // 알 수 없는 조건은 그대로 키워드로 사용
        keywordsList.add(List.of(displayName));
      }
    }
    return keywordsList;
  }

  /**
   * 모든 예금 조건의 매핑 정보를 한번에 변환
   */
  public static DepositFilterMappingResult convertAllDepositFilters(
      List<String> joinDenyDisplayNames,
      List<String> finCoDisplayNames,
      List<String> intrRateTypeDisplayNames,
      List<String> preferentialDisplayNames) {

    return DepositFilterMappingResult.builder()
        .joinDenyCodes(convertJoinDenyNamesToCodes(joinDenyDisplayNames))
        .finCoCodes(convertFinancialCompanyNamesToCodes(finCoDisplayNames))
        .intrRateTypeCodes(convertInterestRateNamesToCodes(intrRateTypeDisplayNames))
        .preferentialKeywords(convertPreferentialNameToKeywords(preferentialDisplayNames))
        .build();
  }

  /**
   * 모든 적금 조건의 매핑 정보를 한번에 변환
   */
  public static SavingFilterMappingResult convertAllSavingFilters(
      List<String> joinDenyDisplayNames,
      List<String> finCoDisplayNames,
      List<String> intrRateTypeDisplayNames,
      List<String> rsrvTypeDisplayNames,
      List<String> preferentialDisplayNames) {

    return SavingFilterMappingResult.builder()
        .joinDenyCodes(convertJoinDenyNamesToCodes(joinDenyDisplayNames))
        .finCoCodes(convertFinancialCompanyNamesToCodes(finCoDisplayNames))
        .intrRateTypeCodes(convertInterestRateNamesToCodes(intrRateTypeDisplayNames))
        .rsrvTypeCodes(convertReserveTypeNamesToCodes(rsrvTypeDisplayNames))
        .preferentialKeywords(convertPreferentialNameToKeywords(preferentialDisplayNames))
        .build();
  }

  /**
   * 역변환: 코드를 표시명으로 변환
   */
  public static String convertJoinDenyCodeToDisplayName(String code) {
    return JoinDenyType.getDisplayNameByCode(code).orElse(code);
  }

  public static String convertFinancialCompanyCodeToDisplayName(String code) {
    return FinancialCompanyType.getDisplayNameByCode(code).orElse(code);
  }

  public static String convertInterestRateCodeToDisplayName(String code) {
    return InterestRateType.getDisplayNameByCode(code).orElse(code);
  }

  public static String convertReserveTypeCodeToDisplayName(String code) {
    return ReserveType.getDisplayNameByCode(code).orElse(code);
  }

  /**
   * 예금 필터 조건명과 DB 코드 간 변환 결과를 담는 DTO
   */
  @Data
  @Builder
  public static class DepositFilterMappingResult {

    private List<String> joinDenyCodes;           // 가입대상 코드들
    private List<String> finCoCodes;              // 금융회사 유형 코드들
    private List<String> intrRateTypeCodes;       // 이자계산방식 코드들
    private List<List<String>> preferentialKeywords; // 우대조건 검색 키워드들
  }

  /**
   * 적금 필터 조건명과 DB 코드 간 변환 결과를 담는 DTO
   */
  @Data
  @Builder
  public static class SavingFilterMappingResult {

    private List<String> joinDenyCodes;           // 가입대상 코드들
    private List<String> finCoCodes;              // 금융회사 유형 코드들
    private List<String> intrRateTypeCodes;       // 이자계산방식 코드들
    private List<String> rsrvTypeCodes;           // 적립방식 코드들
    private List<List<String>> preferentialKeywords; // 우대조건 검색 키워드들
  }

}