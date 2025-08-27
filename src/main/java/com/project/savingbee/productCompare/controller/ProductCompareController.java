package com.project.savingbee.productCompare.controller;

import com.project.savingbee.productCompare.dto.CompareExecuteRequestDto;
import com.project.savingbee.productCompare.dto.CompareRequestDto;
import com.project.savingbee.productCompare.dto.CompareResponseDto;
import com.project.savingbee.productCompare.dto.ProductInfoDto;
import com.project.savingbee.productCompare.service.ProductCompareService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/compare")
public class ProductCompareController {
  private final ProductCompareService productCompareService;

  // 원하는 조건의 상품 목록 가져오기(필터링)
  @GetMapping
  public List<ProductInfoDto> findFilteredProduct(
      @Valid CompareRequestDto compareRequestDto) {

    return productCompareService.findFilteredProducts(compareRequestDto);
  }

  // 선택한 두 상품 정보 비교
  @PostMapping
  public CompareResponseDto compareProduct(
      @Valid @RequestBody CompareExecuteRequestDto compareExecuteRequestDto) {
    return productCompareService.compareProducts(compareExecuteRequestDto);
  }
}
