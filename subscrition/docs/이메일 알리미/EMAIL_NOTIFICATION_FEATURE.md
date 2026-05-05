# 이메일 알리미 기능 명세

## 기능 개요

사용자에게 두 종류의 이메일을 자동 발송한다.

| 종류 | 발송 시각 | 내용 |
|------|----------|------|
| 맞춤 공고 알리미 | 매일 09:00 | 사용자 프로필 기반 추천 공고 최대 10건 |
| 마감 리마인드 | 매일 09:05 | 즐겨찾기 공고 마감 D-7/3/1일 전 각 1회 |

---

## 패키지 구조

```
com.ttait.subscription.notification
├── favorite/
│   ├── controller/   FavoriteController
│   ├── domain/       UserFavoriteAnnouncement
│   ├── dto/          FavoriteCreateRequest, FavoriteResponse
│   ├── repository/   UserFavoriteAnnouncementRepository
│   └── service/      FavoriteService
├── email/
│   ├── domain/       EmailNotificationLog, EmailNotificationType (enum)
│   ├── repository/   EmailNotificationLogRepository
│   ├── sender/       MessageSender (interface), SmtpEmailSender
│   └── service/      RecommendationEmailService, DeadlineEmailService
└── scheduler/        EmailNotificationScheduler
```

---

## 즐겨찾기 API

| 메서드 | 경로 | 상태 코드 | 설명 |
|--------|------|----------|------|
| POST | `/api/me/favorites` | 201 | 즐겨찾기 추가 (멱등 — 이미 있으면 200) |
| DELETE | `/api/me/favorites/{announcementId}` | 204 | 즐겨찾기 해제 (없으면 404) |
| GET | `/api/me/favorites?page&size` | 200 | 즐겨찾기 목록 페이지 |
| GET | `/api/me/favorites/{announcementId}/exists` | 200 | 단건 존재 여부 boolean |

### 요청/응답 예시

**POST `/api/me/favorites`**
```json
{ "announcementId": 123 }
```

**GET `/api/me/favorites`**
```json
{
  "content": [
    {
      "announcementId": 123,
      "noticeName": "2025년 서울시 청년 임대주택 1차 모집",
      "providerName": "LH한국토지주택공사",
      "regionLevel1": "서울특별시",
      "regionLevel2": "강남구",
      "depositAmount": 1000,
      "monthlyRentAmount": 30,
      "applicationEndDate": "2025-07-15",
      "noticeStatus": "OPEN",
      "favoritedAt": "2025-06-01T10:00:00"
    }
  ],
  "page": { "totalElements": 1, "totalPages": 1 }
}
```

---

## 이메일 발송 흐름

### 맞춤 공고 알리미 (`RecommendationEmailService`)

```
사용자 청약 프로필 존재 여부 확인
    └─ 미완성 프로필 → skip (warn 로그)

RecommendationService.getRecommendations() → 후보 최대 20건
    └─ 후보 없음 → skip

EmailNotificationLog 조회 → success=true인 기발송 공고 ID 제외
    └─ 미발송 공고 없음 → skip

미발송 공고 최대 10건으로 제한 → Thymeleaf 렌더링
    └─ SMTP 발송
        ├─ 성공 → EmailNotificationLog(success=true) 저장
        └─ 실패 → EmailNotificationLog(success=false, errorMessage) 저장, 예외 swallow
```

### 마감 리마인드 (`DeadlineEmailService`)

```
UserFavoriteAnnouncement 조회 (userId 기준)
    └─ 즐겨찾기 없음 → skip

각 즐겨찾기 공고의 applicationEndDate 확인
    ├─ null → skip
    └─ today == endDate - N일 (N = 7, 3, 1)
           └─ EmailNotificationLog 중복 확인 (userId + announcementId + type)
                  ├─ 이미 발송 → skip
                  └─ 미발송 → SMTP 발송 → 로그 저장
```

---

## 중복 발송 방지

`EmailNotificationLog` 테이블에 `UNIQUE(user_id, announcement_id, type)` 인덱스가 있다.

| 타입 | 의미 |
|------|------|
| RECOMMENDATION | 맞춤 공고로 발송된 공고 |
| DEADLINE_7 | D-7 마감 리마인드 발송 완료 |
| DEADLINE_3 | D-3 마감 리마인드 발송 완료 |
| DEADLINE_1 | D-1 마감 리마인드 발송 완료 |

- 맞춤 공고: `success=true`인 로그만 "이미 발송됨"으로 처리 → 발송 실패한 공고는 다음 날 재시도
- 마감 리마인드: `exists()` 쿼리로 타입별 1회만 발송

---

## 이메일 템플릿

경로: `src/main/resources/templates/email/`

| 파일 | 용도 |
|------|------|
| `recommendation.html` | 맞춤 공고 알리미 (공고 카드 리스트, 추천 이유 표시) |
| `deadline-reminder.html` | 마감 리마인드 (D-day 배너 + 단일 공고 강조) |

디자인: 너비 600px, 인라인 CSS (Gmail/Outlook 호환), 한국어

---

## 스케줄러 (`EmailNotificationScheduler`)

```java
@Scheduled(cron = "0 0 9 * * *")  // 매일 09:00
void sendDailyRecommendations()

@Scheduled(cron = "0 5 9 * * *")  // 매일 09:05
void sendDeadlineReminders()
```

- 활성 사용자를 100건씩 페이지 단위로 순회
- 사용자별 try-catch: 1명 실패해도 나머지 사용자 계속 처리
- 완료 시 `sent/skipped/failed` 카운트 로그 출력

---

## 환경 변수 설정

`.env` 파일에 추가 필요:

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
NOTIFICATION_FROM_EMAIL=your-email@gmail.com
```

Gmail 사용 시: Google 계정 보안 → 2단계 인증 활성화 → 앱 비밀번호 생성 필요

---

## 의존성 (build.gradle)

```groovy
implementation 'org.springframework.boot:spring-boot-starter-mail'
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
```

---

## DB 테이블

### `user_favorite_announcement`

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT NOT NULL | 사용자 ID |
| announcement_id | BIGINT NOT NULL | 공고 ID (FK) |
| created_at | DATETIME | BaseTimeEntity |
| updated_at | DATETIME | BaseTimeEntity |

UNIQUE: `(user_id, announcement_id)`

### `email_notification_log`

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT NOT NULL | |
| announcement_id | BIGINT NOT NULL | |
| type | VARCHAR | RECOMMENDATION / DEADLINE_7 / DEADLINE_3 / DEADLINE_1 |
| sent_at | DATETIME | 발송 시각 |
| success | BOOLEAN | 발송 성공 여부 |
| error_message | VARCHAR(500) | 실패 시 오류 메시지 |
| created_at | DATETIME | |
| updated_at | DATETIME | |

UNIQUE: `(user_id, announcement_id, type)`
