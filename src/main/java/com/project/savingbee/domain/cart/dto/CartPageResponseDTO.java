package com.project.savingbee.domain.cart.dto;

import lombok.*;
import java.util.List;

/**
 * 장바구니 페이징 응답을 위한 DTO 페이징된 장바구니 목록 조회 결과 제공
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartPageResponseDTO {

  private List<CartResponseDTO> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
  private boolean first;
  private boolean last;
}

