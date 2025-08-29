package com.project.savingbee.filtering.service;

import com.project.savingbee.filtering.dto.DepositFilterRequest;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.util.KoreanParsing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositFilterSearchService {

  private final DepositFilterService depositFilterService;
  private final KoreanParsing koreanParsing;

  /**
   * 필터링 -> 검색어 포함되었는지 확인
   */
  public ProductSummaryResponse depositFilterWithSearch(DepositFilterRequest request){
    log.info("예금 필터링에 검색어 조건 추가 - 검색어 {}",request.getQ());

    // 1. 기존 필터링 수행
    DepositFilterRequest chooseRequest = chooseRequsetFiltering(request);
    Page<ProductSummaryResponse> filteredProducts = depositFilterService.depositFilter(chooseRequest);

    // 2. 검색어 체크 - 검색어가 없으면 필터링 결과만 반환
    // 3. 검색어 전처리
    // 4. 필터링된 상품들에서 해당 검색어를 포함하는 상품찾기
      // 4-1. 검색 결과 있음: 검색 결과만 반환
      // 4-2. 검색 결과 없음: null 반환
  }

  private chooseRequsetFiltering(DepositFilterRequest request){
    //
  }
}
