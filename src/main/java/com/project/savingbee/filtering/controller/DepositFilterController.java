package com.project.savingbee.filtering.controller;

import com.project.savingbee.filtering.dto.DepositFilterRequest;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.dto.SortFilter;
import com.project.savingbee.filtering.service.DepositFilterService;
import com.project.savingbee.filtering.util.FilterMappingUtil;
import com.project.savingbee.filtering.util.FilterParsingUtil;
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
      @RequestParam(required = false) String finCoType,      // finCoType
      @RequestParam(required = false) String joinWay,
      @RequestParam(required = false) String joinDeny,       // "제한없음,서민전용"
      @RequestParam(required = false) String saveTrm,
      @RequestParam(required = false) String intrRateType,   // "단리,복리"
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
          finCoType, joinWay, joinDeny, saveTrm, intrRateType,
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
      String finCoType, String joinWay, String joinDeny, String saveTrm, String intrRateType,
      BigDecimal intrRateMin, BigDecimal intrRateMax, BigDecimal intrRate2Min,
      BigDecimal intrRate2Max,
      BigDecimal maxLimitMin, BigDecimal maxLimitMax, String sortField, String sortOrder,
      Integer page, Integer size) {

    // Filters 객체 생성
    DepositFilterRequest.Filters.FiltersBuilder filtersBuilder = DepositFilterRequest.Filters.builder();

    // 금융회사 유형 변환
    if (finCoType != null && !finCoType.trim().isEmpty()) {
      List<String> displayNames = FilterParsingUtil.parseStringList(finCoType);
      List<String> codes = FilterMappingUtil.convertFinancialCompanyNamesToCodes(displayNames);
      filtersBuilder.finCoNo(codes);
    }

    // 가입대상 변환
    if (joinDeny != null && !joinDeny.trim().isEmpty()) {
      List<String> displayNames = FilterParsingUtil.parseStringList(joinDeny);
      List<String> codes = FilterMappingUtil.convertJoinDenyNamesToCodes(displayNames);
      filtersBuilder.joinDeny(codes);
    }

    // 이자계산방식 변환
    if (intrRateType != null && !intrRateType.trim().isEmpty()) {
      List<String> displayNames = FilterParsingUtil.parseStringList(intrRateType);
      List<String> codes = FilterMappingUtil.convertInterestRateNamesToCodes(displayNames);
      filtersBuilder.intrRateType(codes);
    }


    // 범위 필터들 설정
    filtersBuilder.intrRate(FilterParsingUtil.buildRangeFilter(intrRateMin, intrRateMax));

    // 최고 금리 설정
    filtersBuilder.intrRate2(FilterParsingUtil.buildRangeFilter(intrRate2Min, intrRate2Max));

    // 최고 한도 설정
    filtersBuilder.maxLimit(FilterParsingUtil.buildRangeFilter(maxLimitMin, maxLimitMax));

    DepositFilterRequest.Filters filters = filtersBuilder.build();

    // Sort 객체 생성
    SortFilter sort = null;
    if (sortField != null && !sortField.trim().isEmpty()) {
      sort = SortFilter.builder()
          .field(sortField.trim())
          .order(sortOrder != null ? sortOrder.trim() : "desc")
          .build();
    }

    // 최종 요청 객체 생성
    DepositFilterRequest request = DepositFilterRequest.builder()
        .filters(filters)
        .build();

    request.setSort(sort);
    request.setPage(page);
    request.setSize(size);

    return request;
  }

}
