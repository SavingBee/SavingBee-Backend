package com.project.savingbee.filtering.service;

import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.repository.DepositProductsRepository;
import com.project.savingbee.filtering.dto.DepositFilterRequest;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositFilterService {

  private final DepositProductsRepository depositProductsRepository;

  /**
   * 예금 필터링 필터링 조건 금융권역 - 은행, 저축은행, 신협조합 우대조건 - 비대면 가입, 재예치, 첫 거래, 연령, 실적 가입대상 - 제한없음, 서민전용, 일부 제한
   * 저축기간 - 6개월, 12개월, 24개월, 36개월 이자계산 방식 - 단리, 복리 저축금 기본 금리 - 최저값 ~ 최고값 범위 최고 금리 - 최저값 ~ 최고값 범위
   */
  public Page<ProductSummaryResponse> depositFilter(DepositFilterRequest request) {
    log.info("예금 필터링 시작 - 조건: {}", request);

    // 기본값 설정
    if (request == null) {
      throw new IllegalArgumentException("필터링 요청이 null입니다.");
    }
    request.setDefaultValues();

    // 금리 정렬 여부 확인
    boolean isInterestRateSort = isInterestRateSort(request);

    if (isInterestRateSort) {
      // 금리 정렬인 경우: 서비스 레벨에서 정렬
      return filterWithRateSort(request);
    } else {
      // 일반 정렬인 경우: DB 레벨에서 정렬
      return filterWithBasicSort(request);
    }
  }

  /**
   * 금리 정렬 여부 확인
   */
  private boolean isInterestRateSort(DepositFilterRequest request) {
    if (!request.hasSort()) {
      return true; // 기본값이 금리 정렬
    }

    String sortField = request.getSort().getField();
    return "intr_rate2".equals(sortField) ||
        "max_intr_rate".equals(sortField) ||
        "intr_rate".equals(sortField) ||
        "base_intr_rate".equals(sortField);
  }

  /**
   * 서비스에서 금리 정렬 처리
   */
  private Page<ProductSummaryResponse> filterWithRateSort(DepositFilterRequest request) {
    log.debug("서비스 레벨 금리 정렬 처리 시작");

    // 1. 필터링 조건만 적용하여 모든 데이터 조회 - 정렬 없이
    Specification<DepositProducts> spec = buildFilterSpecification(request);
    List<DepositProducts> allProducts = depositProductsRepository.findAll(spec);

    // 2. 중복 제거
    List<DepositProducts> distinctProducts = removeDuplicates(allProducts);

    // 3. 서비스 레벨에서 금리 기준 정렬
    List<DepositProducts> sortedProducts = sortByInterestRate(distinctProducts, request);

    // 4. 페이징 적용
    int pageNumber = Math.max(0, request.getPageNumber() - 1);
    int pageSize = request.getPageSize();
    int start = pageNumber * pageSize;
    int end = Math.min(start + pageSize, sortedProducts.size());

    List<DepositProducts> pagedProducts = sortedProducts.subList(start, end);

    // 5. DTO 변환
    List<ProductSummaryResponse> responses = pagedProducts.stream()
        .map(this::toProductSummaryResponse)
        .collect(Collectors.toList());

    // 6. Page 객체 생성
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    log.info("서비스 레벨 정렬 결과: 총 {}개 상품 중 {}개 반환 (페이지: {}/{})",
        sortedProducts.size(), responses.size(),
        pageNumber + 1, (sortedProducts.size() + pageSize - 1) / pageSize);

    return new PageImpl<>(responses, pageable, sortedProducts.size());
  }

  /**
   * 상품명 정렬 처리
   */
  private Page<ProductSummaryResponse> filterWithBasicSort(DepositFilterRequest request) {
    log.debug("상품명 정렬 처리 시작");

    // 1. 필터링 조건 생성
    Specification<DepositProducts> spec = buildFilterSpecification(request);

    // 2. 페이징 및 정렬 설정
    Pageable pageable = createPageableForDbSort(request);

    // 3. 쿼리 실행
    Page<DepositProducts> products = depositProductsRepository.findAll(spec, pageable);

    // 4. 중복 제거
    Page<DepositProducts> distinctProducts = removeDuplicatesFromPage(products);

    log.info("DB 레벨 정렬 결과: 총 {}개 상품 중 {}개 반환 (페이지: {}/{})",
        distinctProducts.getTotalElements(), distinctProducts.getNumberOfElements(),
        distinctProducts.getNumber() + 1, distinctProducts.getTotalPages());

    // 5. DTO 변환 및 반환
    return distinctProducts.map(this::toProductSummaryResponse);
  }

  /**
   * 필터링 조건만 생성
   */
  private Specification<DepositProducts> buildFilterSpecification(DepositFilterRequest request) {
    Specification<DepositProducts> spec = Specification.where(isActiveProduct());

    // 필터 조건이 있으면 적용
    if (request.hasFilters()) {
      DepositFilterRequest.Filters filters = request.getFilters();

      // 금융회사 번호 필터
      if (filters.getFinCoNo() != null && !filters.getFinCoNo().isEmpty()) {
        spec = spec.and(filterFinCoNum(filters.getFinCoNo()));
      }

      // 가입제한 필터
      if (filters.getJoinDeny() != null && !filters.getJoinDeny().isEmpty()) {
        spec = spec.and(filterJoinDeny(filters.getJoinDeny()));
      }

      // 금리 관련 필터
      if (hasInterestRateFilters(filters)) {
        spec = spec.and(filterInterestRate(filters));
      }

      // 가입한도 범위 필터
      if (filters.getMaxLimit() != null && filters.getMaxLimit().hasAnyValue()) {
        spec = spec.and(rangeMaxLimit(filters.getMaxLimit()));
      }

      // 우대조건 필터
      if (filters.getJoinWay() != null && !filters.getJoinWay().isEmpty()) {
        spec = spec.and(hasPreferentialConditions(filters.getJoinWay()));
      }
    }

    return spec;
  }

  /**
   * 서비스 레벨에서 금리 기준 정렬
   */
  private List<DepositProducts> sortByInterestRate(List<DepositProducts> products,
      DepositFilterRequest request) {
    String sortField = request.hasSort() ? request.getSort().getField() : "intr_rate2";
    boolean isDescending = request.hasSort() ? request.getSort().isDescending() : true;

    Comparator<DepositProducts> comparator;

    switch (sortField) {
      case "intr_rate2", "max_intr_rate" -> {
        comparator = Comparator.comparing(product -> {
          if (product.getInterestRates() == null || product.getInterestRates().isEmpty()) {
            return BigDecimal.ZERO;
          }
          return product.getInterestRates().stream()
              .map(rate -> rate.getIntrRate2() != null ? rate.getIntrRate2() : rate.getIntrRate())
              .filter(rate -> rate != null)
              .max(BigDecimal::compareTo)
              .orElse(BigDecimal.ZERO);
        });
      }
      case "intr_rate", "base_intr_rate" -> {
        comparator = Comparator.comparing(product -> {
          if (product.getInterestRates() == null || product.getInterestRates().isEmpty()) {
            return BigDecimal.ZERO;
          }
          return product.getInterestRates().stream()
              .map(rate -> rate.getIntrRate())
              .filter(rate -> rate != null)
              .max(BigDecimal::compareTo)
              .orElse(BigDecimal.ZERO);
        });
      }
      default -> {
        log.warn("지원하지 않는 금리 정렬 필드: {}, 최고금리로 대체", sortField);
        comparator = Comparator.comparing(product -> {
          if (product.getInterestRates() == null || product.getInterestRates().isEmpty()) {
            return BigDecimal.ZERO;
          }
          return product.getInterestRates().stream()
              .map(rate -> rate.getIntrRate2() != null ? rate.getIntrRate2() : rate.getIntrRate())
              .filter(rate -> rate != null)
              .max(BigDecimal::compareTo)
              .orElse(BigDecimal.ZERO);
        });
      }
    }

    // 정렬 방향 적용
    if (isDescending) {
      comparator = comparator.reversed();
    }

    return products.stream()
        .sorted(comparator)
        .collect(Collectors.toList());
  }

  /**
   * 상품명 정렬 Pageable 생성
   */
  private Pageable createPageableForDbSort(DepositFilterRequest request) {
    Sort.Direction direction = Sort.Direction.ASC;
    String sortField = "finPrdtNm"; // 기본값: 상품명

    if (request.hasSort()) {
      direction = request.getSort().isDescending() ? Sort.Direction.DESC : Sort.Direction.ASC;
      String requestedField = request.getSort().getField();

      switch (requestedField) {
        case "fin_prdt_nm" -> sortField = "finPrdtNm";
        case "kor_co_nm" -> sortField = "financialCompany.korCoNm";
        case "max_limit" -> sortField = "maxLimit";
        case "dcls_strt_day" -> sortField = "dclsStrtDay";
        default -> {
          log.debug("DB 정렬 지원하지 않는 필드 '{}', 상품명으로 대체", requestedField);
          sortField = "finPrdtNm";
        }
      }
    }

    Sort sort = Sort.by(direction, sortField);
    int pageNumber = Math.max(0, request.getPageNumber() - 1);

    return PageRequest.of(pageNumber, request.getPageSize(), sort);
  }

  /**
   * 활성상품인지 확인
   */
  private Specification<DepositProducts> isActiveProduct() {
    return (root, query, cb) -> cb.equal(root.get("isActive"), true);
  }

  /**
   * 금융회사 번호 필터
   */
  private Specification<DepositProducts> filterFinCoNum(List<String> finCoNumbers) {
    return (root, query, cb) -> root.get("finCoNo").in(finCoNumbers);
  }

  /**
   * 가입 제한 필터
   */
  private Specification<DepositProducts> filterJoinDeny(List<String> joinDenyTypes) {
    return (root, query, cb) -> root.get("joinDeny").in(joinDenyTypes);
  }

  /**
   * 금리 관련 필터가 있는지 확인
   */
  private boolean hasInterestRateFilters(DepositFilterRequest.Filters filters) {
    return (filters.getSaveTrm() != null && !filters.getSaveTrm().isEmpty()) ||
        (filters.getIntrRateType() != null && !filters.getIntrRateType().isEmpty()) ||
        (filters.getIntrRate() != null && filters.getIntrRate().hasAnyValue()) ||
        (filters.getIntrRate2() != null && filters.getIntrRate2().hasAnyValue());
  }

  /**
   * 금리 관련 필터
   */
  private Specification<DepositProducts> filterInterestRate(DepositFilterRequest.Filters filters) {
    return (root, query, cb) -> {
      // 금리 조건 적용
      var interestRatesJoin = root.join("interestRates", JoinType.LEFT);

      return conditionInterestRate(filters, interestRatesJoin, cb);
    };
  }

  /**
   * 금리 및 저축 기간 조건 - 저축기간 조건 - 이자계산 방식 조건 - 기본 금리 범위 - 우대금리 범위
   */
  private Predicate conditionInterestRate(DepositFilterRequest.Filters filters,
      jakarta.persistence.criteria.Join<?, ?> interestRatesJoin,
      jakarta.persistence.criteria.CriteriaBuilder cb) {
    List<Predicate> predicates = new ArrayList<>();

    // 저축기간 조건
    if (filters.getSaveTrm() != null && !filters.getSaveTrm().isEmpty()) {
      predicates.add(interestRatesJoin.get("saveTrm").in(filters.getSaveTrm()));
    }

    // 이자계산방식 조건
    if (filters.getIntrRateType() != null && !filters.getIntrRateType().isEmpty()) {
      predicates.add(interestRatesJoin.get("intrRateType").in(filters.getIntrRateType()));
    }

    // 기본 금리 범위
    if (filters.getIntrRate() != null) {
      if (filters.getIntrRate().hasMinValue()) {
        predicates.add(cb.greaterThanOrEqualTo(
            interestRatesJoin.get("intrRate"), filters.getIntrRate().getMin()));
      }
      if (filters.getIntrRate().hasMaxValue()) {
        predicates.add(cb.lessThanOrEqualTo(
            interestRatesJoin.get("intrRate"), filters.getIntrRate().getMax()));
      }
    }

    // 우대금리 범위
    if (filters.getIntrRate2() != null) {
      if (filters.getIntrRate2().hasMinValue()) {
        predicates.add(cb.greaterThanOrEqualTo(
            interestRatesJoin.get("intrRate2"), filters.getIntrRate2().getMin()));
      }
      if (filters.getIntrRate2().hasMaxValue()) {
        predicates.add(cb.lessThanOrEqualTo(
            interestRatesJoin.get("intrRate2"), filters.getIntrRate2().getMax()));
      }
    }

    return cb.and(predicates.toArray(new Predicate[0]));
  }

  /**
   * 가입 한도 조건 확인 - 최소 한도: maxLimit이 요청 최소값이상 - 최대 한도: maxLimit이 요청 최대값 이하
   */
  private Specification<DepositProducts> rangeMaxLimit(
      DepositFilterRequest.RangeFilter maxLimitRange) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // 최소 한도: 상품의 maxLimit이 null이거나 요청한 최소값 이상
      if (maxLimitRange.hasMinValue()) {
        predicates.add(cb.or(
            cb.isNull(root.get("maxLimit")),
            cb.greaterThanOrEqualTo(root.get("maxLimit"), maxLimitRange.getMin())
        ));
      }

      // 최대 한도: 상품의 maxLimit이 null이거나 요청한 최대값 이하
      if (maxLimitRange.hasMaxValue()) {
        predicates.add(cb.or(
            cb.isNull(root.get("maxLimit")),
            cb.lessThanOrEqualTo(root.get("maxLimit"), maxLimitRange.getMax())
        ));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * 우대조건 필터
   */
  private Specification<DepositProducts> hasPreferentialConditions(List<String> joinWayConditions) {
    return (root, query, cb) -> {
      List<Predicate> conditionPredicates = new ArrayList<>();

      for (String condition : joinWayConditions) {
        // 직접 TEXT 검색
        conditionPredicates.add(cb.like(
            cb.lower(root.get("spclCnd")),
            "%" + condition.toLowerCase() + "%"
        ));
      }

      // 모든 우대조건들을 OR로 연결
      return conditionPredicates.isEmpty()
          ? cb.conjunction()
          : cb.or(conditionPredicates.toArray(new Predicate[0]));
    };
  }

  /**
   * JOIN으로 인한 중복 제거 - list
   */
  private List<DepositProducts> removeDuplicates(List<DepositProducts> products) {
    return products.stream()
        .collect(Collectors.toMap(
            DepositProducts::getFinPrdtCd,  // 상품코드로 중복 제거
            product -> product,
            (existing, replacement) -> existing,  // 중복 시 기존 것 유지
            java.util.LinkedHashMap::new  // 순서 유지
        ))
        .values()
        .stream()
        .collect(Collectors.toList());
  }

  /**
   * JOIN으로 인한 중복 제거 - page
   */
  private Page<DepositProducts> removeDuplicatesFromPage(Page<DepositProducts> products) {
    List<DepositProducts> distinctList = removeDuplicates(products.getContent());

    return new PageImpl<>(
        distinctList,
        products.getPageable(),
        products.getTotalElements()  // 전체 개수는 원래 값 유지
    );
  }

  /**
   * Entity를 Response DTO로 변환
   */
  private ProductSummaryResponse toProductSummaryResponse(DepositProducts product) {
    try {
      // 최고 금리와 기본 금리 계산 (여러 금리 옵션 중 최대값)
      BigDecimal maxIntrRate = BigDecimal.ZERO;
      BigDecimal baseIntrRate = BigDecimal.ZERO;

      if (product.getInterestRates() != null && !product.getInterestRates().isEmpty()) {
        maxIntrRate = product.getInterestRates().stream()
            .map(rate -> rate.getIntrRate2() != null ? rate.getIntrRate2() : rate.getIntrRate())
            .filter(rate -> rate != null)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        baseIntrRate = product.getInterestRates().stream()
            .map(rate -> rate.getIntrRate())
            .filter(rate -> rate != null)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
      }

      String companyName = "정보없음";
      if (product.getFinancialCompany() != null
          && product.getFinancialCompany().getKorCoNm() != null) {
        companyName = product.getFinancialCompany().getKorCoNm();
      }

      return ProductSummaryResponse.builder()
          .finPrdtCd(product.getFinPrdtCd())
          .finPrdtNm(product.getFinPrdtNm() != null ? product.getFinPrdtNm() : "상품명 정보없음")
          .korCoNm(companyName)
          .productType("deposit")
          .maxIntrRate(maxIntrRate)
          .baseIntrRate(baseIntrRate)
          .build();

    } catch (Exception e) {
      log.error("상품 정보 변환 실패 - 상품코드: {}, 오류: {}",
          product.getFinPrdtCd(), e.getMessage());

      // 오류 발생 시 기본값으로 반환
      return ProductSummaryResponse.builder()
          .finPrdtCd(product.getFinPrdtCd())
          .finPrdtNm(product.getFinPrdtNm() != null ? product.getFinPrdtNm() : "상품명 정보없음")
          .korCoNm("정보없음")
          .productType("deposit")
          .maxIntrRate(BigDecimal.ZERO)
          .baseIntrRate(BigDecimal.ZERO)
          .build();
    }
  }
}