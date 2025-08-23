package com.project.savingbee.filtering.service;

import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import com.project.savingbee.filtering.dto.SavingFilterRequest;
import com.project.savingbee.filtering.enums.PreConMapping;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingFilterService extends BaseFilterService<SavingsProducts, SavingFilterRequest>{

  private final SavingsProductsRepository savingsProductsRepository;

  /**
   * 적금 필터링
   * 금융권역 - 은행, 저축은행, 신협조합
   * 우대조건 - 비대면 가입, 재예치, 첫거래, 연령, 실적
   * 가입대상 - 제한 없음, 서민전용, 일부 제한
   * 저축기간 - 6개월, 12개월, 24개월, 36개월
   * 이자계산 방식 - 단리, 복리
   * 적립방식 - 정액적립식, 자유적립식, 전체
   * 월 저축금
   * 총 저축금
   * 기본 금리 - 최저값, 최고값
   * 최고 금리 - 최저값, 최고값
   */

  public Page<ProductSummaryResponse> savingFilter(SavingFilterRequest request){
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
  private Page<ProductSummaryResponse> filterWithRateSort (SavingFilterRequest request){
    // 필터링 조건만 적용하여 모든 데이터 조회
    // 중복 제거
    // 서비스 레벨에서 금리 기준 정렬
    // 페이징 적용
    // DTO 변환
    // Page 객체 생성
  }

  /**
   * 상품명 정렬 처리
   */
  private Page<ProductSummaryResponse> filterWithBasicSort(SavingFilterRequest request){
    // 페이징 조건 생성
    // 페이징 및 정렬 설정
    // 쿼리 실행
    // 중복 제거
    // DTO 변환 및 반환
  }

  /**
   * 필터링 조건만 생성
   */
  private Specification<SavingsProducts> buildFilterSpecification(SavingFilterRequest request){
    // 필터가 없으면 JOIN없이 기본 조건만 반환
    // 금융회사 번호 필터
    // 가입 제한 필터
    // 월 저축금 한도 필터
    // 총 저축금 한도 필터
    // 금리 관련 필터만 JOIN으로 처리
  }

  /**
   * 서비스 레벨에서 금리 기준 정렬
   */
  private List<SavingsProducts> sortByInterestRate(List<SavingsProducts> products, SavingFilterRequest request){
    // 최고 금리 정렬
    // 기본 금리 정렬
    // 디폴트: 최고 금리
    // 정렬 방향
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
  private Specification<SavingsProducts> filterFinCoNum(List<String> finCoNumber){
    return (root, query, cb) -> root.get("finCoNo").in();
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
  private boolean hasInterestRateFilters(SavingFilterRequest.Filters filters){
    return (filters.getSaveTrm() != null && !filters.getSaveTrm().isEmpty()) ||
        (filters.getIntrRateType() != null && !filters.getIntrRateType().isEmpty()) ||
        (filters.getIntrRate() != null && filters.getIntrRate().hasAnyValue()) ||
        (filters.getIntrRate2() != null && filters.getIntrRate2().hasAnyValue());
  }

  /**
   * 금리 관련 필터
   */
  private Specification<SavingsProducts> filterInterestRate(SavingFilterRequest.Filters filters) {
    return (root, query, cb) -> {
      // 금리 조건 적용
      var interestRatesJoin = root.join("interestRates", JoinType.LEFT);

      return conditionInterestRate(filters, interestRatesJoin, cb);
    };
  }

  /**
   * 금리 및 저축 기간 조건
   */
  private Predicate conditionInterestRate(SavingFilterRequest.Filters filters,
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

  // 월 저축금 확인
  private Specification<SavingsProducts> monthlyMaxLimit{
    // 사용자가 입력한 월 저축금보다 상품의 월 저축금(MaxLimit이 커야함)
  }

  // 우대조건 필터
  private Specification<SavingsProducts> hasPreferentialConditions(List<String> joinWayConditions){
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
   * 기타유의사항(etcNote)에서 총 저축금에 관한 내용 찾기
   */
  private Specification<SavingsProducts> totalMaxLimit{
    // 사용자가 입력한 총 저축금보다 상품의 총 저축금이 커야함.
  }

  /**
   * 적립 방식에 대한 필터
   */

  /**
   * Entity를 Response DTO로 변환
   */
  private ProductSummaryResponse toProductSummaryResponse(SavingsProducts product){
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
  protected String getProductCode(SavingsProducts product) {
    return product.getFinPrdtCd();
  }
}
