package com.project.savingbee.filtering.controller;

import com.project.savingbee.filtering.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class SearchController {

  private final SearchService searchService;

  /**
   * 상품명으로 상품 검색
   *
   * @param q 검색할 상품명
   * @return 검색 결과 또는 인기 상품
   */
  @GetMapping("/search")
  public ResponseEntity<?> searchProducts(@RequestParam("q") String q) {
    return searchService.searchProduct(q);
  }
}
