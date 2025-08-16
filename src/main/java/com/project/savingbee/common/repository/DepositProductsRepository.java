package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.DepositProducts;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositProductsRepository extends JpaRepository<DepositProducts, String> {

  // 마지막 스캔 이후 수정된 상품만 조회(스캔 후보 수집용)
  List<DepositProducts> findByUpdatedAtAfter(LocalDateTime since);

  // 금융상품코드 목록에 해당하는 상품 조회(금리 옵션 정보만 변경된 상품을 코드로 가져온 뒤 조회)
  List<DepositProducts> findByFinPrdtCdIn(Collection<String> codes);
}
