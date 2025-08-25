# 마이페이지 API 명세서 (업데이트)

## API 기본 정보
- **Base URL**: `/api/mypage`
- **인증 방식**: JWT 토큰 (Bearer Authentication)
- **Content-Type**: `application/json`

## 1. 사용자 정보 관리

### 1.1 현재 사용자 정보 조회
```http
GET /api/mypage/profile
Authorization: Bearer {token}
```

**응답 예시:**
```json
{
  "username": "user123",
  "isSocial": false,
  "nickname": "사용자닉네임",
  "email": "user@example.com"
}
```

### 1.2 회원정보 수정 (이메일, 닉네임)
```http
PUT /api/mypage/profile
Authorization: Bearer {token}
Content-Type: application/json

{
  "email": "newemail@example.com",     // 선택적 (이메일만 수정 가능)
  "nickname": "새로운닉네임"            // 선택적 (닉네임만 수정 가능)
}
```

**주의사항:**
- `email`과 `nickname` 중 최소 하나는 반드시 포함되어야 함
- 둘 다 제공하면 동시 수정 가능
- 하나만 제공하면 해당 필드만 수정
- `username`은 JWT 토큰에서 자동 추출 (Request Body에 불필요)

**응답 예시:**
```json
{
  "message": "회원정보가 성공적으로 수정되었습니다."
}
```

### 1.3 비밀번호 변경
```http
PUT /api/mypage/password
Authorization: Bearer {token}
Content-Type: application/json

{
  "currentPassword": "현재비밀번호",
  "password": "새로운비밀번호",
  "passwordConfirm": "새로운비밀번호확인"
}
```

**응답 예시:**
```json
{
  "message": "비밀번호가 성공적으로 변경되었습니다."
}
```

### 1.4 회원 탈퇴 ⭐ **NEW**
```http
DELETE /api/mypage/account
Authorization: Bearer {token}
Content-Type: application/json

{
  // Request Body는 빈 객체 또는 생략 가능
  // username은 JWT 토큰에서 자동 추출
}
```

**응답 예시:**
```json
{
  "message": "회원 탈퇴가 성공적으로 처리되었습니다."
}
```

**탈퇴 처리 내용:**
- ✅ 사용자 계정 완전 삭제
- ✅ 모든 Refresh 토큰 삭제
- ✅ 연관 데이터 자동 삭제 (보유상품, 장바구니, 알림설정 등)
- ✅ 본인 인증 및 권한 검증

## 2. 보유상품 관리

### 2.1 보유상품 목록 조회 (페이징)
```http
GET /api/mypage/products?page=0&size=10
Authorization: Bearer {token}
```

**쿼리 파라미터:**
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 10)

**응답 예시:**
```json
{
  "content": [
    {
      "userProductId": 1,
      "userId": 123,
      "bankName": "KB국민은행",
      "productName": "KB Star 정기예금",
      "productType": "DEPOSIT",
      "interestRate": 3.50,
      "depositAmount": 10000000,
      "termMonths": 12,
      "joinDate": "2024-01-15",
      "maturityDate": "2025-01-15",
      "specialConditions": "신규고객 우대금리",
      "isActive": true,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00",
      "daysToMaturity": 365,
      "expectedInterest": 350000,
      "maturityStatus": "ACTIVE"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### 2.2 보유상품 등록
```http
POST /api/mypage/products
Authorization: Bearer {token}
Content-Type: application/json

{
  "bankName": "KB국민은행",
  "productName": "KB Star 정기예금",
  "productType": "DEPOSIT",          // DEPOSIT(예금) 또는 SAVINGS(적금)
  "interestRate": 3.50,
  "depositAmount": 10000000,
  "termMonths": 12,
  "joinDate": "2024-01-15",
  "maturityDate": "2025-01-15",
  "specialConditions": "신규고객 우대금리"  // 선택적
}
```

**응답 예시:**
```json
{
  "message": "보유상품이 성공적으로 등록되었습니다.",
  "userProductId": 1
}
```

### 2.3 보유상품 상세 조회
```http
GET /api/mypage/products/{userProductId}
Authorization: Bearer {token}
```

**응답 예시:**
```json
{
  "userProductId": 1,
  "userId": 123,
  "bankName": "KB국민은행",
  "productName": "KB Star 정기예금",
  "productType": "DEPOSIT",
  "interestRate": 3.50,
  "depositAmount": 10000000,
  "termMonths": 12,
  "joinDate": "2024-01-15",
  "maturityDate": "2025-01-15",
  "specialConditions": "신규고객 우대금리",
  "isActive": true,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "daysToMaturity": 365,
  "expectedInterest": 350000,
  "maturityStatus": "ACTIVE"
}
```

### 2.4 보유상품 수정
```http
PUT /api/mypage/products/{userProductId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "bankName": "KB국민은행",
  "productName": "KB Star 정기예금 (수정)",
  "productType": "DEPOSIT",
  "interestRate": 3.80,
  "depositAmount": 15000000,
  "termMonths": 12,
  "joinDate": "2024-01-15",
  "maturityDate": "2025-01-15",
  "specialConditions": "수정된 우대조건"
}
```

**응답 예시:**
```json
{
  "message": "보유상품이 성공적으로 수정되었습니다."
}
```

### 2.5 보유상품 삭제
```http
DELETE /api/mypage/products/{userProductId}
Authorization: Bearer {token}
```

**응답 예시:**
```json
{
  "message": "보유상품이 성공적으로 삭제되었습니다."
}
```

## 3. 에러 응답

### 3.1 인증 오류 (401)
```json
{
  "error": "Unauthorized",
  "message": "토큰이 유효하지 않습니다."
}
```

### 3.2 권한 오류 (403)
```json
{
  "error": "Forbidden",
  "message": "본인 계정만 수정 가능합니다."
}
```

### 3.3 유효성 검사 오류 (400)
```json
{
  "error": "Bad Request",
  "message": "현재 비밀번호가 일치하지 않습니다."
}
```

### 3.4 리소스 없음 (404)
```json
{
  "error": "Not Found",
  "message": "보유 상품을 찾을 수 없습니다."
}
```

## 4. 데이터 타입 및 제약사항

### 4.1 ProductType 열거형
- `DEPOSIT`: 예금
- `SAVINGS`: 적금

### 4.2 MaturityStatus 값
- `ACTIVE`: 만기까지 31일 이상
- `NEAR_MATURITY`: 만기까지 30일 이하
- `EXPIRED`: 만기 경과

### 4.3 필드 제약사항
- `bankName`: 최대 100자
- `productName`: 최대 200자
- `interestRate`: 0.01% ~ 99.99%
- `depositAmount`: 1원 이상
- `termMonths`: 1개월 ~ 120개월
- `joinDate`: 과거 또는 현재 날짜
- `maturityDate`: 미래 날짜
- `specialConditions`: 최대 500자 (선택적)

## 5. 보안 및 인증

### 5.1 JWT 토큰 기반 인증
- 모든 API는 유효한 JWT 토큰 필요
- `username`은 토큰에서 자동 추출
- 본인 계정만 접근/수정 가능

### 5.2 회원 탈퇴 보안
- 본인 또는 관리자만 탈퇴 가능
- 모든 연관 데이터 완전 삭제
- Refresh 토큰 즉시 무효화

## 6. API 엔드포인트 요약

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/mypage/profile` | 사용자 정보 조회 |
| PUT | `/api/mypage/profile` | 회원정보 수정 |
| PUT | `/api/mypage/password` | 비밀번호 변경 |
| **DELETE** | **`/api/mypage/account`** | **회원 탈퇴** ⭐ |
| GET | `/api/mypage/products` | 보유상품 목록 조회 |
| POST | `/api/mypage/products` | 보유상품 등록 |
| GET | `/api/mypage/products/{id}` | 보유상품 상세 조회 |
| PUT | `/api/mypage/products/{id}` | 보유상품 수정 |
| DELETE | `/api/mypage/products/{id}` | 보유상품 삭제 |
