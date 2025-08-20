package com.project.savingbee.filtering.controller;

import com.project.savingbee.filtering.dto.DepositFilterRequest;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.service.DepositFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 예금 상품 필터링 Controller
 */

@Slf4j
@RestController
@RequestMapping("/products/filter")
@RequiredArgsConstructor
public class DepositFilterController {

  private final DepositFilterService depositFilterService;

  @GetMapping("deposite")
  public ResponseEntity<Page<ProductSummaryResponse>> filterDepositProducts(
      @RequestParam(required = false) String finCoNo,
      @RequestParam(required = false) String joinWay,
      @RequestParam(required = false) String joinDeny,
      @RequestParam(required = false) String saveTrm,
      @RequestParam(required = false) String intrRateType,
      @RequestParam(required = false) BigDecimal intrRateMin,
      @RequestParam(required = false) BigDecimal intrRateMax,
      @RequestParam(required = false) BigDecimal intrRate2Min,
      @RequestParam(required = false) BigDecimal intrRate2Max,
      @RequestParam(required = false) BigDecimal maxLimitMin,
      @RequestParam(required = false) BigDecimal maxLimitMax,
      @RequestParam(required = false) String sortField,
      @RequestParam(required = false) String sortOrder,
      @RequestParam(defaultValue = "1") Integer page) {

    final int PAGE_SIZE = 10; // 고정 페이지 크기

    log.info("예금 필터링 요청 - 페이지: {}, 크기: {}, 정렬: {} {}",
        page, PAGE_SIZE, sortField, sortOrder);

    try {
      // 요청 파라미터를 DepositFilterRequest로 변환
      DepositFilterRequest request = buildFilterRequest(
          finCoNo, joinWay, joinDeny, saveTrm, intrRateType,
          intrRateMin, intrRateMax, intrRate2Min, intrRate2Max,
          maxLimitMin, maxLimitMax, sortField, sortOrder, page, PAGE_SIZE);

      // 필터링 서비스 호출
      Page<ProductSummaryResponse> result = depositFilterService.depositFilter(request);

      log.info("예금 필터링 결과 - 총 {}개 상품 중 {}개 반환",
          result.getTotalElements(), result.getNumberOfElements());

      return ResponseEntity.ok(result);

    } catch (IllegalArgumentException e) {
      log.error("잘못된 요청 파라미터: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("예금 필터링 중 오류 발생", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 파라미터를 DepositFilterRequest 객체로 변환
   */
  private DepositFilterRequest buildFilterRequest(
      String finCoNo, String joinWay, String joinDeny, String saveTrm, String intrRateType,
      BigDecimal intrRateMin, BigDecimal intrRateMax, BigDecimal intrRate2Min,
      BigDecimal intrRate2Max,
      BigDecimal maxLimitMin, BigDecimal maxLimitMax, String sortField, String sortOrder,
      Integer page, Integer size) {

    // Filters 객체 생성
    DepositFilterRequest.Filters.FiltersBuilder filtersBuilder = DepositFilterRequest.Filters.builder();

    // 문자열 파라미터들을 리스트로 변환
    if (finCoNo != null && !finCoNo.trim().isEmpty()) {
      filtersBuilder.finCoNo(parseStringList(finCoNo));
    }
    if (joinWay != null && !joinWay.trim().isEmpty()) {
      filtersBuilder.joinWay(parseStringList(joinWay));
    }
    if (joinDeny != null && !joinDeny.trim().isEmpty()) {
      filtersBuilder.joinDeny(parseStringList(joinDeny));
    }
    if (intrRateType != null && !intrRateType.trim().isEmpty()) {
      filtersBuilder.intrRateType(parseStringList(intrRateType));
    }

    // 정수 파라미터들을 리스트로 변환 (저축기간)
    if (saveTrm != null && !saveTrm.trim().isEmpty()) {
      filtersBuilder.saveTrm(parseIntegerList(saveTrm));
    }

    // 범위 필터들 설정
    if (intrRateMin != null || intrRateMax != null) {
      filtersBuilder.intrRate(DepositFilterRequest.RangeFilter.builder()
          .min(intrRateMin)
          .max(intrRateMax)
          .build());
    }

    if (intrRate2Min != null || intrRate2Max != null) {
      filtersBuilder.intrRate2(DepositFilterRequest.RangeFilter.builder()
          .min(intrRate2Min)
          .max(intrRate2Max)
          .build());
    }

    if (maxLimitMin != null || maxLimitMax != null) {
      filtersBuilder.maxLimit(DepositFilterRequest.RangeFilter.builder()
          .min(maxLimitMin)
          .max(maxLimitMax)
          .build());
    }

    DepositFilterRequest.Filters filters = filtersBuilder.build();

    // Sort 객체 생성
    DepositFilterRequest.Sort sort = null;
    if (sortField != null && !sortField.trim().isEmpty()) {
      sort = DepositFilterRequest.Sort.builder()
          .field(sortField.trim())
          .order(sortOrder != null ? sortOrder.trim() : "desc")
          .build();
    }

    // 최종 요청 객체 생성
    return DepositFilterRequest.builder()
        .filters(filters)
        .sort(sort)
        .page(page)
        .size(size)
        .build();
  }

  /**
   * 콤마로 구분된 문자열을 List<String>으로 변환
   */
  private List<String> parseStringList(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  /**
   * 콤마로 구분된 문자열을 List<Integer>로 변환
   */
  private List<Integer> parseIntegerList(String value) {
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

  /**
   * 간단한 헬스 체크 엔드포인트
   */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Deposit Filter API is running");
  }
}
