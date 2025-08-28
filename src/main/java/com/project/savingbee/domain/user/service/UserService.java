package com.project.savingbee.domain.user.service;

import com.project.savingbee.domain.jwt.service.JwtService;
import com.project.savingbee.domain.user.dto.CustomOAuth2User;
import com.project.savingbee.domain.user.dto.UserRequestDTO;
import com.project.savingbee.domain.user.dto.UserResponseDTO;
import com.project.savingbee.domain.user.entity.PasswordResetToken;
import com.project.savingbee.domain.user.entity.SocialProviderType;
import com.project.savingbee.domain.user.entity.UserEntity;
import com.project.savingbee.domain.user.entity.UserRoleType;
import com.project.savingbee.domain.user.entity.SignupVerificationToken;
import com.project.savingbee.domain.user.repository.PasswordResetTokenRepository;
import com.project.savingbee.domain.user.repository.UserRepository;
import com.project.savingbee.domain.user.repository.SignupVerificationTokenRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;


@Service
public class UserService extends DefaultOAuth2UserService implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SignupVerificationTokenRepository signupVerificationTokenRepository; // 추가

    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository, 
                      JwtService jwtService, EmailService emailService,
                      PasswordResetTokenRepository passwordResetTokenRepository,
                      SignupVerificationTokenRepository signupVerificationTokenRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.signupVerificationTokenRepository = signupVerificationTokenRepository; // 추가
    }

    // 자체 로그인 회원 가입 (존재 여부)
    @Transactional(readOnly = true)
    public Boolean existUser(UserRequestDTO dto) {
        return userRepository.existsByUsername(dto.getUsername());
    }
    // 기존 방식: 즉시 회원가입 (이메일 인증 없이)
    @Transactional
    public Long addUser(UserRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("이미 유저가 존재합니다.");
        }

        // 비밀번호 확인 검증
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        UserEntity entity = UserEntity.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .isLock(false)
                .isSocial(false)
                .roleType(UserRoleType.USER)
                .nickname(dto.getNickname())
                .email(dto.getEmail())
                .alarm(true)
                .build();

        Long userId = userRepository.save(entity).getUserId();

        // 환영 이메일 발송 (선택사항)
        try {
            emailService.sendSignupWelcomeEmail(dto.getEmail(), dto.getUsername());
        } catch (Exception e) {
            // 이메일 발송 실패해도 회원가입은 성공 처리
            // log.warn("환영 이메일 발송 실패: {}", e.getMessage()); // Original code had this line commented out
        }

        return userId;
    }

    // 새로운 방식: 이메일 인증 후 회원가입
    @Transactional
    public Long addUserWithEmailVerification(UserRequestDTO dto) {
        // 인증된 토큰 확인
        Optional<SignupVerificationToken> tokenOpt = signupVerificationTokenRepository
            .findByEmailAndIsVerifiedTrueAndIsUsedFalseAndExpiresAtAfter(
                dto.getEmail(), LocalDateTime.now());
        
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("이미 유저가 존재합니다.");
        }

        // 비밀번호 확인 검증
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        UserEntity entity = UserEntity.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .isLock(false)
                .isSocial(false)
                .roleType(UserRoleType.USER)
                .nickname(dto.getNickname())
                .email(dto.getEmail())
                .alarm(true)
                .build();

        Long userId = userRepository.save(entity).getUserId();

        // 토큰 사용 처리
        SignupVerificationToken token = tokenOpt.get();
        token.setIsUsed(true);
        signupVerificationTokenRepository.save(token);

        // 환영 이메일 발송
        emailService.sendSignupWelcomeEmail(dto.getEmail(), dto.getUsername());

        return userId;
    }



    // 자체 로그인

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserEntity entity = userRepository.findByUsernameAndIsLockAndIsSocial(username, false, false)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return User.builder()
                .username(entity.getUsername())
                .password(entity.getPassword())
                .roles(entity.getRoleType().name())
                .accountLocked(entity.getIsLock())
                .build();
    }

    // 자체 로그인 회원 정보 수정
    @Transactional
    public Long updateUser(UserRequestDTO dto) throws AccessDeniedException {

        // 본인만 수정 가능 검증
        String sessionUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!sessionUsername.equals(dto.getUsername())) {
            throw new AccessDeniedException("본인 계정만 수정 가능");
        }

        // 이메일과 닉네임 중 최소 하나는 제공되어야 함
        boolean hasEmail = dto.getEmail() != null && !dto.getEmail().trim().isEmpty();
        boolean hasNickname = dto.getNickname() != null && !dto.getNickname().trim().isEmpty();
        
        if (!hasEmail && !hasNickname) {
            throw new IllegalArgumentException("이메일 또는 닉네임 중 최소 하나는 입력해야 합니다.");
        }

        // 조회
        UserEntity entity = userRepository.findByUsernameAndIsLockAndIsSocial(dto.getUsername(), false, false)
                .orElseThrow(() -> new UsernameNotFoundException(dto.getUsername()));

        // 회원 정보 수정
        entity.updateUser(dto);

        return userRepository.save(entity).getUserId();
    }

    // 현재 비밀번호 확인 후 새 비밀번호로 변경
    @Transactional
    public void changePassword(UserRequestDTO dto) throws AccessDeniedException {
        // 본인만 변경 가능 검증
        String sessionUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!sessionUsername.equals(dto.getUsername())) {
            throw new AccessDeniedException("본인 계정만 수정 가능");
        }

        // 자체 로그인 사용자만 비밀번호 변경 가능
        UserEntity entity = userRepository.findByUsernameAndIsLockAndIsSocial(dto.getUsername(), false, false)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(dto.getCurrentPassword(), entity.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호와 확인 비밀번호 일치 검증
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        // 현재 비밀번호와 새 비밀번호가 같은지 확인
        if (passwordEncoder.matches(dto.getPassword(), entity.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        // 비밀번호 업데이트
        entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(entity);
    }
    // 자체/소셜 로그인 회원 탈퇴
    @Transactional
    public void deleteUser(UserRequestDTO dto) throws AccessDeniedException {

        // 본인 및 어드민만 삭제 가능 검증
        SecurityContext context = SecurityContextHolder.getContext();
        String sessionUsername = context.getAuthentication().getName();
        String sessionRole = context.getAuthentication().getAuthorities().iterator().next().getAuthority();

        boolean isOwner = sessionUsername.equals(dto.getUsername());
        boolean isAdmin = sessionRole.equals("ROLE_"+UserRoleType.ADMIN.name());

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("본인 혹은 관리자만 삭제할 수 있습니다.");
        }

        // 유저 제거
        userRepository.deleteByUsername(dto.getUsername());

        // Refresh 토큰 제거
        jwtService.removeRefreshUser(dto.getUsername());
    }

    // 소셜 로그인 (매 로그인시 : 신규 = 가입, 기존 = 업데이트)
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 부모 메소드 호출
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 데이터
        Map<String, Object> attributes;
        List<GrantedAuthority> authorities;

        String username;
        String role = UserRoleType.USER.name();
        String email;
        String nickname;

        // provider 제공자별 데이터 획득
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        if (registrationId.equals(SocialProviderType.NAVER.name())) {

            attributes = (Map<String, Object>) oAuth2User.getAttributes().get("response");
            username = registrationId + "_" + attributes.get("id");
            email = attributes.get("email").toString();
            nickname = attributes.get("nickname").toString();

        } else if (registrationId.equals(SocialProviderType.GOOGLE.name())) {

            attributes = (Map<String, Object>) oAuth2User.getAttributes();
            username = registrationId + "_" + attributes.get("sub");
            email = attributes.get("email").toString();
            nickname = attributes.get("name").toString();

        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }

        // 데이터베이스 조회 -> 존재하면 업데이트, 없으면 신규 가입
        Optional<UserEntity> entity = userRepository.findByUsernameAndIsSocial(username, true);
        if (entity.isPresent()) {
            // role 조회
            role = entity.get().getRoleType().name();

            // 기존 유저 업데이트
            UserRequestDTO dto = new UserRequestDTO();
            dto.setNickname(nickname);
            dto.setEmail(email);
            entity.get().updateUser(dto);

            userRepository.save(entity.get());
        } else {
            // 신규 유저 추가
            UserEntity newUserEntity = UserEntity.builder()
                    .username(username)
                    .password("")
                    .isLock(false)
                    .isSocial(true)
                    .socialProviderType(SocialProviderType.valueOf(registrationId))
                    .roleType(UserRoleType.USER)
                    .nickname(nickname)
                    .email(email)
                    .alarm(true)
                    .build();

            userRepository.save(newUserEntity);
        }

        authorities = List.of(new SimpleGrantedAuthority(role));

        return new CustomOAuth2User(attributes, authorities, username);
    }

    // 자체/소셜 유저 정보 조회
    @Transactional(readOnly = true)
    public UserResponseDTO readUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        UserEntity entity = userRepository.findByUsernameAndIsLock(username, false) //false는 안잠긴 계정
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + username));

        return new UserResponseDTO(username, entity.getIsSocial(), entity.getNickname(), entity.getEmail());
    }

    // 특정 사용자 정보 조회 (username으로)
    @Transactional(readOnly = true)
    public UserResponseDTO getUserInfo(String username) {
        UserEntity entity = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + username));

        return new UserResponseDTO(username, entity.getIsSocial(), entity.getNickname(), entity.getEmail());
    }

    // 아이디 찾기 (이메일로)
    @Transactional
    public void findUsername(UserRequestDTO dto) {
        Optional<UserEntity> userEntity = userRepository.findByEmailAndIsSocial(dto.getEmail(), false);
        
        if (userEntity.isEmpty()) {
            throw new IllegalArgumentException("해당 이메일로 가입된 계정이 없습니다.");
        }
        
        // 이메일로 아이디 발송
        emailService.sendUsernameEmail(dto.getEmail(), userEntity.get().getUsername());
    }

    // 비밀번호 재설정 (임시 비밀번호 발급)
    @Transactional
    public void resetPassword(UserRequestDTO dto) {
        Optional<UserEntity> userEntity = userRepository.findByUsernameAndEmailAndIsSocial(
            dto.getUsername(), dto.getEmail(), false);
        
        if (userEntity.isEmpty()) {
            throw new IllegalArgumentException("입력하신 아이디와 이메일이 일치하는 계정이 없습니다.");
        }
        
        // 임시 비밀번호 생성 (8자리 랜덤)
        String temporaryPassword = generateTemporaryPassword();
        
        // 임시 비밀번호로 업데이트
        UserEntity user = userEntity.get();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);
        
        // 이메일로 임시 비밀번호 발송
        emailService.sendTemporaryPasswordEmail(dto.getEmail(), dto.getUsername(), temporaryPassword);
    }

    // 비밀번호 찾기 (인증 코드 발송)
    @Transactional
    public void findPassword(UserRequestDTO dto) {
        Optional<UserEntity> userEntity = userRepository.findByUsernameAndEmailAndIsSocial(
            dto.getUsername(), dto.getEmail(), false);
        
        if (userEntity.isEmpty()) {
            throw new IllegalArgumentException("입력하신 아이디와 이메일이 일치하는 계정이 없습니다.");
        }
        
        // 기존 토큰 삭제
        passwordResetTokenRepository.deleteByUsernameAndEmail(dto.getUsername(), dto.getEmail());
        
        // 6자리 인증 코드 생성
        String verificationCode = generateVerificationCode();
        
        // 토큰 생성 (5분 후 만료)
        PasswordResetToken token = PasswordResetToken.builder()
            .username(dto.getUsername())
            .email(dto.getEmail())
            .verificationCode(verificationCode)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .build();
        
        passwordResetTokenRepository.save(token);
        
        // 이메일로 인증 코드 발송
        emailService.sendVerificationCodeEmail(dto.getEmail(), dto.getUsername(), verificationCode);
    }

    // 인증 코드 확인
    @Transactional(readOnly = true)
    public boolean verifyCode(UserRequestDTO dto) {
        Optional<PasswordResetToken> token = passwordResetTokenRepository
            .findByUsernameAndVerificationCodeAndIsUsedFalseAndExpiresAtAfter(
                dto.getUsername(), dto.getVerificationCode(), LocalDateTime.now());
        
        return token.isPresent();
    }

    // 새 비밀번호 설정
    @Transactional
    public void setNewPassword(UserRequestDTO dto) {
        // 인증 코드 확인
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository
            .findByUsernameAndVerificationCodeAndIsUsedFalseAndExpiresAtAfter(
                dto.getUsername(), dto.getVerificationCode(), LocalDateTime.now());
        
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 인증 코드입니다.");
        }
        
        // 비밀번호 확인 검증
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        
        // 사용자 비밀번호 업데이트
        Optional<UserEntity> userEntity = userRepository.findByUsernameAndIsSocial(dto.getUsername(), false);
        if (userEntity.isEmpty()) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        
        UserEntity user = userEntity.get();
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
        
        // 토큰 사용 처리
        PasswordResetToken token = tokenOpt.get();
        token.setIsUsed(true);
        passwordResetTokenRepository.save(token);
    }

    // 임시 비밀번호 생성
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }

    // 6자리 인증 코드 생성
    private String generateVerificationCode() {
        java.util.Random random = new java.util.Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    // 회원가입 이메일 인증 코드 발송
    @Transactional
    public void sendSignupVerificationCode(UserRequestDTO dto) {
        // 이미 가입된 이메일인지 확인
        Optional<UserEntity> existingUser = userRepository.findByEmailAndIsSocial(dto.getEmail(), false);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 기존 토큰 삭제
        signupVerificationTokenRepository.deleteByEmail(dto.getEmail());

        // 6자리 인증 코드 생성
        String verificationCode = generateVerificationCode();

        // 토큰 생성 (10분 후 만료)
        SignupVerificationToken token = SignupVerificationToken.builder()
            .email(dto.getEmail())
            .verificationCode(verificationCode)
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .tempUsername(dto.getUsername())
            .tempPassword(passwordEncoder.encode(dto.getPassword()))
            .tempNickname(dto.getNickname())
            .build();

        signupVerificationTokenRepository.save(token);

        // 이메일로 인증 코드 발송
        emailService.sendSignupVerificationCodeEmail(dto.getEmail(), verificationCode);
    }

    // 회원가입 인증 코드 확인
    @Transactional
    public boolean verifySignupCode(UserRequestDTO dto) {
        Optional<SignupVerificationToken> tokenOpt = signupVerificationTokenRepository
            .findByEmailAndVerificationCodeAndIsUsedFalseAndExpiresAtAfter(
                dto.getEmail(), dto.getVerificationCode(), LocalDateTime.now());

        if (tokenOpt.isPresent()) {
            // 인증 완료 처리
            SignupVerificationToken token = tokenOpt.get();
            token.setIsVerified(true);
            signupVerificationTokenRepository.save(token);
            return true;
        }
        return false;
    }
}