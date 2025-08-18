package com.project.savingbee.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestDTO {

    public interface existGroup {} // 회원 가입시 username 존재 확인
    public interface addGroup {} // 회원 가입시
    public interface passwordGroup {} // 비밀번호 변경시
    public interface updateGroup {} // 회원 수정시
    public interface deleteGroup {} // 회원 삭제시
    public interface findUsernameGroup {} // 아이디 찾기시
    public interface resetPasswordGroup {} // 비밀번호 재설정시 (임시 비밀번호)
    public interface findPasswordGroup {} // 비밀번호 찾기시 (인증 코드 발송)
    public interface verifyCodeGroup {} // 인증 코드 확인시
    public interface newPasswordGroup {} // 새 비밀번호 설정시

    @NotBlank(groups = {existGroup.class, addGroup.class, updateGroup.class, deleteGroup.class, 
                       resetPasswordGroup.class, findPasswordGroup.class, verifyCodeGroup.class, newPasswordGroup.class}) 
    @Size(min = 4)
    private String username;
    
    @NotBlank(groups = {addGroup.class, passwordGroup.class, newPasswordGroup.class}) @Size(min = 4)
    private String password;
    
    @NotBlank(groups = {addGroup.class, newPasswordGroup.class}) @Size(min = 4)
    private String passwordConfirm; // 비밀번호 확인
    
    @NotBlank(groups = {addGroup.class, updateGroup.class})
    private String nickname;
    
    @Email(groups = {addGroup.class, updateGroup.class, findUsernameGroup.class, 
                    resetPasswordGroup.class, findPasswordGroup.class})
    @NotBlank(groups = {findUsernameGroup.class, resetPasswordGroup.class, findPasswordGroup.class})
    private String email;
    
    @NotBlank(groups = {verifyCodeGroup.class}) @Size(min = 6, max = 6)
    private String verificationCode; // 인증 코드
}