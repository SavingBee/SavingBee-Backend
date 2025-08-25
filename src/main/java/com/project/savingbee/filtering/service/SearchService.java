package com.project.savingbee.filtering.service;

import com.project.savingbee.common.entity.*;
import com.project.savingbee.common.repository.*;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.util.KoreanParsing;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * 금융 상품 이름으로 상품 검색
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

  private final DepositProductsRepository depositProductsRepository;
  private final SavingsProductsRepository savingsProductsRepository;
  private final KoreanParsing koreanParsing;

  // 인기 상품 캐시 (사용자들이 확인했던 상품)
  private final Set<String> viewedProductsCache = new HashSet<>();

  /**
   * 상품 조회수 증가 - 상세 조회(DetailService) 시 호출
   */
  public void addToViewedProductsCache(String productCode) {
    if (productCode != null && !productCode.trim().isEmpty()) {
      viewedProductsCache.add(productCode);
      log.debug("상품이 인기 상품 캐시에 추가되었습니다: {}", productCode);
    }
  }

  /**
   * 상품 검색
   */
  public ResponseEntity<?> searchProduct(String productName) {
    // 검색어 유효성 검사
    if (productName == null || productName.trim().length() < 2) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "검색어는 2자 이상 입력해주세요"));
    }

    // 한국어 검색어 전처리
    String processedName = koreanParsing.processKoreanText(productName);
    try {
      // productName과 동일한 finPrdtNm이 있을 경우 - 성공(200)
      List<ProductSummaryResponse> searchResults = new ArrayList<>();

      // 예금 DepositProducts 에서 찾기
      List<DepositProducts> depositMatches = depositProductsRepository
          .findByFinPrdtNmContainingIgnoreCaseAndIsActiveTrue(processedName);

      // 적금 SavingsProducts 에서 찾기
      List<SavingsProducts> savingsMatches = savingsProductsRepository
          .findByFinPrdtNmContainingIgnoreCaseAndIsActiveTrue(processedName);

      // 같은 건 모두 반환
      searchResults.addAll(depositMatches.stream()
          .map(this::convertDepositToResponse)
          .collect(Collectors.toList()));

      searchResults.addAll(savingsMatches.stream()
          .map(this::convertSavingsToResponse)
          .collect(Collectors.toList()));

      if (!searchResults.isEmpty()) {
        // 검색 결과가 있는 경우 - 캐시에 추가
        searchResults.forEach(product ->
            viewedProductsCache.add(product.getFinPrdtCd()));

        return ResponseEntity.ok(Map.of(
            "products", searchResults,
            "totalCount", searchResults.size(),
            "searchTerm", processedName
        ));
      }

      // productName과 동일한 finPrdtNm이 없을 경우 - 성공(200)
      // 인기있는 상품 3개 반환
      List<ProductSummaryResponse> popularProducts = popularProduct();

      return ResponseEntity.ok(Map.of(
          "products", Collections.emptyList(),
          "popularProducts", popularProducts,
          "totalCount", 0,
          "message", "검색 결과가 없어 인기 상품을 추천합니다"
      ));

    } catch (Exception e) {
      // 검색 실패 - 실패(400)
      return ResponseEntity.badRequest()
          .body(Map.of("error", "검색 중 오류가 발생했습니다: " + e.getMessage()));
    }
  }

  /**
   * 인기 있는 상품 3개 반환 - 사용자들이 확인했던 상품을 캐싱해두어 사용
   */
  private List<ProductSummaryResponse> popularProduct() {
    List<ProductSummaryResponse> popularProducts = new ArrayList<>();

    // 사용자들이 확인했던 상품이 3가지가 넘을 경우
    if (viewedProductsCache.size() >= 3) {
      // 데이터 중 랜덤하게 3개 반환
      List<String> viewedList = new ArrayList<>(viewedProductsCache);
      Collections.shuffle(viewedList);

      for (int i = 0; i < 3 && i < viewedList.size(); i++) {
        String productCode = viewedList.get(i);
        ProductSummaryResponse product = findProductByCode(productCode);
        if (product != null) {
          popularProducts.add(product);
        }
      }
      return popularProducts;
    }

    // 사용자들이 확인했던 상품이 3개가 넘지 않을 경우
    List<ProductSummaryResponse> viewedProducts = new ArrayList<>();
    for (String productCode : viewedProductsCache) {
      ProductSummaryResponse product = findProductByCode(productCode);
      if (product != null) {
        viewedProducts.add(product);
      }
    }

    if (viewedProductsCache.isEmpty()) {
      // 사용자들이 확인했던 상품이 아예 없을 경우
      // 최고 금리가 높은 예금 1가지, 적금 2가지 반환
      popularProducts.addAll(getTopDepositProducts(1));
      popularProducts.addAll(getTopSavingsProducts(2));

    } else if (viewedProductsCache.size() == 1) {
      // 사용자들이 확인했던 상품이 1개 존재할 경우
      // 해당 상품과 최고 금리가 높은 예금 1가지, 적금 1가지 반환
      popularProducts.addAll(viewedProducts);
      popularProducts.addAll(getTopDepositProducts(1));
      popularProducts.addAll(getTopSavingsProducts(1));

    } else if (viewedProductsCache.size() == 2) {
      // 사용자들이 확인했던 상품이 2개 존재할 경우
      // 해당 상품들과 최고 금리가 높은 적금 1가지 반환
      popularProducts.addAll(viewedProducts);
      popularProducts.addAll(getTopSavingsProducts(1));
    }

    return popularProducts.stream().limit(3).collect(Collectors.toList());
  }

  // 예금 상품을 응답 DTO로 변환
  private ProductSummaryResponse convertDepositToResponse(DepositProducts deposit) {
    // 금융회사명 안전하게 가져오기
    String companyName = "정보없음";
    if (deposit.getFinancialCompany() != null
        && deposit.getFinancialCompany().getKorCoNm() != null) {
      companyName = deposit.getFinancialCompany().getKorCoNm();
    }

    // 최고 우대 금리 계산
    BigDecimal maxRate = deposit.getInterestRates().stream()
        .map(rate -> rate.getIntrRate2())
        .max(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);

    // 기본 금리 계산
    BigDecimal baseRate = deposit.getInterestRates().stream()
        .map(rate -> rate.getIntrRate())
        .max(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);

    return ProductSummaryResponse.builder()
        .finPrdtCd(deposit.getFinPrdtCd())
        .finPrdtNm(deposit.getFinPrdtNm())
        .korCoNm(deposit.getFinancialCompany().getKorCoNm())
        .productType("deposit")
        .maxIntrRate(maxRate)
        .baseIntrRate(baseRate)
        .build();
  }

  // 적금 상품을 응답 DTO로 변환
  private ProductSummaryResponse convertSavingsToResponse(SavingsProducts savings) {
    // 금융회사명 안전하게 가져오기
    String companyName = "정보없음";
    if (savings.getFinancialCompany() != null && savings.getFinancialCompany().getKorCoNm() != null) {
      companyName = savings.getFinancialCompany().getKorCoNm();
    }

    // 최고 우대 금리 계산
    BigDecimal maxRate = savings.getInterestRates().stream()
        .map(rate -> rate.getIntrRate2())
        .max(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);

    // 기본 금리 계산
    BigDecimal baseRate = savings.getInterestRates().stream()
        .map(rate -> rate.getIntrRate())
        .max(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);

    return ProductSummaryResponse.builder()
        .finPrdtCd(savings.getFinPrdtCd())
        .finPrdtNm(savings.getFinPrdtNm())
        .korCoNm(savings.getFinancialCompany().getKorCoNm())
        .productType("saving")
        .maxIntrRate(maxRate)
        .baseIntrRate(baseRate)
        .build();
  }

  // 상품 코드로 상품 찾기
  private ProductSummaryResponse findProductByCode(String productCode) {
    // 예금 상품에서 찾기
    Optional<DepositProducts> deposit = depositProductsRepository.findById(productCode);
    if (deposit.isPresent()) {
      return convertDepositToResponse(deposit.get());
    }

    // 적금 상품에서 찾기
    Optional<SavingsProducts> savings = savingsProductsRepository.findById(productCode);
    if (savings.isPresent()) {
      return convertSavingsToResponse(savings.get());
    }

    return null;
  }

  // 최고 금리 예금 상품 조회
  private List<ProductSummaryResponse> getTopDepositProducts(int limit) {
    return depositProductsRepository.findByIsActiveTrueOrderByCreatedAtDesc()
        .stream()
        .limit(limit)
        .map(this::convertDepositToResponse)
        .collect(Collectors.toList());
  }

  // 최고 금리 적금 상품 조회
  private List<ProductSummaryResponse> getTopSavingsProducts(int limit) {
    return savingsProductsRepository.findByIsActiveTrueOrderByCreatedAtDesc()
        .stream()
        .limit(limit)
        .map(this::convertSavingsToResponse)
        .collect(Collectors.toList());
  }
}
