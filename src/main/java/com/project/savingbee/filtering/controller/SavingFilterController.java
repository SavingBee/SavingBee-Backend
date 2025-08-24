package com.project.savingbee.filtering.controller;

import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.dto.SavingFilterRequest;
import com.project.savingbee.filtering.dto.SortFilter;
import com.project.savingbee.filtering.service.SavingFilterService;
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

/**
 * 적금 상품 필터링 Controller
 */
@Slf4j
@RestController
@RequestMapping("/products/filter")
@RequiredArgsConstructor
public class SavingFilterController {

  private final SavingFilterService savingFilterService;

  @GetMapping("saving")
  public ResponseEntity<Page<ProductSummaryResponse>> filterSavingProducts(
      @RequestParam(required = false) String finCoType,
      @RequestParam(required = false) String joinWay,
      @RequestParam(required = false) String joinDeny,
      @RequestParam(required = false) String saveTrm,
      @RequestParam(required = false) String intrRateType,
      @RequestParam(required = false) String rsrvType,        // 적금 고유
      @RequestParam(required = false) Integer monthlyMaxLimit, // 적금 고유
      @RequestParam(required = false) Integer totalMaxLimit,   // 적금 고유
      @RequestParam(required = false) BigDecimal intrRateMin,
      @RequestParam(required = false) BigDecimal intrRateMax,
      @RequestParam(required = false) BigDecimal intrRate2Min,
      @RequestParam(required = false) BigDecimal intrRate2Max,
      @RequestParam(required = false) String sortField,
      @RequestParam(required = false) String sortOrder,
      @RequestParam(defaultValue = "1") Integer page) {
    // 고정 페이지 크기
    final int PAGE_SIZE = 10;

    log.info("적금 필터링 요청 - 페이지: {}, 크기: {}, 정렬: {} {}",
        page, PAGE_SIZE, sortField, sortOrder);

    try {
      // 요청 파라미터를 SavingFilterRequest로 변환
      SavingFilterRequest request = buildSavingFilterRequest(
          finCoType, joinWay, joinDeny, saveTrm, intrRateType, rsrvType,
          monthlyMaxLimit, totalMaxLimit, intrRateMin, intrRateMax,
          intrRate2Min, intrRate2Max, sortField, sortOrder, page, PAGE_SIZE);
      // 필터링 서비스 호출
      Page<ProductSummaryResponse> result = savingFilterService.savingFilter(request);

      log.info("적금 필터링 결과 - 총 {}개 상품 중 {}개 반환",
          result.getTotalElements(), result.getNumberOfElements());

      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      log.error("잘못된 요청 파라미터: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("적금 필터링 중 오류 발생", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 파라미터를 SavingFilterRequest 객체로 변환
   */
  private SavingFilterRequest buildSavingFilterRequest(String finCoType, String joinWay,
      String joinDeny, String saveTrm,
      String intrRateType, String rsrvType, Integer monthlyMaxLimit, Integer totalMaxLimit,
      BigDecimal intrRateMin, BigDecimal intrRateMax, BigDecimal intrRate2Min,
      BigDecimal intrRate2Max,
      String sortField, String sortOrder, Integer page, Integer size) {
    // 필터 객체 생성
    SavingFilterRequest.Filters.FiltersBuilder filtersBuilder = SavingFilterRequest.Filters.builder();

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

    // 적립방식 변환 로직
    if (rsrvType != null && !rsrvType.trim().isEmpty()) {
      List<String> displayNames = FilterParsingUtil.parseStringList(rsrvType);
      List<String> codes = FilterMappingUtil.convertReserveTypeNamesToCodes(displayNames);
      filtersBuilder.rsrvType(codes);
    }

    // 월 저축금 파라미터 처리
    if (monthlyMaxLimit != null) {
      filtersBuilder.monthlyMaxLimit(monthlyMaxLimit);
    }

    // 총 저축금 파라미터 처리
    if (totalMaxLimit != null) {
      filtersBuilder.totalMaxLimit(totalMaxLimit);
    }

    // 우대조건 처리
    if (joinWay != null && !joinWay.trim().isEmpty()) {
      List<String> joinWayList = FilterParsingUtil.parseStringList(joinWay);
      filtersBuilder.joinWay(joinWayList);
    }

    // 저축기간 처리
    if (saveTrm != null && !saveTrm.trim().isEmpty()) {
      List<Integer> saveTrmList = FilterParsingUtil.parseIntegerList(saveTrm);
      filtersBuilder.saveTrm(saveTrmList);
    }

    // 기봄 금리 범위
    filtersBuilder.intrRate(FilterParsingUtil.buildRangeFilter(intrRateMin, intrRateMax));

    // 우대 금리 범위
    filtersBuilder.intrRate2(FilterParsingUtil.buildRangeFilter(intrRate2Min, intrRate2Max));

    SavingFilterRequest.Filters filters = filtersBuilder.build();

    // Sort 객체 생성
    SortFilter sort = null;
    if (sortField != null && !sortField.trim().isEmpty()) {
      sort = SortFilter.builder()
          .field(sortField.trim())
          .order(sortOrder != null ? sortOrder.trim() : "desc")
          .build();
    }

    // 최종 적금 요청 객체
    SavingFilterRequest request = SavingFilterRequest.builder()
        .filters(filters)
        .build();

    request.setSort(sort);
    request.setPage(page);
    request.setSize(size);

    return request;
  }

}
