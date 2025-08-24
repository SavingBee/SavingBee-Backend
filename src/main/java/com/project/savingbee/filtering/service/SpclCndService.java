package com.project.savingbee.filtering.service;

import com.project.savingbee.filtering.enums.SpclCndType;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 우대조건 텍스트를 파싱하여 조건 타입 반환 캐싱 적용으로 동일 텍스트 재활용 시도
 */
@Slf4j
@Service
public class SpclCndService {

  @Cacheable(value = "preferential-conditions", key = "#spclCnd")
  public Set<SpclCndType> parseConditions(String spclCnd) {
    if (!StringUtils.hasText(spclCnd)) {
      return Set.of(SpclCndType.NONE);
    }

    try {
      Set<SpclCndType> conditions = SpclCndType.parseConditions(spclCnd);

      // 디버깅용 로그
      if (log.isDebugEnabled()) {
        log.debug("우대조건 파싱 결과 - 원본: '{}', 결과: {}",
            spclCnd, conditions);
      }

      return conditions;

    } catch (Exception e) {
      log.warn("우대조건 파싱 실패 - 원본: '{}', 오류: {}", spclCnd, e.getMessage());
      return Set.of(SpclCndType.NONE);
    }
  }

//  /**
//   * 두 우대조건 세트가 매칭되는지 확인
//   *
//   * @param productConditions 상품의 우대조건들
//   * @param searchConditions  검색하려는 우대조건들
//   * @return 매칭 여부
//   */
//  public boolean matchesConditions(Set<SpclCndType> productConditions,
//      Set<SpclCndType> searchConditions) {
//
//    if (searchConditions == null || searchConditions.isEmpty()) {
//      return true; // 검색 조건이 없으면 모든 상품 매칭
//    }
//
//    // 검색하는 조건 중 하나라도 상품에 있으면 매칭 (OR 조건)
//    for (SpclCndType searchCondition : searchConditions) {
//      if (productConditions.contains(searchCondition)) {
//        return true;
//      }
//    }
//
//    return false;
//  }
}
