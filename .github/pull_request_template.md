## 📌 계획
 
-
기존 상태

- common 디렉토리에 User 디렉토리 존재

변경 후 상태

- domain.user.entity 디렉토리를 생성후 이 곳으로 user 디렉토리 이동
- application-secret.properties 생성
- User클래스에 @EntityListeners(AuditingEntityListener.class)
  @Table(name = "users" ) 추가.

 

## 🔍 PR에서 핵심적으로 변경된 사항
- 자체 로그인: 회원가입시 유저 존재 여부
- PasswordEncoder등록
- 자체 로그인: 회원가입

## 🧪 테스트
- [ ] 단위 테스트
- [ ] API 테스트

## 📝 기타 변경 사항
- 
