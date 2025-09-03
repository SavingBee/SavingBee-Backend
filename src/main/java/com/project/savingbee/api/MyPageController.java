package com.project.savingbee.api;

import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.dto.UserResponseDTO;
import com.project.savingbee.domain.user.service.UserService;
import com.project.savingbee.domain.userproduct.dto.UserProductPageResponseDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductRequestDTO;
import com.project.savingbee.domain.userproduct.dto.UserProductResponseDTO;
import com.project.savingbee.domain.userproduct.service.UserProductService;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지 관련 API 컨트롤러 - 회원정보 수정 (이메일, 닉네임) - 비밀번호 변경 - 보유상품 조회/등록/수정/삭제 - 회원 탈퇴
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
  public ResponseEntity<UserResponseDTO> getUserProfile(
      @AuthenticationPrincipal UserDetails userDetails) {
    System.out.println("=== MyPageController.getUserProfile Debug ===");
    System.out.println("UserDetails is null: " + (userDetails == null));

    if (userDetails != null) {
      System.out.println("UserDetails username: " + userDetails.getUsername());
      System.out.println("UserDetails authorities: " + userDetails.getAuthorities());

      // JWT 토큰이 있고 유효한 경우
      UserResponseDTO userInfo = userService.getUserInfo(userDetails.getUsername());
      return ResponseEntity.ok(userInfo);
    } else {
      System.out.println("UserDetails is null - returning 401");
      // JWT 토큰이 없거나 유효하지 않은 경우 - 에러 반환
      return ResponseEntity.status(401).build();
    }
  }

  /**
   * 회원정보 수정 (이메일, 닉네임)
   */
  @PutMapping(value = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, String>> updateProfile(
      @RequestBody UserRequestDTO dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) throws AccessDeniedException {
    System.out.println("=== MyPageController.updateProfile Debug ===");
    System.out.println(
        "UserDetails: " + (userDetails != null ? userDetails.getUsername() : "null"));
    System.out.println("Request DTO: " + dto);

    if (userDetails == null) {
      return ResponseEntity.status(401).build();
    }

    // 현재 로그인한 사용자 정보 설정
    dto.setUsername(userDetails.getUsername());

    // 간단한 검증
    if (dto.getNickname() != null && dto.getNickname().trim().isEmpty()) {
      return ResponseEntity.badRequest()
          .body(Collections.singletonMap("error", "닉네임은 공백일 수 없습니다."));
    }

    if (dto.getEmail() != null && dto.getEmail().trim().isEmpty()) {
      return ResponseEntity.badRequest()
          .body(Collections.singletonMap("error", "이메일은 공백일 수 없습니다."));
    }

    userService.updateUser(dto);
    Map<String, String> responseBody = Collections.singletonMap("message", "회원정보가 성공적으로 수정되었습니다.");
    return ResponseEntity.ok(responseBody);
  }

  /**
   * 비밀번호 변경
   */
  @PutMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, String>> changePassword(
      @RequestBody UserRequestDTO dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) throws AccessDeniedException {
    System.out.println("=== MyPageController.changePassword Debug ===");
    System.out.println(
        "UserDetails: " + (userDetails != null ? userDetails.getUsername() : "null"));
    System.out.println("Request DTO: " + dto);

    if (userDetails == null) {
      return ResponseEntity.status(401).build();
    }

    // 현재 로그인한 사용자 정보 설정
    dto.setUsername(userDetails.getUsername());

    // 간단한 검증
    if (dto.getCurrentPassword() == null || dto.getCurrentPassword().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(Collections.singletonMap("error", "현재 비밀번호는 필수입니다."));
    }

    if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(Collections.singletonMap("error", "새 비밀번호는 필수입니다."));
    }

    if (dto.getPasswordConfirm() == null || dto.getPasswordConfirm().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(Collections.singletonMap("error", "비밀번호 확인은 필수입니다."));
    }

    if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
      return ResponseEntity.badRequest()
          .body(Collections.singletonMap("error", "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."));
    }

    if (dto.getPassword().length() < 4) {
      return ResponseEntity.badRequest()
          .body(Collections.singletonMap("error", "비밀번호는 4자 이상이어야 합니다."));
    }

    userService.changePassword(dto);
    Map<String, String> responseBody = Collections.singletonMap("message", "비밀번호가 성공적으로 변경되었습니다.");
    return ResponseEntity.ok(responseBody);
  }

  /**
   * 보유상품 목록 조회 (페이징)
   */
  @GetMapping("/products")
  public ResponseEntity<UserProductPageResponseDTO> getOwnedProducts(
      @AuthenticationPrincipal UserDetails userDetails,
      @PageableDefault(size = 10) Pageable pageable
  ) {
    if (userDetails == null) {
      return ResponseEntity.status(401).build();
    }

    UserProductPageResponseDTO products = userProductService.getUserProducts(
        userDetails.getUsername(), pageable);
    return ResponseEntity.ok(products);
  }

  /**
   * 보유상품 등록
   */
  @PostMapping(value = "/products", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> addOwnedProduct(
      @Validated(UserProductRequestDTO.CreateGroup.class) @RequestBody UserProductRequestDTO dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    System.out.println("=== MyPageController.addOwnedProduct Debug ===");
    System.out.println(
        "UserDetails: " + (userDetails != null ? userDetails.getUsername() : "null"));
    System.out.println("Request DTO: " + dto);

    if (userDetails == null) {
      return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
    }

    // 현재 로그인한 사용자 정보 설정
    dto.setUsername(userDetails.getUsername());

    // @Valid 검증으로 대체되어 수동 검증 제거

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
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    if (userDetails == null) {
      return ResponseEntity.status(401).build();
    }

    UserProductResponseDTO product = userProductService.getUserProduct(userProductId,
        userDetails.getUsername());
    return ResponseEntity.ok(product);
  }

  /**
   * 보유상품 수정
   */
  @PutMapping(value = "/products/{userProductId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, String>> updateOwnedProduct(
      @PathVariable Long userProductId,
      @RequestBody UserProductRequestDTO dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    if (userDetails == null) {
      return ResponseEntity.status(401).build();
    }

    // 현재 로그인한 사용자 정보 설정
    dto.setUsername(userDetails.getUsername());

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
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    if (userDetails == null) {
      return ResponseEntity.status(401).build();
    }

    userProductService.deleteUserProduct(userProductId, userDetails.getUsername());
    Map<String, String> responseBody = Collections.singletonMap("message", "보유상품이 성공적으로 삭제되었습니다.");
    return ResponseEntity.ok(responseBody);
  }

  /**
   * 회원 탈퇴
   */
  @DeleteMapping(value = "/account", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, String>> deleteAccount(
      @RequestBody UserRequestDTO dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) throws AccessDeniedException {
    System.out.println("=== MyPageController.deleteAccount Debug ===");
    System.out.println(
        "UserDetails: " + (userDetails != null ? userDetails.getUsername() : "null"));
    System.out.println("Request DTO: " + dto);

    if (userDetails == null) {
      return ResponseEntity.status(401).build();
    }

    // 현재 로그인한 사용자 정보 설정
    dto.setUsername(userDetails.getUsername());

    // 간단한 검증 - 비밀번호 확인 (회원 탈퇴시 보안을 위해)
    if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
      return ResponseEntity.badRequest()
          .body(Collections.singletonMap("error", "회원 탈퇴를 위해 비밀번호 확인이 필요합니다."));
    }

    userService.deleteUser(dto);
    Map<String, String> responseBody = Collections.singletonMap("message", "회원 탈퇴가 성공적으로 처리되었습니다.");
    return ResponseEntity.ok(responseBody);
  }
}
