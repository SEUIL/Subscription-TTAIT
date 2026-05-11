# Frontend Handoff

## 현재 구조

- 원본 디자인 참고 파일: `frontDemo`
- 실제 작업용 프론트: `app`
- 화면 엔트리
  - `app/list.html`
  - `app/detail.html`
  - `app/auth.html`
  - `app/mypage.html`
  - `app/recommend.html`

## 공통 파일

- 공통 스타일
  - `app/assets/css/tokens.css`
  - `app/assets/css/base.css`
  - `app/assets/css/components.css`
  - `app/assets/css/pages.css`
- 공통 렌더링
  - `app/assets/js/components.js`
- 유틸
  - `app/assets/js/utils.js`

## 백엔드 연결 포인트

- 현재 목업 데이터 위치
  - `app/assets/js/mock-data.js`
- 현재 API 인터페이스 위치
  - `app/assets/js/api.js`

현재 `api.js`는 `USE_MOCK = true` 기준으로 동작합니다.
백엔드 연결 시 가장 먼저 이 파일을 실제 API 호출로 바꾸면 됩니다.

## 현재 프론트가 기대하는 API 함수

- `fetchListings(params)`
- `fetchListingDetail(id)`
- `fetchRecommendedListings()`
- `login(payload)`
- `signup(payload)`
- `fetchMyProfile()`
- `fetchSavedListings()`
- `fetchNotifications()`
- `fetchNotificationSettings()`

## 화면별 사용 API

- 목록: `fetchListings`
- 상세: `fetchListingDetail`
- 로그인/회원가입: `login`, `signup`
- 마이페이지: `fetchMyProfile`, `fetchSavedListings`, `fetchNotifications`, `fetchNotificationSettings`
- 추천공고: `fetchRecommendedListings`

## 현재 데이터 기준 필드

- 공고
  - `id`
  - `title`
  - `provider`
  - `providerDetail`
  - `region`
  - `housingType`
  - `supplyType`
  - `status`
  - `deposit`
  - `monthlyRent`
  - `salePrice`
  - `deadlineLabel`
  - `liked`
  - `tags`
- 사용자
  - `id`
  - `name`
  - `email`
  - `phone`
  - `age`
  - `maritalStatus`
  - `householdCount`
  - `preferredRegion`
  - `preferredDistrict`
  - `preferredHousingType`
  - `preferredSupplyType`
  - `maxDeposit`
  - `maxRent`
  - `categories`
- 알림
  - `id`
  - `title`
  - `message`
  - `read`
  - `createdAt`

## 연결 시 우선 작업

1. `app/assets/js/api.js`에서 `USE_MOCK = false` 전환
2. 각 함수의 `fetch(...)` URL을 실제 백엔드 엔드포인트로 교체
3. 응답 필드명이 다르면 `api.js`에서 프론트 형식으로 변환
4. 인증 토큰이 필요하면 `login()` 이후 저장 로직 추가
5. 저장 공고 / 알림 읽음 처리 / 설정 저장 API가 있으면 버튼 이벤트 연결

## 메모

- `frontDemo`는 수정 기준이 아니라 디자인 참고본입니다.
- 실제 유지보수 기준은 `app`입니다.
- 공통 CSS는 이미 합쳐진 상태라, 스타일 수정은 먼저 `components.css`를 보면 됩니다.
