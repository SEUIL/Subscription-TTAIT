# 청약 따잇 - 프로젝트 분석 문서

> 데모 기반 Spring Boot + React 본격 개발 전 참고용 분석

---

## 1. 전체 기능 목록

### 인증 & 사용자 관리
- 회원가입 (loginId, 비밀번호, 전화번호, 이메일)
- 로그인 (JWT 토큰 발급, 12시간 유효)
- 사용자 프로필 관리 (나이, 혼인상태, 자녀 수, 선호 설정)
- 사용자 상태 관리 (ACTIVE / INACTIVE)
- 무주택자 / 저소득층 / 고령자 플래그 관리

### 공고 관리
- 외부 API 연동 데이터 수집 (LH, 마이홈, RTMS)
- 공고 중복 감지 및 병합
- 공고 상태 관리 (예정 / 모집중 / 마감)
- 다중 필터링 (지역 1/2단계, 공급 유형, 제공기관, 상태, 키워드)
- 정렬 (추천순 / 최신순)
- 첨부파일 관리 (문서, 이미지)
- 시세 비교 데이터 연동

### 맞춤 추천
- 사용자 프로필 기반 선호 지역/주택유형/공급유형 설정
- 개인화 추천 엔진 (공고-사용자 조건 매칭)
- 최대 보증금 / 월세 임계값 설정

### 알림
- 알림 유형: 신규공고, 모집시작 D-1, 마감 D-3, 마감 D-1, 마감일
- 알림 배치 처리
- 읽음 처리 (읽은 시각 기록)
- 알림 이력 관리

### 저장 공고
- 공고 저장 / 저장 취소
- 저장 목록 조회

### 외부 데이터 연동
- LH / 마이홈 / RTMS API 수집
- 원본 페이로드 저장 (감사 추적)
- 데이터 정규화 및 통합

### 시세 데이터
- 지역별 시세 스냅샷 (LAWD 코드 매핑)
- 공고 시세 비교 데이터

---

## 2. 현재 기술 스택

### 백엔드
| 항목 | 내용 |
|------|------|
| Framework | Spring Boot 3.3.2 |
| Language | Java 17 |
| Build | Gradle 9.4.1 |
| Database | MySQL |
| ORM | Spring Data JPA + Hibernate |
| Auth | JWT (jjwt 0.12.6) + Spring Security |
| Validation | Jakarta Validation |
| API 문서 | Springdoc OpenAPI (Swagger UI) |
| 기타 | Lombok, 소프트 삭제, 낙관적 락(version) |

### 프론트엔드 (현재 데모)
| 항목 | 내용 |
|------|------|
| 언어 | HTML5, CSS3, Vanilla JS (ES6 모듈) |
| API 클라이언트 | Fetch API |
| 상태 저장 | LocalStorage |
| 빌드 도구 | 없음 (정적 파일 직접 서빙) |

---

## 3. API 엔드포인트 목록

### 인증 (Public)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 (JWT 반환) |

### 공고 (Public)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/announcements` | 목록 조회 (필터/페이징) |
| GET | `/api/announcements/{id}` | 상세 조회 |
| GET | `/api/filters/**` | 필터 옵션 조회 |

### 사용자 (Authenticated)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/me` | 내 프로필 조회 |
| PUT | `/api/me/profile` | 프로필 수정 |
| POST | `/api/announcements/{id}/save` | 공고 저장 |
| DELETE | `/api/announcements/{id}/save` | 저장 취소 |
| GET | `/api/notifications` | 알림 목록 |
| PATCH | `/api/notifications/{id}/read` | 알림 읽음 처리 |
| GET | `/api/recommendations` | 맞춤 추천 목록 |

### 관리자 (Admin)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| * | `/api/admin/**` | 관리자 전용 (역할 기반) |

### 기타
| 경로 | 설명 |
|------|------|
| `/swagger-ui.html` | Swagger UI |
| `/v3/api-docs/**` | OpenAPI 스펙 |

---

## 4. 데이터 구조 / 모델

### User
```
id, loginId (unique), passwordHash, phone, email (unique),
status (ACTIVE/INACTIVE), createdAt, updatedAt, deleted
```

### UserProfile (User 1:1)
```
id, userId, age, maritalStatus, childrenCount,
isHomeless, isLowIncome, isElderly,
preferredRegionLevel1, preferredRegionLevel2,
preferredHouseType, preferredSupplyType,
maxDeposit, maxMonthlyRent
```

### Announcement
```
id, sourcePrimary (LH/MYHOME/RTMS), sourceNoticeId,
noticeName, providerName, sourceNoticeUrl,
noticeStatus (SCHEDULED/OPEN/CLOSED),
announcementDate, applicationStartDate, applicationEndDate, winnerAnnouncementDate,
regionLevel1, regionLevel2, fullAddress, legalCode,
complexName, supplyTypeRaw, supplyTypeNormalized,
houseTypeRaw, houseTypeNormalized,
depositAmount, monthlyRentAmount, supplyHouseholdCount,
matchKey, isMerged, mergedGroupKey, version,
collectedAt, createdAt, updatedAt, deleted
```

### AnnouncementDetail / AnnouncementAttachment / AnnouncementCategoryTag
```
공고 세부 정보, 첨부파일, 카테고리 태그
```

### Notification
```
id, userId, announcementId,
notificationType (NEW_NOTICE/START_MINUS_1/END_MINUS_3/END_MINUS_1/END_DAY),
title, message, isRead, readAt, createdAt, updatedAt
```

### UserSavedAnnouncement
```
userId, announcementId
```

### MarketRentSnapshot / LawdCodeMapping
```
지역별 시세 스냅샷 / 법정코드-지역 매핑
```

### RawPayload
```
외부 API 원본 JSON 저장 (감사 추적용)
```

### 주요 Enum
```
AnnouncementStatus: SCHEDULED, OPEN, CLOSED
SourceType:         LH, MYHOME, RTMS
NotificationType:   NEW_NOTICE, START_MINUS_1, END_MINUS_3, END_MINUS_1, END_DAY
UserStatus:         ACTIVE, INACTIVE
MaritalStatus:      (청약 자격 기준 상태값)
```

### 공통 패턴
- 소프트 삭제 (`SoftDeleteBaseEntity` 상속)
- 낙관적 락 (Announcement의 `version` 필드)
- Auditing (`createdAt`, `updatedAt` 자동 관리)

---

## 5. Spring Boot + React 아키텍처 제안

### 전체 구조

```
+----------------------------------------------------+
|          React Frontend (Vite + TypeScript)        |
|  - Zustand / React Query for state management      |
|  - Axios interceptor for JWT auth                  |
|  - Tailwind CSS or shadcn/ui for styling           |
|  - React Router v6 for routing                     |
+----------------------+-----------------------------+
                       | HTTPS / REST
                       v
+----------------------------------------------------+
|         Spring Boot Backend (그대로 유지)            |
|  - 현재 API 구조 그대로 재사용 가능                   |
|  - CORS 설정 추가 필요 (React dev server용)          |
|  - Spring Security에 React origin 허용 추가         |
+----------------------+-----------------------------+
                       | JDBC
                       v
+----------------------------------------------------+
|                  MySQL Database                    |
+----------------------------------------------------+
```

### 프론트엔드 디렉토리 구조 (권장)

```
frontend/
├── src/
│   ├── api/            # axios 인스턴스, API 함수 모음
│   │   ├── auth.ts
│   │   ├── announcements.ts
│   │   ├── notifications.ts
│   │   └── user.ts
│   ├── components/     # 재사용 컴포넌트
│   │   ├── AnnouncementCard.tsx
│   │   ├── FilterPanel.tsx
│   │   └── NotificationItem.tsx
│   ├── pages/          # 라우트별 페이지
│   │   ├── ListPage.tsx
│   │   ├── DetailPage.tsx
│   │   ├── AuthPage.tsx
│   │   ├── MyPage.tsx
│   │   └── RecommendPage.tsx
│   ├── stores/         # Zustand 전역 상태
│   │   └── authStore.ts
│   ├── hooks/          # 커스텀 훅 (React Query)
│   │   ├── useAnnouncements.ts
│   │   └── useNotifications.ts
│   └── types/          # TypeScript 타입 정의 (백엔드 모델 반영)
│       ├── announcement.ts
│       ├── user.ts
│       └── notification.ts
```

### 백엔드 추가/보완 필요 항목

| 항목 | 현황 | 조치 |
|------|------|------|
| 저장 공고 목록 GET | LocalStorage 사용 중 | `GET /api/me/saved` 엔드포인트 추가 |
| 알림 설정 저장 | LocalStorage 사용 중 | 알림 설정 테이블/API 추가 |
| CORS 설정 | 미설정 | `@CrossOrigin` 또는 `WebMvcConfigurer` 추가 |
| Refresh Token | 미구현 | 선택적 추가 (12시간 만료로 운영 가능) |
| 파일 업로드 | 미구현 | 관리자 첨부파일 업로드 필요 시 추가 |

### 개발 순서 권장

1. **환경 설정**: Vite + React + TypeScript 프로젝트 생성, CORS 설정
2. **타입 정의**: 백엔드 모델 기반 TypeScript 인터페이스 작성
3. **API 클라이언트**: Axios 인스턴스 + JWT 인터셉터
4. **인증 플로우**: 로그인 -> JWT 저장 -> 보호 라우트
5. **공고 목록/상세**: 현재 가장 완성도 높은 기능 -> 우선 구현
6. **저장/추천/알림**: 순서대로 구현
7. **마이페이지**: 프로필 편집

### 환경 변수 분리 (보안 필수)

```properties
# application-prod.properties 분리 필요
jwt.secret=<강력한_랜덤_키>
lh.api.serviceKey=<실제_키>
myhome.api.serviceKey=<실제_키>
spring.datasource.password=<실제_비밀번호>
```
