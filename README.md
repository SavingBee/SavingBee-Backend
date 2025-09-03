# SavingBee 백엔드 README

## 📝 소개

> **금융 상품 비교 서비스**
>
> 금융감독원 공공 API와 연동ㅎ여 예금/적금 데이터를 제공하는 API 서버

### 프로젝트 기능

#### 🔍 상품 검색 & 필터링

- 금리, 기간, 가입 조건, 우대사항 등 10여 가지 조건으로 검색
- 일일 데이터 업데이트로 신규 상품 검색 가능

#### ⚖️ 상품 비교 & 알림

- 2가지의 상품을 비교 및 이점 표시
- 선택한 조건과 일치하는 상품 발생 시 알림

#### 👤 개인 상품 관리 & 알림

- 실 가입 상품 등록 관리 및 만기 알림
- 만기시 재투자 상품 추천

<br />

### 프로토타입

[피그마 프로토 타입 확인하기](https://www.figma.com/design/jXjlTzJZHxhG3lS2sIDwpL/%ED%98%91%EC%97%852%EC%A1%B0?node-id=0-1&m=dev&t=pslM6mjntm4ySH2a-1)

<br />

## 🗂️ APIs

작성한 API는 아래에서 확인할 수 있습니다.

👉🏻 [API 바로보기](https://curse-jade-2cc.notion.site/ebd/23f64e5ed8bc81ef875fef3157275354)


<br />

## ⚙ 기술 스택

> skills 폴더에 있는 아이콘을 이용할 수 있습니다.

### Back-end

- Java 17 + Spring Boot 3.x
- JPA/Hibernate
- MySQL

### Infra

- GCP

### Tools

- Github
- Notion
  <br />

[//]: # (## 🛠️ 프로젝트 아키텍쳐)

[//]: # ()

[//]: # (![no-image]&#40;https://user-images.githubusercontent.com/80824750/208294567-738dd273-e137-4bbf-8307-aff64258fe03.png&#41;)

[//]: # ()

[//]: # ()

<br />

[//]: # ()

[//]: # (## 🤔 기술적 이슈와 해결 과정)

[//]: # ()

[//]: # (- Stream 써야할까?)

[//]: # (    - [Stream API에 대하여]&#40;https://velog.io/@yewo2nn16/Java-Stream-API&#41;)

[//]: # (- Gmail STMP 이용하여 이메일 전송하기)

[//]: # (    - [gmail 보내기]&#40;https://velog.io/@yewo2nn16/Email-이메일-전송하기with-첨부파일&#41;)

[//]: # (- AWS EC2에 배포하기)

[//]: # (    - [서버 배포하기-1]&#40;https://velog.io/@yewo2nn16/SpringBoot-서버-배포&#41;)

[//]: # (    - [서버 배포하기-2]&#40;https://velog.io/@yewo2nn16/SpringBoot-서버-배포-인텔리제이에서-jar-파일-빌드해서-배포하기&#41;)

[//]: # ()

[//]: # (<br />)

## 💁‍♂️ 프로젝트 팀원

- **김현승**
    - 회원가입
    - 개인 상품 관리 및 만기 알림
    - 관심 상품 보관함 기능
- **장지원**
    - 금용감독원 api 연결 데이터 처리 및 저장
    - 상품명 검색 기능
    - 상품 필터링 기능
- **조제승**
    - 상품 알림 기능
    - 상품 비교 기능