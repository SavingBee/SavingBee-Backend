package com.project.savingbee.filtering.controller;

import com.project.savingbee.filtering.dto.DepositFilterRequest;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.dto.SortFilter;
import com.project.savingbee.filtering.service.DepositFilterSearchService;
import com.project.savingbee.filtering.util.FilterMappingUtil;
import com.project.savingbee.filtering.util.FilterParsingUtil;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/products/filter")
@RequiredArgsConstructor
public class DepositFilterSearchController {

  private final DepositFilterSearchService depositFilterSearchService;

  @GetMapping("deposit/search")
  public ResponseEntity<Page<ProductSummaryResponse>> filterDepositProductsWithSearch(
      @RequestParam(required = false) String q,              // 검색어 추가
      @RequestParam(required = false) String finCoType,
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

    final int PAGE_SIZE = 10;

    log.info("예금 필터링+검색 요청 - 검색어: {}, 페이지: {}, 크기: {}, 정렬: {} {}",
        q, page, PAGE_SIZE, sortField, sortOrder);

    try {
      // 요청 파라미터를 DepositFilterRequest로 변환
      DepositFilterRequest request = buildFilterSearchRequest(
          q, finCoType, joinWay, joinDeny, saveTrm, intrRateType,
          intrRateMin, intrRateMax, intrRate2Min, intrRate2Max,
          maxLimitMin, maxLimitMax, sortField, sortOrder, page, PAGE_SIZE);

      // 필터링+검색 서비스 호출
      Page<ProductSummaryResponse> result = depositFilterSearchService.depositFilterWithSearch(request);

      log.info("예금 필터링+검색 결과 - 총 {}개 상품 중 {}개 반환",
          result.getTotalElements(), result.getNumberOfElements());

      return ResponseEntity.ok(result);

    } catch (IllegalArgumentException e) {
      log.error("잘못된 요청 파라미터: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("예금 필터링+검색 중 오류 발생", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 파라미터를 DepositFilterRequest 객체로 변환 (검색어 포함)
   */
  private DepositFilterRequest buildFilterSearchRequest(
      String q, String finCoType, String joinWay, String joinDeny, String saveTrm, String intrRateType,
      BigDecimal intrRateMin, BigDecimal intrRateMax, BigDecimal intrRate2Min,
      BigDecimal intrRate2Max,
      BigDecimal maxLimitMin, BigDecimal maxLimitMax, String sortField, String sortOrder,
      Integer page, Integer size) {

    // 최종 요청 객체 생성
    DepositFilterRequest.Filters.FiltersBuilder filtersBuilder = DepositFilterRequest.Filters.builder();

    // 금융회사 유형 변환
    if (finCoType != null && !finCoType.trim().isEmpty()) {
      List<String> displayNames = FilterParsingUtil.parseStringList(finCoType);
      List<String> codes = FilterMappingUtil.convertFinancialCompanyNamesToCodes(displayNames);
      filtersBuilder.orgTypeCode(codes);
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

    // 우대 조건 처리
    if (joinWay != null && !joinWay.trim().isEmpty()) {
      List<String> joinWayList = FilterParsingUtil.parseStringList(joinWay);
      filtersBuilder.joinWay(joinWayList);
    }

    // 저축기간 처리
    if (saveTrm != null && !saveTrm.trim().isEmpty()) {
      List<Integer> saveTrmList = FilterParsingUtil.parseIntegerList(saveTrm);
      filtersBuilder.saveTrm(saveTrmList);
    }

    // 기본 금리 범위 설정
    filtersBuilder.intrRate(FilterParsingUtil.buildRangeFilter(intrRateMin, intrRateMax));

    // 최고 금리 범위 설정
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