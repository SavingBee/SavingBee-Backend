# 장바구니 API 문서

## 개요
장바구니 기능은 사용자가 관심 있는 예적금 상품을 임시로 저장하고 관리할 수 있는 기능입니다. 
프론트엔드에서 장바구니 버튼을 클릭하면 페이지 이동 없이 모달이나 사이드바 형태로 장바구니 기능을 제공할 수 있습니다.

## 주요 기능
- 상품 담기 (중복 체크 포함)
- 목록 조회 (필터링, 정렬, 페이징)
- 개별/선택/전체 삭제

## API 엔드포인트

### 1. 장바구니 목록 조회

**GET** `/api/cart`

사용자의 장바구니에 담긴 상품 목록을 조회합니다. 다양한 필터링과 정렬 옵션을 지원합니다.

#### Query Parameters
```
bankName: 은행명 필터 (선택사항)
filterProductType: 상품 타입 필터 (DEPOSIT/SAVINGS, 선택사항)
sortBy: 정렬 기준 (recent/interest, 기본값: recent)
page: 페이지 번호 (기본값: 0)
size: 페이지 크기 (기본값: 10)
```

#### Request Examples
```
# 기본 조회 (최근순, 10개)
GET /api/cart

# 신한은행 상품만 조회
GET /api/cart?bankName=신한

# 예금 상품만 금리순으로 조회
GET /api/cart?filterProductType=DEPOSIT&sortBy=interest

# 신한은행 적금 상품을 금리순으로 20개씩 조회
GET /api/cart?bankName=신한&filterProductType=SAVINGS&sortBy=interest&size=20
```

#### Response
**성공 (200 OK)**
```json
{
  "content": [
    {
      "cartId": 1,
      "productCode": "10220000001",
      "productType": "DEPOSIT",
      "bankName": "신한은행",
      "productName": "신한 정기예금",
      "maxInterestRate": 3.50,
      "termMonths": 12,
      "createdAt": "2024-01-15T10:30:00"
    },
    {
      "cartId": 2,
      "productCode": "10230000001",
      "productType": "SAVINGS",
      "bankName": "국민은행",
      "productName": "KB Star 적금",
      "maxInterestRate": 3.20,
      "termMonths": 24,
      "createdAt": "2024-01-14T15:20:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 25,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

#### 비즈니스 로직
1. 사용자 인증 확인
2. 필터링 조건에 따른 쿼리 선택
3. 정렬 기준 적용 (최근순/금리순)
4. 페이징 처리
5. DTO 변환 후 반환

---

### 2. 상품 담기

**POST** `/api/cart`

예적금 상품을 장바구니에 추가합니다.

#### Request Body
```json
{
  "productCode": "10220000001",
  "productType": "DEPOSIT"
}
```

#### Validation
- `productCode`: 필수, 상품 코드
- `productType`: 필수, DEPOSIT 또는 SAVINGS

#### Response
**성공 (201 Created)**
```json
{
  "cartId": 123,
  "productCode": "10220000001",
  "productType": "DEPOSIT",
  "bankName": "신한은행",
  "productName": "신한 정기예금",
  "maxInterestRate": 3.50,
  "termMonths": 12,
  "createdAt": "2024-01-15T10:30:00"
}
```

**실패 (400 Bad Request)**
```json
{
  "error": "이미 장바구니에 담긴 상품입니다."
}
```

```json
{
  "error": "예금 상품을 찾을 수 없습니다."
}
```

#### 비즈니스 로직
1. 사용자 인증 확인
2. 중복 상품 체크 (같은 사용자, 상품코드, 상품타입)
3. 상품 정보 자동 조회 (은행명, 상품명, 최고금리, 기간)
4. 장바구니 엔티티 생성 및 저장
5. 응답 DTO 반환

---

### 3. 개별 상품 삭제

**DELETE** `/api/cart/{cartId}`

장바구니에서 특정 상품을 삭제합니다.

#### Path Parameters
- `cartId`: 삭제할 장바구니 항목 ID

#### Response
**성공 (200 OK)**
```json
{
  "message": "장바구니에서 상품이 삭제되었습니다."
}
```

**실패 (400 Bad Request)**
```json
{
  "error": "장바구니 항목을 찾을 수 없습니다."
}
```

```json
{
  "error": "권한이 없습니다."
}
```

#### 비즈니스 로직
1. 사용자 인증 확인
2. 장바구니 항목 존재 확인
3. 소유권 확인 (본인의 장바구니 항목인지)
4. 삭제 실행

---

### 4. 선택 상품 일괄 삭제

**DELETE** `/api/cart/batch`

장바구니에서 선택한 여러 상품을 한번에 삭제합니다.

#### Request Body
```json
{
  "cartIds": [1, 2, 3, 4, 5]
}
```

#### Validation
- `cartIds`: 필수, 삭제할 장바구니 항목 ID 목록

#### Response
**성공 (200 OK)**
```json
{
  "message": "선택한 상품들이 삭제되었습니다.",
  "deletedCount": 5
}
```

#### 비즈니스 로직
1. 사용자 인증 확인
2. 해당 사용자의 장바구니 항목만 삭제 (소유권 확인 포함)
3. 삭제된 개수 반환

---

### 5. 장바구니 전체 비우기

**DELETE** `/api/cart/clear`

사용자의 장바구니를 완전히 비웁니다.

#### Response
**성공 (200 OK)**
```json
{
  "message": "장바구니가 비워졌습니다.",
  "deletedCount": 12
}
```

#### 비즈니스 로직
1. 사용자 인증 확인
2. 해당 사용자의 모든 장바구니 항목 삭제
3. 삭제된 개수 반환

## 데이터베이스 스키마

### Cart 테이블
```sql
CREATE TABLE cart (
    cart_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_code VARCHAR(255) NOT NULL,
    product_type ENUM('DEPOSIT', 'SAVINGS') NOT NULL,
    bank_name VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    max_interest_rate DECIMAL(5,2) NOT NULL,
    term_months INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_user_product (user_id, product_code, product_type),
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_user_interest (user_id, max_interest_rate)
);
```

## 프론트엔드 구현 가이드

### 장바구니 모달/사이드바 구조
```html
<div id="cart-modal" class="cart-modal">
  <!-- 헤더 -->
  <div class="cart-header">
    <h3>장바구니</h3>
    <button class="close-btn">&times;</button>
  </div>
  
  <!-- 필터/정렬 옵션 -->
  <div class="cart-filters">
    <select id="bank-filter">
      <option value="">전체 은행</option>
      <option value="신한">신한은행</option>
      <option value="국민">국민은행</option>
    </select>
    
    <select id="type-filter">
      <option value="">전체 상품</option>
      <option value="DEPOSIT">예금</option>
      <option value="SAVINGS">적금</option>
    </select>
    
    <select id="sort-option">
      <option value="recent">최근 담은 순</option>
      <option value="interest">금리 높은 순</option>
    </select>
  </div>
  
  <!-- 일괄 선택/삭제 -->
  <div class="cart-actions">
    <label>
      <input type="checkbox" id="select-all"> 전체 선택
    </label>
    <button id="delete-selected">선택 삭제</button>
    <button id="clear-all">전체 비우기</button>
  </div>
  
  <!-- 상품 목록 -->
  <div class="cart-items" id="cart-items">
    <!-- 동적으로 생성 -->
  </div>
  
  <!-- 페이징 -->
  <div class="cart-pagination" id="cart-pagination">
    <!-- 동적으로 생성 -->
  </div>
</div>
```

### JavaScript 구현 예제
```javascript
class CartManager {
  constructor() {
    this.currentPage = 0;
    this.pageSize = 10;
    this.filters = {
      bankName: '',
      filterProductType: '',
      sortBy: 'recent'
    };
  }

  // 장바구니 목록 조회
  async loadCartItems() {
    const params = new URLSearchParams({
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.filters.sortBy,
      ...this.filters.bankName && { bankName: this.filters.bankName },
      ...this.filters.filterProductType && { filterProductType: this.filters.filterProductType }
    });

    try {
      const response = await fetch(`/api/cart?${params}`, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        this.renderCartItems(data.content);
        this.renderPagination(data);
      }
    } catch (error) {
      console.error('장바구니 조회 실패:', error);
    }
  }

  // 상품 담기
  async addToCart(productCode, productType) {
    try {
      const response = await fetch('/api/cart', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getToken()}`
        },
        body: JSON.stringify({ productCode, productType })
      });

      if (response.ok) {
        alert('장바구니에 추가되었습니다.');
        this.loadCartItems(); // 목록 새로고침
      } else {
        const error = await response.json();
        alert(error.error || '추가에 실패했습니다.');
      }
    } catch (error) {
      console.error('상품 담기 실패:', error);
    }
  }

  // 개별 삭제
  async removeItem(cartId) {
    if (!confirm('이 상품을 장바구니에서 삭제하시겠습니까?')) return;

    try {
      const response = await fetch(`/api/cart/${cartId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });

      if (response.ok) {
        this.loadCartItems(); // 목록 새로고침
      }
    } catch (error) {
      console.error('삭제 실패:', error);
    }
  }

  // 선택 삭제
  async removeSelectedItems() {
    const selectedIds = this.getSelectedCartIds();
    if (selectedIds.length === 0) {
      alert('삭제할 상품을 선택해주세요.');
      return;
    }

    if (!confirm(`선택한 ${selectedIds.length}개 상품을 삭제하시겠습니까?`)) return;

    try {
      const response = await fetch('/api/cart/batch', {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getToken()}`
        },
        body: JSON.stringify({ cartIds: selectedIds })
      });

      if (response.ok) {
        const result = await response.json();
        alert(`${result.deletedCount}개 상품이 삭제되었습니다.`);
        this.loadCartItems(); // 목록 새로고침
      }
    } catch (error) {
      console.error('선택 삭제 실패:', error);
    }
  }

  // 전체 비우기
  async clearCart() {
    if (!confirm('장바구니를 완전히 비우시겠습니까?')) return;

    try {
      const response = await fetch('/api/cart/clear', {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });

      if (response.ok) {
        const result = await response.json();
        alert(`${result.deletedCount}개 상품이 삭제되었습니다.`);
        this.loadCartItems(); // 목록 새로고침
      }
    } catch (error) {
      console.error('전체 비우기 실패:', error);
    }
  }

  // 필터 적용
  applyFilters() {
    this.currentPage = 0; // 첫 페이지로 리셋
    this.loadCartItems();
  }

  // 상품 목록 렌더링
  renderCartItems(items) {
    const container = document.getElementById('cart-items');
    if (items.length === 0) {
      container.innerHTML = '<p class="empty-message">장바구니가 비어있습니다.</p>';
      return;
    }

    container.innerHTML = items.map(item => `
      <div class="cart-item" data-cart-id="${item.cartId}">
        <label class="item-checkbox">
          <input type="checkbox" name="cart-item" value="${item.cartId}">
        </label>
        <div class="item-info">
          <h4>${item.productName}</h4>
          <p class="bank-name">${item.bankName}</p>
          <p class="interest-rate">최고 ${item.maxInterestRate}%</p>
          <p class="term">${item.termMonths}개월</p>
          <p class="added-date">${this.formatDate(item.createdAt)}</p>
        </div>
        <button class="delete-btn" onclick="cartManager.removeItem(${item.cartId})">
          삭제
        </button>
      </div>
    `).join('');
  }

  // 페이징 렌더링
  renderPagination(data) {
    const container = document.getElementById('cart-pagination');
    if (data.totalPages <= 1) {
      container.innerHTML = '';
      return;
    }

    let pagination = '';
    
    // 이전 페이지
    if (!data.first) {
      pagination += `<button onclick="cartManager.goToPage(${data.page - 1})">이전</button>`;
    }
    
    // 페이지 번호들
    const startPage = Math.max(0, data.page - 2);
    const endPage = Math.min(data.totalPages - 1, data.page + 2);
    
    for (let i = startPage; i <= endPage; i++) {
      const active = i === data.page ? 'active' : '';
      pagination += `<button class="${active}" onclick="cartManager.goToPage(${i})">${i + 1}</button>`;
    }
    
    // 다음 페이지
    if (!data.last) {
      pagination += `<button onclick="cartManager.goToPage(${data.page + 1})">다음</button>`;
    }
    
    container.innerHTML = pagination;
  }

  // 페이지 이동
  goToPage(page) {
    this.currentPage = page;
    this.loadCartItems();
  }

  // 선택된 장바구니 ID 목록 가져오기
  getSelectedCartIds() {
    return Array.from(document.querySelectorAll('input[name="cart-item"]:checked'))
                .map(checkbox => parseInt(checkbox.value));
  }

  // JWT 토큰 가져오기 (실제 구현에 맞게 수정)
  getToken() {
    return localStorage.getItem('accessToken');
  }

  // 날짜 포맷팅
  formatDate(dateString) {
    return new Date(dateString).toLocaleDateString('ko-KR');
  }
}

// 전역 인스턴스 생성
const cartManager = new CartManager();

// 이벤트 리스너 설정
document.addEventListener('DOMContentLoaded', () => {
  // 필터 변경 이벤트
  document.getElementById('bank-filter').addEventListener('change', (e) => {
    cartManager.filters.bankName = e.target.value;
    cartManager.applyFilters();
  });

  document.getElementById('type-filter').addEventListener('change', (e) => {
    cartManager.filters.filterProductType = e.target.value;
    cartManager.applyFilters();
  });

  document.getElementById('sort-option').addEventListener('change', (e) => {
    cartManager.filters.sortBy = e.target.value;
    cartManager.applyFilters();
  });

  // 전체 선택/해제
  document.getElementById('select-all').addEventListener('change', (e) => {
    const checkboxes = document.querySelectorAll('input[name="cart-item"]');
    checkboxes.forEach(checkbox => checkbox.checked = e.target.checked);
  });

  // 선택 삭제
  document.getElementById('delete-selected').addEventListener('click', () => {
    cartManager.removeSelectedItems();
  });

  // 전체 비우기
  document.getElementById('clear-all').addEventListener('click', () => {
    cartManager.clearCart();
  });
});
```

## 주의사항

1. **인증 필요**: 모든 API는 사용자 인증이 필요합니다.
2. **중복 방지**: 같은 상품은 한 번만 담을 수 있습니다.
3. **소유권 확인**: 삭제 시 본인의 장바구니 항목만 삭제 가능합니다.
4. **자동 정보 조회**: 상품 담기 시 상품 정보가 자동으로 조회되어 저장됩니다.
5. **페이징 성능**: 대용량 데이터를 고려한 페이징 처리가 구현되어 있습니다.

## 에러 처리

모든 API는 적절한 HTTP 상태코드와 함께 에러 메시지를 반환합니다:
- `200 OK`: 성공
- `201 Created`: 생성 성공
- `400 Bad Request`: 잘못된 요청 (중복 상품, 유효하지 않은 데이터 등)
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 리소스를 찾을 수 없음
- `500 Internal Server Error`: 서버 내부 오류

## 성능 최적화

1. **인덱스 활용**: user_id, created_at, max_interest_rate에 대한 복합 인덱스
2. **페이징**: 대용량 데이터 처리를 위한 효율적인 페이징
3. **중복 방지**: 유니크 제약조건으로 데이터베이스 레벨에서 중복 방지
4. **쿼리 최적화**: 필터링과 정렬을 데이터베이스에서 처리하여 성능 향상
