package com.project.savingbee.api;

import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.dto.UserResponseDTO;
import com.project.savingbee.domain.user.service.UserService;
import com.project.savingbee.domain.userproduct.dto.UserProductPageResponseDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductRequestDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductResponseDTO;
import com.project.savingbee.domain.userproduct.service.UserProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

/**
 * 마이페이지 관련 API 컨트롤러
 * - 회원정보 수정 (이메일, 닉네임)
 * - 비밀번호 변경
 * - 보유상품 조회/등록/수정/삭제
 * - 회원 탈퇴
 */
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;
    private final UserProductService userProductService;

    /**
     * 현재 사용자 정보 조회
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getUserProfile(Principal principal) {
        UserResponseDTO userInfo = userService.readUser();
        return ResponseEntity.ok(userInfo);
    }

    /**
     * 회원정보 수정 (이메일, 닉네임)
     */
    @PutMapping(value = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> updateProfile(
            @Validated(UserRequestDTO.updateGroup.class) @RequestBody UserRequestDTO dto,
            Principal principal
    ) throws AccessDeniedException {
        // 현재 로그인한 사용자와 요청 사용자가 일치하는지 확인
        dto.setUsername(principal.getName());
        
        userService.updateUser(dto);
        Map<String, String> responseBody = Collections.singletonMap("message", "회원정보가 성공적으로 수정되었습니다.");
        return ResponseEntity.ok(responseBody);
    }

    /**
     * 비밀번호 변경
     */
    @PutMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> changePassword(
            @Validated(UserRequestDTO.changePasswordGroup.class) @RequestBody UserRequestDTO dto,
            Principal principal
    ) throws AccessDeniedException {
        // 현재 로그인한 사용자와 요청 사용자가 일치하는지 확인
        dto.setUsername(principal.getName());
        
        userService.changePassword(dto);
        Map<String, String> responseBody = Collections.singletonMap("message", "비밀번호가 성공적으로 변경되었습니다.");
        return ResponseEntity.ok(responseBody);
    }

    /**
     * 보유상품 목록 조회 (페이징)
     */
    @GetMapping("/products")
    public ResponseEntity<UserProductPageResponseDTO> getOwnedProducts(
            Principal principal,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        UserProductPageResponseDTO products = userProductService.getUserProducts(principal.getName(), pageable);
        return ResponseEntity.ok(products);
    }

    /**
     * 보유상품 등록
     */
    @PostMapping(value = "/products", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addOwnedProduct(
            @Validated(UserProductRequestDTO.CreateGroup.class) @RequestBody UserProductRequestDTO dto,
            Principal principal
    ) {
        // 현재 로그인한 사용자 설정
        dto.setUsername(principal.getName());
        
        Long userProductId = userProductService.addUserProduct(dto);
        
        Map<String, Object> responseBody = Map.of(
            "message", "보유상품이 성공적으로 등록되었습니다.",
            "userProductId", userProductId
        );
        return ResponseEntity.ok(responseBody);
    }

    /**
     * 보유상품 상세 조회
     */
    @GetMapping("/products/{userProductId}")
    public ResponseEntity<UserProductResponseDTO> getOwnedProductDetail(
            @PathVariable Long userProductId,
            Principal principal
    ) {
        UserProductResponseDTO product = userProductService.getUserProduct(userProductId, principal.getName());
        return ResponseEntity.ok(product);
    }

    /**
     * 보유상품 수정
     */
    @PutMapping(value = "/products/{userProductId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> updateOwnedProduct(
            @PathVariable Long userProductId,
            @Validated(UserProductRequestDTO.UpdateGroup.class) @RequestBody UserProductRequestDTO dto,
            Principal principal
    ) {
        // 현재 로그인한 사용자 설정
        dto.setUsername(principal.getName());
        
        userProductService.updateUserProduct(userProductId, dto);
        Map<String, String> responseBody = Collections.singletonMap("message", "보유상품이 성공적으로 수정되었습니다.");
        return ResponseEntity.ok(responseBody);
    }

    /**
     * 보유상품 삭제
     */
    @DeleteMapping("/products/{userProductId}")
    public ResponseEntity<Map<String, String>> deleteOwnedProduct(
            @PathVariable Long userProductId,
            Principal principal
    ) {
        userProductService.deleteUserProduct(userProductId, principal.getName());
        Map<String, String> responseBody = Collections.singletonMap("message", "보유상품이 성공적으로 삭제되었습니다.");
        return ResponseEntity.ok(responseBody);
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping(value = "/account", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> deleteAccount(
            @Validated(UserRequestDTO.deleteGroup.class) @RequestBody UserRequestDTO dto,
            Principal principal
    ) throws AccessDeniedException {
        // 현재 로그인한 사용자와 요청 사용자가 일치하는지 확인
        dto.setUsername(principal.getName());
        
        userService.deleteUser(dto);
        Map<String, String> responseBody = Collections.singletonMap("message", "회원 탈퇴가 성공적으로 처리되었습니다.");
        return ResponseEntity.ok(responseBody);
    }
}
