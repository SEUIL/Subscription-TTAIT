# 공고 조회 API 가이드

이 문서는 현재 백엔드 기준으로 아래 3개 API만 빠르게 확인할 수 있도록 정리한 문서입니다.

- 맞춤공고 추천
- 공고 필터링 검색
- 공고 상세 조회

기본 서버 주소:

```text
http://localhost:8080
```

## 1. 맞춤공고 추천

사용자 프로필을 기준으로 추천 공고를 반환합니다.

### 요청

```http
GET /api/recommendations?page=0&size=20
```

헤더

```http
Authorization: Bearer {USER_TOKEN}
```

### 설명

- 로그인한 사용자 기준 API
- 사용자 프로필이 있어야 정상 동작
- 추천 점수는 카테고리, 지역, 예산, 자격조건 등을 바탕으로 계산

### 예시 요청

```http
GET http://localhost:8080/api/recommendations?page=0&size=20
Authorization: Bearer {USER_TOKEN}
```

### 예시 응답

```json
{
  "content": [
    {
      "announcementId": 3,
      "noticeName": "부산 청년 무주택 임대 테스트 A",
      "providerName": "LH",
      "supplyType": null,
      "houseType": null,
      "regionLevel1": "부산",
      "regionLevel2": "해운대구",
      "fullAddress": "부산 해운대구 테스트로 11",
      "complexName": "해운대청년하우스A",
      "depositAmount": 7000,
      "monthlyRentAmount": 20,
      "applicationStartDate": "2026-05-03",
      "applicationEndDate": "2026-05-20",
      "noticeStatus": "OPEN",
      "matchScore": 51,
      "matchReasons": [
        "선택한 신분 유형과 일치",
        "보증금 예산 범위 충족",
        "월세 예산 범위 충족"
      ]
    }
  ],
  "number": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

### 주요 응답 필드

- `announcementId`: 공고 ID
- `noticeName`: 공고명
- `providerName`: 공급기관명
- `regionLevel1`, `regionLevel2`: 지역 정보
- `depositAmount`, `monthlyRentAmount`: 보증금, 월세
- `matchScore`: 추천 점수
- `matchReasons`: 추천 이유 목록

## 2. 공고 필터링 검색

조건별로 공고 목록을 조회합니다.

### 요청

```http
GET /api/announcements
```

### 지원 쿼리 파라미터

- `regionLevel1`
- `regionLevel2`
- `supplyType`
- `houseType`
- `provider`
- `status`
- `keyword`
- `minDeposit`
- `maxDeposit`
- `minMonthlyRent`
- `maxMonthlyRent`
- `categories`
- `page`
- `size`

### 예시 요청 1

서울 지역 공고 검색

```http
GET http://localhost:8080/api/announcements?regionLevel1=서울&page=0&size=20
```

### 예시 요청 2

카테고리 기준 검색

```http
GET http://localhost:8080/api/announcements?categories=YOUTH&page=0&size=20
```

### 예시 요청 3

복합 조건 검색

```http
GET http://localhost:8080/api/announcements?regionLevel1=서울&categories=YOUTH&categories=NEWLYWED&maxDeposit=10000&page=0&size=20
```

### 필터 동작 방식

- 다른 필터들은 기본적으로 함께 만족해야 합니다
- `categories`를 여러 개 넣으면 그중 하나라도 맞으면 포함됩니다

예:

```http
GET /api/announcements?categories=YOUTH&categories=NEWLYWED
```

의미:

- `YOUTH` 또는 `NEWLYWED` 카테고리 공고 조회

### 예시 응답

```json
{
  "content": [
    {
      "announcementId": 1,
      "noticeName": "서울 청년 공공임대 테스트",
      "providerName": "LH",
      "supplyType": null,
      "houseType": null,
      "regionLevel1": "서울",
      "regionLevel2": "강남구",
      "fullAddress": "서울 강남구 테스트로 1",
      "complexName": "테스트하우스",
      "depositAmount": 8000,
      "monthlyRentAmount": 25,
      "applicationStartDate": "2026-05-03",
      "applicationEndDate": "2026-05-20",
      "noticeStatus": "OPEN"
    }
  ],
  "number": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 주요 응답 필드

- `announcementId`
- `noticeName`
- `providerName`
- `regionLevel1`, `regionLevel2`
- `depositAmount`, `monthlyRentAmount`
- `applicationStartDate`, `applicationEndDate`
- `noticeStatus`

## 3. 공고 상세 조회

특정 공고 1건의 상세 정보를 조회합니다.

### 요청

```http
GET /api/announcements/{announcementId}
```

### 예시 요청

```http
GET http://localhost:8080/api/announcements/3
```

### 예시 응답

```json
{
  "announcementId": 3,
  "noticeName": "부산 청년 무주택 임대 테스트 A",
  "providerName": "LH",
  "noticeStatus": "OPEN",
  "announcementDate": null,
  "applicationStartDate": "2026-05-03",
  "applicationEndDate": "2026-05-20",
  "winnerAnnouncementDate": null,
  "supplyType": null,
  "houseType": null,
  "complexName": "해운대청년하우스A",
  "fullAddress": "부산 해운대구 테스트로 11",
  "depositAmount": 7000,
  "monthlyRentAmount": 20,
  "householdCount": null,
  "supplyHouseholdCount": 10,
  "heatingType": null,
  "exclusiveAreaText": null,
  "exclusiveAreaValue": null,
  "moveInExpectedYm": null,
  "applicationDatetimeText": null,
  "guideText": null,
  "contactPhone": null,
  "sourceUrl": ""
}
```

### 주요 응답 필드

- `announcementId`
- `noticeName`
- `providerName`
- `noticeStatus`
- `applicationStartDate`
- `applicationEndDate`
- `fullAddress`
- `complexName`
- `depositAmount`
- `monthlyRentAmount`
- `supplyHouseholdCount`
- `sourceUrl`

### 참고

- 수동 등록 공고는 일부 상세 필드가 `null` 일 수 있습니다
- 없는 공고 ID 조회 시 `404` 가 반환됩니다

## 빠른 테스트 순서

1. `GET /api/announcements` 로 목록 검색
2. 응답에서 `announcementId` 확인
3. `GET /api/announcements/{announcementId}` 로 상세 조회
4. `GET /api/recommendations` 로 맞춤공고 확인
