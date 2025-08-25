package com.project.savingbee.filtering.controller;

import com.project.savingbee.filtering.dto.ProductDetailResponse;
import com.project.savingbee.filtering.service.DetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class DetailController {
  private final DetailService detailService;

  /**
   * 상품 상세 조회
   */
  @GetMapping("/{productId}")
  public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable String productId) {
    try {
      ProductDetailResponse response = detailService.getProductDetail(productId);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(null); // 400 - 잘못된 요청
    } catch (Exception e) {
      log.error("상품 상세 조회 중 예외 발생 - productId: {}, error: {}", productId, e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // 500 - 서버 오류
    }
  }
}
