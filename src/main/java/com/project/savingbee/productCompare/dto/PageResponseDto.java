package com.project.savingbee.productCompare.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

@Getter
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)  // null 필드는 응답에서 생략
public class PageResponseDto<T> {

  private final List<T> content;
  private final int page;
  private final int size; // page size
  private final long totalElements; // 전체 건수

  private MatchedBankInfo matchedBankInfo;  // 금융회사명 필터 응답 확인용

  public static <T> PageResponseDto<T> fromList(
      List<T> all, Pageable pageable, MatchedBankInfo matchedBankInfo) {
    int page = Math.max(0, pageable.getPageNumber());
    int size = pageable.getPageSize();              // @PageableDefault로 기본값 20
    int from = Math.min(page * size, all.size());
    int to = Math.min(from + size, all.size());
    List<T> slice = all.subList(from, to);

    return new PageResponseDto<>(slice, page, size, all.size(), matchedBankInfo);
  }

  @Getter
  @AllArgsConstructor
  public static class MatchedBankInfo {

    private String bankKeyword;
    private List<MatchedBank> matchedBanks;
  }

  @Getter
  @AllArgsConstructor
  public static class MatchedBank {

    String finCoNo; // 금융회사 고유번호
    String korCoNm; // 금융회사 명
  }
}
