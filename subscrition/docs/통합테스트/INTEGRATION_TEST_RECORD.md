# 통합 테스트 기록

테스트 일자: 2026-05-05  
테스트 환경: 로컬 (localhost:8080, MySQL 8.0)  
테스트 이메일 수신 계정: sone91961@gmail.com  
이메일 발신 계정: together580428@gmail.com (Gmail SMTP 앱 비밀번호)

---

## 환경 확인

| 항목 | 상태 |
|------|------|
| MySQL (3306) | ✅ 실행 중 |
| Spring Boot (8080) | ✅ 실행 중 |
| DB `user_favorite_announcement` 테이블 | ✅ 존재 |
| DB `email_notification_log` 테이블 | ✅ 존재 |
| Gmail SMTP 연결 | ✅ 이전 테스트에서 확인 완료 |

---

## 테스트 데이터 준비

### 테스트 계정 생성

```
POST /api/auth/signup
{
  "loginId": "testadmin",
  "password": "Test1234!",
  "phone": "010-1234-5678",
  "email": "sone91961@gmail.com"
}
→ 201 Created, userId=1
```

DB 직접 ADMIN 권한 부여:
```sql
UPDATE users SET role='ADMIN' WHERE login_id='testadmin';
```

### 공고 데이터 (기존 5개 + 신규 4개 = 총 9개)

기존 LH 수집 공고 5개가 DB에 존재했으며, 테스트용 공고 4개 추가 삽입:

| ID | 공고명 | 마감일 | D-day (2026-05-05 기준) |
|----|--------|--------|------------------------|
| 6 | 2025 서울 청년 공공임대 1차 모집 | 2026-05-12 | D-7 |
| 7 | 2025 경기 신혼부부 행복주택 2차 | 2026-06-04 | - |
| 8 | 2025 부산 저소득층 국민임대 모집 | 2026-05-08 | D-3 |
| 9 | 2025 서울 고령자 매입임대 특별공급 | 2026-05-06 | D-1 |

### 공고 카테고리 설정

```sql
-- YOUTH: announcement 1, 2, 3, 6
-- NEWLYWED: announcement 7
-- LOW_INCOME: announcement 4, 8
-- ELDERLY: announcement 9
```

### 사용자 프로필 설정

```
PUT /api/me/profile
{
  "age": 28, "maritalStatus": "SINGLE",
  "isHomeless": true, "isLowIncome": false,
  "preferredRegionLevel1": "서울특별시",
  "preferredHouseType": "아파트", "preferredSupplyType": "공공임대",
  "maxDeposit": 1000, "maxMonthlyRent": 30,
  "categories": ["YOUTH", "HOMELESS"]
}
→ 200 OK, profileCompleted: true
```

---

## 테스트 결과

### 1. 맞춤 추천 공고 API

```
GET /api/recommendations?page=0&size=10
```

| 결과 | |
|------|---|
| 총 추천 수 | 7개 |
| 1위 | [89점] 2025 서울 청년 공공임대 1차 모집 (YOUTH+지역+유형+예산 매칭) |
| 2위 | [36점] 2025 서울 고령자 매입임대 특별공급 |
| 나머지 | 35점, 26점 등 |

**→ PASS** — 카테고리(YOUTH/HOMELESS)와 지역(서울), 유형, 예산 스코어링 정상 동작

---

### 2. 즐겨찾기 API

#### 2-1. 추가 (POST /api/me/favorites)

| 요청 | 응답 | 결과 |
|------|------|------|
| 공고 6 추가 | 201 Created | ✅ PASS |
| 공고 9 추가 | 201 Created | ✅ PASS |
| 공고 8 추가 | 201 Created | ✅ PASS |
| 공고 6 재추가 (멱등성) | 201 Created (중복 없음) | ✅ PASS |

DB 확인: `user_favorite_announcement` — 3개 row (중복 없음)

#### 2-2. 목록 조회 (GET /api/me/favorites)

```json
{
  "totalElements": 3,
  "content": [
    { "announcementId": 6, "noticeName": "2025 서울 청년 공공임대 1차 모집", "applicationEndDate": "2026-05-12" },
    { "announcementId": 8, "noticeName": "2025 부산 저소득층 국민임대 모집",  "applicationEndDate": "2026-05-08" },
    { "announcementId": 9, "noticeName": "2025 서울 고령자 매입임대 특별공급", "applicationEndDate": "2026-05-06" }
  ]
}
```

**→ PASS**

#### 2-3. 존재 여부 확인 (GET /api/me/favorites/{id}/exists)

| 요청 | 응답 | 결과 |
|------|------|------|
| 공고 6 (존재) | `true` | ✅ PASS |
| 공고 99 (미존재) | `false` | ✅ PASS |

#### 2-4. 삭제 (DELETE /api/me/favorites/{id})

| 요청 | 응답 | 결과 |
|------|------|------|
| 공고 8 삭제 | 204 No Content | ✅ PASS |
| 공고 99 삭제 (없음) | 404 Not Found | ✅ PASS |

삭제 후 목록 조회 → totalElements: 2 (공고 6, 9만 남음) **→ PASS**

---

### 3. 맞춤 공고 이메일 발송

트리거: `POST /api/admin/test/email/recommendations` (임시 테스트 엔드포인트, 테스트 완료 후 삭제)

**발송 결과:**

| 로그 ID | 공고 | 타입 | success |
|---------|------|------|---------|
| 1 | 2025 서울 청년 공공임대 1차 모집 | RECOMMENDATION | ✅ 1 |
| 2 | 2025 서울 고령자 매입임대 특별공급 | RECOMMENDATION | ✅ 1 |
| 3 | 과천지식정보타운 근린생활시설용지 | RECOMMENDATION | ✅ 1 |
| 4 | 과천지식정보타운 가스충전소용지 | RECOMMENDATION | ✅ 1 |
| 5 | 과천지식정보타운 단독주택(주거전용)용지 | RECOMMENDATION | ✅ 1 |
| 6 | 2025 부산 저소득층 국민임대 모집 | RECOMMENDATION | ✅ 1 |
| 7 | 2025 경기 신혼부부 행복주택 2차 | RECOMMENDATION | ✅ 1 |

- 추천 7건 모두 발송 성공, error_message = NULL
- **sone91961@gmail.com 수신 확인 필요**

**→ PASS** (발송 성공 로그 기준)

---

### 4. 마감 리마인드 이메일 발송

트리거: `POST /api/admin/test/email/deadlines` (임시 테스트 엔드포인트)

2026-05-05 기준 즐겨찾기 3건의 마감 D-day:

| 공고 | 마감일 | D-day | 발송 타입 |
|------|--------|-------|----------|
| 공고 6 (서울 청년) | 2026-05-12 | D-7 | DEADLINE_7 |
| 공고 8 (부산 저소득) | 2026-05-08 | D-3 | DEADLINE_3 |
| 공고 9 (고령자 매입) | 2026-05-06 | D-1 | DEADLINE_1 |

**발송 결과:**

| 로그 ID | 타입 | 공고 | success |
|---------|------|------|---------|
| 8 | DEADLINE_7 | 2025 서울 청년 공공임대 1차 모집 | ✅ 1 |
| 9 | DEADLINE_3 | 2025 부산 저소득층 국민임대 모집 | ✅ 1 |
| 10 | DEADLINE_1 | 2025 서울 고령자 매입임대 특별공급 | ✅ 1 |

- D-7, D-3, D-1 모두 정확히 매칭되어 발송
- **sone91961@gmail.com 수신 확인 필요**

**→ PASS** (발송 성공 로그 기준)

---

### 5. 중복 발송 방지 검증

배치 2회 실행 후 로그 카운트:

```sql
SELECT type, COUNT(*) FROM email_notification_log GROUP BY type;
-- RECOMMENDATION: 7 (증가 없음)
-- DEADLINE_1:     1 (증가 없음)
-- DEADLINE_3:     1 (증가 없음)
-- DEADLINE_7:     1 (증가 없음)
```

**→ PASS** — 2회 실행해도 로그 중복 없음, 이메일 재발송 없음

---

## 최종 DB 상태

```sql
-- users
SELECT id, login_id, email, profile_completed, status, role FROM users;
-- 1 | testadmin | sone91961@gmail.com | 1 | ACTIVE | ADMIN

-- user_favorite_announcement
SELECT id, user_id, announcement_id FROM user_favorite_announcement;
-- 1 | 1 | 6
-- 2 | 1 | 9
-- 3 | 1 | 8  (일시 삭제 후 재추가)

-- email_notification_log (총 10건)
-- RECOMMENDATION: 7건 (success=1)
-- DEADLINE_7:     1건 (success=1)
-- DEADLINE_3:     1건 (success=1)
-- DEADLINE_1:     1건 (success=1)
```

---

## 수동 확인 필요 항목

- [ ] **sone91961@gmail.com 메일함** — 맞춤 공고 이메일 (제목: "[청약따잇] 오늘의 맞춤 공고가 도착했어요") 수신 확인
  - 이메일 ID 1~7: 발신자명 확인 (이전 세션 발송 — 인코딩 버그 상태)
  - 이메일 ID 11: **발신자명 인코딩 수정 후 발송** → 발신자: `청약따잇 <together580428@gmail.com>` 정상 표시 여부 확인
- [ ] **sone91961@gmail.com 메일함** — 마감 D-7 이메일 (제목: "[청약따잇] D-7 즐겨찾기 공고 마감 임박") 수신 확인
- [ ] **sone91961@gmail.com 메일함** — 마감 D-3 이메일 (제목: "[청약따잇] D-3 즐겨찾기 공고 마감 임박") 수신 확인
- [ ] **sone91961@gmail.com 메일함** — 마감 D-1 이메일 (제목: "[청약따잇] D-1 즐겨찾기 공고 마감 임박") 수신 확인
- [ ] 이메일 HTML 디자인 (공고 카드, 추천 이유, 링크) 렌더링 확인

---

## 6. 버그 수정 후 추가 검증 (2026-05-05)

### 발신자 이름 인코딩 수정 확인

서버 재시작 후 (인코딩 수정 적용) 추가 발송:

| 로그 ID | 공고 | 타입 | success | 발송 시각 |
|---------|------|------|---------|----------|
| 11 | 2025 경기 신혼부부 행복주택 2차 | RECOMMENDATION | ✅ 1 | 11:29:13 |

- `InternetAddress(email, name, "UTF-8")` 방식으로 발송
- **sone91961@gmail.com에서 발신자 이름 정상 표시 여부 육안 확인 필요**

### LH 수집 토지 공고 필터링 확인

```
POST /api/admin/import/lh?page=1&size=3
→ { "imported": 0, "failed": 0 }
```

page=1의 3건이 모두 토지 공고(UPP_AIS_TP_CD="01")였으며 전부 필터링됨 — **정상**

```
POST /api/admin/import/lh?page=2&size=3
→ { "imported": 3, "failed": 0 }
```

page=2의 주택 공고 3건 정상 수집 (ID 10 신규 추가) — **정상**

### application_start_date 수집 확인

| ID | 공고명 | application_start_date |
|----|--------|----------------------|
| 4 | 양주옥정 영구임대 | 2026-05-19 (SBSC_ACP_ST_DT 반영) |
| 10 | 2026년 1차 일반 매입임대주택 | 2026-05-12 (신규 수집, 정상) |

**→ PASS** — 상세 API에서 시작일 정상 반영됨

---

## 테스트 통과 기준 요약

| 테스트 항목 | 결과 |
|------------|------|
| 회원가입 → DB row 생성 | ✅ PASS |
| 프로필 설정 → profileCompleted=true | ✅ PASS |
| 맞춤 추천 API (스코어 기반 정렬) | ✅ PASS |
| 즐겨찾기 추가 (201, 멱등) | ✅ PASS |
| 즐겨찾기 삭제 (204, 없으면 404) | ✅ PASS |
| 즐겨찾기 목록 조회 | ✅ PASS |
| 즐겨찾기 존재 여부 확인 | ✅ PASS |
| 맞춤 공고 이메일 발송 (7건, success=1) | ✅ PASS |
| 마감 D-7/D-3/D-1 이메일 발송 (3건) | ✅ PASS |
| 중복 발송 방지 (2회 실행 시 로그 불변) | ✅ PASS |
| email_notification_log DB 저장 | ✅ PASS |
| 토지 공고 필터링 (page=1 → 0/0) | ✅ PASS |
| 주택 공고 수집 (page=2 → 3/0) | ✅ PASS |
| application_start_date SBSC_ACP_ST_DT 반영 | ✅ PASS |
| 이메일 인코딩 수정 후 재발송 성공 | ✅ PASS |

**전체 14/14 PASS** (발신자 이름 정상 표시는 sone91961@gmail.com 육안 확인 필요)
