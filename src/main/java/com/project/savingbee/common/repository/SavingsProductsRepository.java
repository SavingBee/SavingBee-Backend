package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.filtering.dto.ProductSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SavingsProductsRepository extends JpaRepository<SavingsProducts, String>,
    JpaSpecificationExecutor<SavingsProducts> {

  // 마지막 스캔 이후 수정된 상품만 조회(스캔 후보 수집용)
  List<SavingsProducts> findByUpdatedAtAfter(LocalDateTime since);

  // 금융상품코드 목록에 해당하는 상품 조회(금리 옵션 정보만 변경된 상품을 코드로 가져온 뒤 조회)
  List<SavingsProducts> findByFinPrdtCdIn(Collection<String> codes);

  // 상품명으로 검색 - 가입 가능 상품만
  List<SavingsProducts> findByFinPrdtNmContainingIgnoreCaseAndIsActiveTrue(String finPrdtNm);

  // 활성 상품을 최신 등록순으로 조회
  List<SavingsProducts> findByIsActiveTrueOrderByCreatedAtDesc();
  
  // 활성 상품 조회 (추천 시스템용)
  List<SavingsProducts> findByIsActiveTrue();
}