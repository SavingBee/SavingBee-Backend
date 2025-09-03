package com.project.savingbee.productCompare.controller;

import com.project.savingbee.productCompare.dto.CompareExecuteRequestDto;
import com.project.savingbee.productCompare.dto.CompareRequestDto;
import com.project.savingbee.productCompare.dto.CompareResponseDto;
import com.project.savingbee.productCompare.dto.PageResponseDto;
import com.project.savingbee.productCompare.dto.ProductInfoDto;
import com.project.savingbee.productCompare.service.ProductCompareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
  public PageResponseDto<ProductInfoDto> findFilteredProduct(
      @Valid CompareRequestDto compareRequestDto,
      @PageableDefault(size = 20) Pageable pageable) {

    return productCompareService.findFilteredProducts(compareRequestDto, pageable);
  }

  // 반환된 상품 목록에서 사용자가 입력한 키워드(금융회사명)로 필터링
  @GetMapping("/filter")
  public PageResponseDto<ProductInfoDto> findBankFilteredProduct(
      @Valid CompareRequestDto compareRequestDto,
      @PageableDefault(size = 20) Pageable pageable) {

    return productCompareService.findFilteredProducts(compareRequestDto, pageable);
  }

  // 선택한 두 상품 정보 비교
  @PostMapping
  public CompareResponseDto compareProduct(
      @Valid @RequestBody CompareExecuteRequestDto compareExecuteRequestDto) {
    return productCompareService.compareProducts(compareExecuteRequestDto);
  }
}
