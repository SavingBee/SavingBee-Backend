package com.project.savingbee.filtering.service;

import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.filtering.dto.*;
import com.project.savingbee.filtering.enums.PreConMapping;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingFilterService extends BaseFilterService<SavingsProducts, SavingFilterRequest> {

  private final SavingsProductsRepository savingsProductsRepository;

  /**
   * 적금 필터링
   * 금융권역 - 은행, 저축은행, 신협조합
   * 우대조건 - 비대면 가입, 재예치, 첫거래, 연령, 실적
   * 가입대상 - 제한 없음, 서민전용, 일부 제한
   * 저축기간 - 6개월, 12개월, 24개월, 36개월
   * 이자계산 방식 - 단리, 복리
   * 적립방식 - 정액적립식, 자유적립식,전체
   * 월 저축금
   * 총 저축금
   * 기본 금리 - 최저값, 최고값 최고
   * 금리 - 최저값, 최고값
   */

  public Page<ProductSummaryResponse> savingFilter(SavingFilterRequest request) {
    log.info("적금 필터링 시작 - 조건:{}", request);

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
   * 서비스에서 금리 정렬 처리
   */
  private Page<ProductSummaryResponse> filterWithRateSort(SavingFilterRequest request) {
    // 필터링 조건만 적용하여 모든 데이터 조회
    Specification<SavingsProducts> spec = buildFilterSpecification(request);
    List<SavingsProducts> allProducts = savingsProductsRepository.findAll(spec);
    // 중복 제거
    List<SavingsProducts> distinctProducts = removeDuplicates(allProducts);
    // 서비스 레벨에서 금리 기준 정렬
    List<SavingsProducts> sortedProducts = sortByInterestRate(distinctProducts, request);
    // 페이징 적용
    int pageNumber = Math.max(0, request.getPageNumber() - 1);
    int pageSize = request.getPageSize();
    int start = pageNumber * pageSize;
    int end = Math.min(start + pageSize, sortedProducts.size());

    List<SavingsProducts> pagedProducts = sortedProducts.subList(start, end);
    // DTO 변환
    List<ProductSummaryResponse> responses = pagedProducts.stream()
        .map(this::toProductSummaryResponse)
        .collect(Collectors.toList());
    // Page 객체 생성
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    log.info("서비스 레벨 정렬 결과: 총 {}개 상품 중 {}개 반환 (페이지: {}/{})",
        sortedProducts.size(), responses.size(),
        pageNumber + 1, (sortedProducts.size() + pageSize - 1) / pageSize);

    return new PageImpl<>(responses, pageable, sortedProducts.size());
  }

  /**
   * 상품명 정렬 처리
   */
  private Page<ProductSummaryResponse> filterWithBasicSort(SavingFilterRequest request) {
    // 페이징 조건 생성
    Specification<SavingsProducts> spec = buildFilterSpecification(request);
    // 페이징 및 정렬 설정
    Pageable pageable = createPageableForDbSort(request);
    // 쿼리 실행
    Page<SavingsProducts> products = savingsProductsRepository.findAll(spec, pageable);
    // 중복 제거
    Page<SavingsProducts> distinctProducts = removeDuplicatesFromPage(products);

    log.info("DB 레벨 정렬 결과: 총 {}개 상품 중 {}개 반환 (페이지: {}/{})",
        distinctProducts.getTotalElements(), distinctProducts.getNumberOfElements(),
        distinctProducts.getNumber() + 1, distinctProducts.getTotalPages());
    // DTO 변환 및 반환
    return distinctProducts.map(this::toProductSummaryResponse);
  }

  /**
   * 필터링 조건만 생성
   */
  private Specification<SavingsProducts> buildFilterSpecification(SavingFilterRequest request) {
    Specification<SavingsProducts> spec = isActiveProduct();

    // 필터가 없으면 JOIN없이 기본 조건만 반환
    if (!request.hasFilters()) {
      return spec;
    }

    SavingFilterRequest.Filters filters = request.getFilters();

    // 금융회사 번호 필터
    if (filters.getFinCoNo() != null && !filters.getFinCoNo().isEmpty()) {
      spec = spec.and(filterFinCoNum(filters.getFinCoNo()));
    }

    // 가입 제한 필터
    if (filters.getJoinDeny() != null && !filters.getJoinDeny().isEmpty()) {
      spec = spec.and(filterJoinDeny(filters.getJoinDeny()));
    }

    // 월 저축금 한도 필터
    if (filters.getMonthlyMaxLimit() != null) {
      spec = spec.and(monthlyMaxLimit(filters.getMonthlyMaxLimit()));
    }

    // 우대 조건 필터
    if (filters.getJoinWay() != null && !filters.getJoinWay().isEmpty()) {
      spec = spec.and(hasPreferentialConditions(filters.getJoinWay()));
    }

    // 금리 관련 필터만 JOIN으로 처리
    if (hasInterestRateFilters(filters)) {
      spec = spec.and(filterInterestRate(filters));
    }
    return spec;
  }

  /**
   * 서비스 레벨에서 금리 기준 정렬
   */
  private List<SavingsProducts> sortByInterestRate(List<SavingsProducts> products,
      SavingFilterRequest request) {
    // 최고 금리 정렬
    String sortField = request.hasSort() ? request.getSort().getField() : "intr_rate2";
    boolean isDescending = request.hasSort() ? request.getSort().isDescending() : true;

    Comparator<SavingsProducts> comparator;

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
      // 기본 금리 정렬
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
      // 디폴트: 최고 금리
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

    // 정렬 방향
    if (isDescending) {
      comparator = comparator.reversed();
    }

    return products.stream()
        .sorted(comparator)
        .collect(Collectors.toList());
  }

  /**
   * 활성 상품 확인
   */
  private Specification<SavingsProducts> isActiveProduct() {
    return (root, query, cb) -> cb.equal(root.get("isActive"), true);
  }

  /**
   * 금융회사 번호 필터
   */
  private Specification<SavingsProducts> filterFinCoNum(List<String> orgTypeCodes){
    return (root, query, cb) -> {
      // 금융회사 테이블과 조인
      var financialCompanyJoin = root.join("financialCompany", JoinType.INNER);
      return financialCompanyJoin.get("orgTypeCode").in(orgTypeCodes);
    };
  }

  /**
   * 가입 제한 필터
   */
  private Specification<SavingsProducts> filterJoinDeny(List<String> joinDenyTypes) {
    return (root, query, cb) -> root.get("joinDeny").in(joinDenyTypes);
  }

  /**
   * 금리 관련 필터가 있는지 확인
   */
  private boolean hasInterestRateFilters(SavingFilterRequest.Filters filters) {
    return (filters.getSaveTrm() != null && !filters.getSaveTrm().isEmpty()) ||
        (filters.getIntrRateType() != null && !filters.getIntrRateType().isEmpty()) ||
        (filters.getRsrvType() != null && !filters.getRsrvType().isEmpty()) ||
        (filters.getIntrRate() != null && filters.getIntrRate().hasAnyValue()) ||
        (filters.getIntrRate2() != null && filters.getIntrRate2().hasAnyValue()) ||
        (filters.getTotalMaxLimit() != null);
  }

  /**
   * 금리 관련 필터
   */
  private Specification<SavingsProducts> filterInterestRate(SavingFilterRequest.Filters filters) {
    return (root, query, cb) -> {
      // 금리 조건 적용
      var interestRatesJoin = root.join("interestRates", JoinType.LEFT);
      return conditionInterestRate(filters, root, interestRatesJoin, cb);
    };
  }

  /**
   * 금리, 저축 기간, 총 저축금 조건
   */
  private Predicate conditionInterestRate(SavingFilterRequest.Filters filters,
      jakarta.persistence.criteria.Root<?> root,
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

    // 적립방식 조건
    if (filters.getRsrvType() != null && !filters.getRsrvType().isEmpty()) {
      predicates.add(interestRatesJoin.get("rsrvType").in(filters.getRsrvType()));
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

    // 총 저축금 조건
    if (filters.getTotalMaxLimit() != null) {
      if (filters.getSaveTrm() != null && !filters.getSaveTrm().isEmpty()) {
        // 1. 저축기간을 입력한 경우: 입력된 기간들에 대해 계산
        List<Predicate> totalPredicates = new ArrayList<>(); // 사용자가 선택한 기간들 리스트
        for (Integer period : filters.getSaveTrm()) {
          var calculatedTotal = cb.prod(
              cb.coalesce(root.get("maxLimit"), Integer.MAX_VALUE), // 해당 기간의 월 최대 한도
              period
          );
          totalPredicates.add(cb.and(
              cb.equal(interestRatesJoin.get("saveTrm"), period),
              cb.greaterThanOrEqualTo(calculatedTotal, filters.getTotalMaxLimit())
          ));
        }
        predicates.add(cb.or(
            cb.isNull(root.get("maxLimit")), // 제한없음
            cb.or(totalPredicates.toArray(new Predicate[0]))
        ));
      } else {
        // 2. 저축기간 미입력: 현재 JOIN된 기간으로 계산
        var calculatedTotal = cb.prod(  // 해당 상품에서 최대 기간으로 최대 한도 계산
            cb.coalesce(root.get("maxLimit"), Integer.MAX_VALUE),
            interestRatesJoin.get("saveTrm")
        );
        predicates.add(cb.or(
            cb.isNull(root.get("maxLimit")), // 제한없음
            cb.greaterThanOrEqualTo(calculatedTotal, filters.getTotalMaxLimit())
        ));
      }
    }

    return cb.and(predicates.toArray(new Predicate[0]));
  }

  /**
   * 월 저축금 확인
   */
  private Specification<SavingsProducts> monthlyMaxLimit(Integer monthlyLimit) {
    // 사용자가 입력한 월 저축금보다 상품의 월 저축금(MaxLimit)이 커야함
    return (root, query, cb) -> {
      return cb.or(
          cb.isNull(root.get("maxLimit")),  // 제한없음인 상품 - maxLimit이 null
          cb.greaterThanOrEqualTo(root.get("maxLimit"), monthlyLimit)  // 월 저축금이 요청금액 이상인 상품
      );
    };
  }

  // 우대조건 필터
  private Specification<SavingsProducts> hasPreferentialConditions(List<String> joinWayConditions) {
    return (root, query, cb) -> {
      List<Predicate> conditionPredicates = new ArrayList<>();

      for (String condition : joinWayConditions) {
        // PreConMapping에서 키워드들 찾기
        Optional<List<String>> keywords = PreConMapping.getKeywordsByDisplayName(condition);

        if (keywords.isPresent()) {
          // 매핑된 키워드들로 OR 검색
          List<Predicate> keywordPredicates = new ArrayList<>();
          for (String keyword : keywords.get()) {
            keywordPredicates.add(cb.like(
                cb.lower(root.get("spclCnd")),
                "%" + keyword.toLowerCase() + "%"
            ));
          }
          // 각 조건의 키워드들은 OR로 연결
          conditionPredicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
        } else {
          // 매핑되지 않은 경우 직접 검색
          conditionPredicates.add(cb.like(
              cb.lower(root.get("spclCnd")),
              "%" + condition.toLowerCase() + "%"
          ));
        }
      }

      // 여러 조건들은 OR로 연결 (하나라도 매칭되면 포함)
      return conditionPredicates.isEmpty()
          ? cb.conjunction()
          : cb.or(conditionPredicates.toArray(new Predicate[0]));
    };
  }

  /**
   * Entity를 Response DTO로 변환
   */
  private ProductSummaryResponse toProductSummaryResponse(SavingsProducts product) {
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
          .productType("saving")
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
          .productType("saving")
          .maxIntrRate(BigDecimal.ZERO)
          .baseIntrRate(BigDecimal.ZERO)
          .build();
    }
  }

  // 추상 메서드 구현
  @Override
  protected String getProductCode(SavingsProducts product) {
    return product.getFinPrdtCd();
  }
}
