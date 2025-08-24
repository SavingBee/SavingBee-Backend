package com.project.savingbee.api;

import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.dto.UserResponseDTO;
import com.project.savingbee.domain.user.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.Map;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 자체 로그인 유저 존재 확인
    @PostMapping(value = "/user/exist", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> existUserApi(
            @Validated(UserRequestDTO.existGroup.class) @RequestBody UserRequestDTO dto
    ) {
        return ResponseEntity.ok(userService.existUser(dto));
    }
    // 기존 방식: 즉시 회원가입
    @PostMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> joinApi(
            @Validated(UserRequestDTO.addGroup.class) @RequestBody UserRequestDTO dto
    ) {
        Long id = userService.addUser(dto);
        Map<String, Long> responseBody = Collections.singletonMap("userEntityId", id);
        return ResponseEntity.status(201).body(responseBody);
    }

    // 새로운 방식: 이메일 인증 후 회원가입
    @PostMapping(value = "/user/verified", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> joinWithVerificationApi(
            @Validated(UserRequestDTO.addGroup.class) @RequestBody UserRequestDTO dto
    ) {
        Long id = userService.addUserWithEmailVerification(dto);
        Map<String, Long> responseBody = Collections.singletonMap("userEntityId", id);
        return ResponseEntity.status(201).body(responseBody);
    }

    // 아이디 찾기
    @PostMapping(value = "/user/find-username", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> findUsernameApi(
            @Validated(UserRequestDTO.findUsernameGroup.class) @RequestBody UserRequestDTO dto
    ) {
        userService.findUsername(dto);
        Map<String, String> responseBody = Collections.singletonMap("message", "아이디가 이메일로 발송되었습니다.");
        return ResponseEntity.ok(responseBody);
    }

    // 비밀번호 재설정 (임시 비밀번호 발급)
    @PostMapping(value = "/user/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> resetPasswordApi(
            @Validated(UserRequestDTO.resetPasswordGroup.class) @RequestBody UserRequestDTO dto
    ) {
        userService.resetPassword(dto);
        Map<String, String> responseBody = Collections.singletonMap("message", "임시 비밀번호가 이메일로 발송되었습니다.");
        return ResponseEntity.ok(responseBody);
    }

    // 유저 정보
    @GetMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponseDTO userMeApi() {
        return userService.readUser();
    }

    // 유저 수정 (자체 로그인 유저만)
    @PutMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> updateUserApi(
            @Validated(UserRequestDTO.updateGroup.class) @RequestBody UserRequestDTO dto
    ) throws AccessDeniedException {
        return ResponseEntity.status(200).body(userService.updateUser(dto));
    }

    // 비밀번호 변경 (현재 비밀번호 확인 후)
    @PutMapping(value = "/user/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> changePasswordApi(
            @Validated(UserRequestDTO.changePasswordGroup.class) @RequestBody UserRequestDTO dto
    ) throws AccessDeniedException {
        userService.changePassword(dto);
        Map<String, String> responseBody = Collections.singletonMap("message", "비밀번호가 성공적으로 변경되었습니다.");
        return ResponseEntity.ok(responseBody);
    }

    // 유저 제거 (자체/소셜)
    @DeleteMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> deleteUserApi(
            @Validated(UserRequestDTO.deleteGroup.class) @RequestBody UserRequestDTO dto
    ) throws AccessDeniedException {

        userService.deleteUser(dto);
        return ResponseEntity.status(200).body(true);
    }
    // 비밀번호 찾기 (인증 코드 발송)
    @PostMapping(value = "/user/find-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> findPasswordApi(
            @Validated(UserRequestDTO.findPasswordGroup.class) @RequestBody UserRequestDTO dto
    ) {
        userService.findPassword(dto);
        Map<String, String> responseBody = Collections.singletonMap("message", "인증 코드가 이메일로 발송되었습니다.");
        return ResponseEntity.ok(responseBody);
    }

    // 인증 코드 확인
    @PostMapping(value = "/user/verify-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Boolean>> verifyCodeApi(
            @Validated(UserRequestDTO.verifyCodeGroup.class) @RequestBody UserRequestDTO dto
    ) {
        boolean isValid = userService.verifyCode(dto);
        Map<String, Boolean> responseBody = Collections.singletonMap("valid", isValid);
        return ResponseEntity.ok(responseBody);
    }

    // 새 비밀번호 설정
    @PostMapping(value = "/user/new-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> setNewPasswordApi(
            @Validated(UserRequestDTO.newPasswordGroup.class) @RequestBody UserRequestDTO dto
    ) {
        userService.setNewPassword(dto);
        Map<String, String> responseBody = Collections.singletonMap("message", "비밀번호가 성공적으로 변경되었습니다.");
        return ResponseEntity.ok(responseBody);
    }

    // 회원가입 이메일 인증 코드 발송
    @PostMapping(value = "/user/signup/send-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> sendSignupVerificationCodeApi(
            @Validated(UserRequestDTO.signupEmailVerifyGroup.class) @RequestBody UserRequestDTO dto
    ) {
        userService.sendSignupVerificationCode(dto);
        Map<String, String> responseBody = Collections.singletonMap("message", "인증 코드가 이메일로 발송되었습니다.");
        return ResponseEntity.ok(responseBody);
    }

    // 회원가입 인증 코드 확인
    @PostMapping(value = "/user/signup/verify-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Boolean>> verifySignupCodeApi(
            @Validated(UserRequestDTO.signupVerifyCodeGroup.class) @RequestBody UserRequestDTO dto
    ) {
        boolean isValid = userService.verifySignupCode(dto);
        Map<String, Boolean> responseBody = Collections.singletonMap("valid", isValid);
        return ResponseEntity.ok(responseBody);
    }


}