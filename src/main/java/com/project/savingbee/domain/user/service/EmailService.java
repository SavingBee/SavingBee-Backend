package com.project.savingbee.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendUsernameEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[SavingBee] 아이디 찾기 결과");
            message.setText("안녕하세요.\n\n요청하신 아이디는 다음과 같습니다.\n\n아이디: " + username + 
                          "\n\n감사합니다.\nSavingBee 팀");
            
            mailSender.send(message);
            log.info("아이디 찾기 이메일 발송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("아이디 찾기 이메일 발송 실패: {}", toEmail, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    public void sendTemporaryPasswordEmail(String toEmail, String username, String temporaryPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[SavingBee] 임시 비밀번호 발급");
            message.setText("안녕하세요.\n\n임시 비밀번호가 발급되었습니다.\n\n" +
                          "아이디: " + username + "\n" +
                          "임시 비밀번호: " + temporaryPassword + "\n\n" +
                          "로그인 후 반드시 비밀번호를 변경해주세요.\n\n감사합니다.\nSavingBee 팀");
            
            mailSender.send(message);
            log.info("임시 비밀번호 이메일 발송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("임시 비밀번호 이메일 발송 실패: {}", toEmail, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    public void sendVerificationCodeEmail(String toEmail, String username, String verificationCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[SavingBee] 비밀번호 찾기 인증 코드");
            message.setText("안녕하세요.\n\n비밀번호 찾기 인증 코드입니다.\n\n" +
                          "아이디: " + username + "\n" +
                          "인증 코드: " + verificationCode + "\n\n" +
                          "인증 코드는 5분간 유효합니다.\n\n감사합니다.\nSavingBee 팀");
            
            mailSender.send(message);
            log.info("인증 코드 이메일 발송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("인증 코드 이메일 발송 실패: {}", toEmail, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    public void sendSignupVerificationCodeEmail(String toEmail, String verificationCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[SavingBee] 회원가입 이메일 인증 코드");
            message.setText("안녕하세요.\n\n회원가입을 위한 이메일 인증 코드입니다.\n\n" +
                          "인증 코드: " + verificationCode + "\n\n" +
                          "인증 코드는 10분간 유효합니다.\n\n감사합니다.\nSavingBee 팀");
            
            mailSender.send(message);
            log.info("회원가입 인증 코드 이메일 발송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("회원가입 인증 코드 이메일 발송 실패: {}", toEmail, e);
            log.warn("개발 환경에서 이메일 발송 실패를 무시합니다.");
        }
    }

    public void sendSignupWelcomeEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[SavingBee] 회원가입을 축하합니다!");
            message.setText("안녕하세요, " + username + "님!\n\n" +
                          "SavingBee 회원가입이 성공적으로 완료되었습니다.\n\n" +
                          "다양한 예적금 상품을 확인하고 맞춤 알림을 설정해보세요.\n\n" +
                          "감사합니다.\nSavingBee 팀");
            
            mailSender.send(message);
            log.info("회원가입 환영 이메일 발송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("회원가입 환영 이메일 발송 실패: {}", toEmail, e);
            log.warn("개발 환경에서 이메일 발송 실패를 무시합니다.");
        }
    }
}
