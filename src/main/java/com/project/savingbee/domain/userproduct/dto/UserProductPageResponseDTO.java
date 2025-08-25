package com.project.savingbee.domain.userproduct.dto;

import lombok.*;

import java.util.List;

// 페이징 응답용 DTO
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProductPageResponseDTO {
    private List<UserProductResponseDTO> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
