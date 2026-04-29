# 메인 버전 프론트 기능서 / 화면 설계서 / 디자인 기획서

## 1. 문서 목적

본 문서는 `subscrition` 백엔드를 기준으로 실제 운영 가능한 메인 프론트를 설계하기 위한 기준 문서다.

이 버전은 **실서비스 구축용**이며, demo와 별개로 개발한다.

---

## 2. 현재 백엔드 기준 범위

### 확인된 공개 API
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/announcements`
- `GET /api/announcements/{announcementId}`

### 확인된 인증 사용자 API
- `GET /api/me`
- `PUT /api/me/profile`

### 확인된 관리자 API
- `GET /api/admin/review`
- `GET /api/admin/review/{announcementId}`
- `POST /api/admin/review/{announcementId}`
- `POST /api/admin/import/lh`

### 문서상 존재하지만 구현 추가가 필요한 영역
- 저장 공고
- 알림 목록 / 읽음 처리
- 추천 목록
- 필터 옵션 조회
- 알림 설정

즉, 메인 프론트는 **현재 구현 API 기준 MVP**와 **추가 예정 기능**을 분리해서 설계해야 한다.

---

## 3. 메인 버전 목표

### 제품 목표
1. 실제 회원가입/로그인/공고 탐색/프로필 관리가 가능해야 한다.
2. API 구조와 정합성 있는 프론트를 만든다.
3. 일반 사용자 화면과 관리자 검수 화면을 모두 제공한다.
4. 향후 추천/알림/저장 공고 기능을 확장하기 쉬운 구조로 만든다.

---

## 4. 권장 프론트 아키텍처

## 4-1. 기술 스택
- React
- TypeScript
- Vite
- React Router
- TanStack Query
- Zustand 또는 Context 기반 auth store
- Axios 또는 fetch wrapper
- Tailwind CSS 또는 CSS Module + design token

## 4-2. 권장 폴더 구조

```text
frontend/
 └─ src/
    ├─ app/
    ├─ pages/
    ├─ widgets/
    ├─ features/
    │  ├─ auth/
    │  ├─ announcement/
    │  ├─ profile/
    │  ├─ review-admin/
    │  └─ common/
    ├─ entities/
    ├─ shared/
    │  ├─ api/
    │  ├─ ui/
    │  ├─ lib/
    │  ├─ config/
    │  └─ types/
    └─ routes/
```

## 4-3. 앱 분리 방식

### 권장안
한 개의 React 앱 안에서 라우트 기준 분리:
- public 영역
- user 영역
- admin 영역

### 이유
- 공통 디자인 시스템 재사용 가능
- 인증/권한 관리 일관성 유지
- 배포 단순화

---

## 5. 사용자 그룹

### 일반 사용자
- 공고 탐색
- 상세 조회
- 회원가입/로그인
- 내 프로필 관리
- 향후 저장/추천/알림 사용

### 관리자
- AI 파싱 검수 목록 확인
- 검수 상세 확인
- 승인/수정/반려/재수집 실행
- 수집 상태 운영

---

## 6. 정보구조(IA)

```text
공개 영역
 ├─ 홈/목록
 ├─ 공고 상세
 ├─ 로그인
 └─ 회원가입

로그인 사용자 영역
 ├─ 마이페이지
 │  ├─ 내 정보
 │  ├─ 선호 조건 관리
 │  ├─ 저장 공고 (추가 예정)
 │  ├─ 알림 (추가 예정)
 │  └─ 추천 (추가 예정)

관리자 영역
 ├─ 검수 목록
 ├─ 검수 상세
 └─ LH 수집 실행
```

---

## 7. 메인 기능서

## 7-1. 공고 목록

### 목적
공고를 안정적으로 탐색하는 핵심 진입 화면.

### MVP 기능
- 페이지네이션 기반 목록 조회
- 공고 카드 표시
- 상태/유형/지역 필터 UI는 선구현 가능하되, 실제 API 스펙 확정 전까지 disabled 또는 local 상태로 관리

### API
- `GET /api/announcements?page=0&size=20`

### 카드 표시 항목
- 공고명
- 공급기관명
- 공급유형
- 주택유형
- 지역 1/2단계
- 단지명
- 보증금
- 월세
- 접수 시작일/종료일
- 공고 상태

### 필수 상태
- 로딩
- 결과 없음
- 에러
- 페이지 이동

---

## 7-2. 공고 상세

### 목적
상세 판단과 신청 링크 이동을 지원.

### API
- `GET /api/announcements/{announcementId}`

### 표시 항목
- 공고명
- 공급기관명
- 공고 상태
- 공고일 / 접수 시작 / 접수 종료 / 당첨자 발표일
- 공급유형 / 주택유형 / 단지명 / 주소
- 보증금 / 월세 / 세대수 / 공급호수
- 난방 방식 / 전용면적 / 입주예정
- 신청일시 설명 / 안내문 / 연락처 / 원문 링크

### UX 원칙
- 정책 정보는 섹션 단위로 잘라서 보여준다.
- 원문 링크는 항상 제공한다.
- 일정 정보는 상단에서 먼저 노출한다.

---

## 7-3. 회원가입 / 로그인

### API
- `POST /api/auth/signup`
- `POST /api/auth/login`

### 회원가입 입력
- loginId
- password
- phone
- email

### 로그인 결과
- `userId`
- `loginId`
- `accessToken`

### UX 원칙
- 로그인 후 토큰 저장
- 보호 라우트 접근 제어
- 토큰 만료 시 재로그인 유도
- 에러 메시지 사용자 언어화

---

## 7-4. 마이페이지

### 현재 MVP 기능
- 내 프로필 조회
- 내 프로필 수정

### API
- `GET /api/me`
- `PUT /api/me/profile`

### 입력/표시 항목
- 기본 정보: loginId, email, phone
- 개인 조건: age, maritalStatus, childrenCount
- 자격 플래그: isHomeless, isLowIncome, isElderly
- 선호 조건: preferredRegionLevel1, preferredRegionLevel2, preferredHouseType, preferredSupplyType
- 예산: maxDeposit, maxMonthlyRent
- 카테고리: categories

### 중요 포인트
- 프로필은 추천/알림의 기반 데이터이므로 입력 폼 UX가 매우 중요하다.
- 저장 성공/실패 피드백이 명확해야 한다.

---

## 7-5. 관리자 검수

### 목적
AI 파싱 결과를 운영자가 검수/보정하는 백오피스.

### API
- `GET /api/admin/review?status=PENDING&page=0&size=20`
- `GET /api/admin/review/{announcementId}`
- `POST /api/admin/review/{announcementId}`
- `POST /api/admin/import/lh?page=1&size=10`

### 검수 액션
- APPROVE
- CORRECT
- REJECT
- REIMPORT

### 화면 구성

#### 검수 목록
- 상태 필터(PENDING / APPROVED / CORRECTED / REJECTED)
- 공고명
- 기관명
- 주요 파싱 결과 요약
- 검수 상태
- 검수자/검수일(있을 경우)

#### 검수 상세
- 공고 기본 정보
- AI 파싱 결과 값
- 원문(raw) 비교
- 수정 입력 폼
- 검수 메모
- 액션 버튼 4종

#### 수집 실행 패널
- page / size 입력
- 실행 버튼
- imported / failed 결과 요약

### 권한
- `ADMIN`만 접근
- 일반 사용자 접근 시 403 대응 UI 제공

---

## 8. 추가 예정 기능 설계

현재 문서에는 있으나 백엔드 구현이 덜 된 기능은 아래처럼 선행 설계만 한다.

### 저장 공고
- 사용자 관심 공고 보관
- 목록/상세에서 저장 토글
- 마이페이지 저장 공고 탭

### 추천
- 프로필 기반 개인화 목록
- 추천 사유 노출
- 추후 별도 추천 점수 표기

### 알림
- 신규 공고 / 마감 알림
- 읽음 처리
- 알림 설정 관리

이 3개는 **UI 선구현 가능**, 단 실제 API 붙이기는 백엔드 준비 후 진행한다.

---

## 9. 디자인 기획

## 9-1. 브랜드 방향
- 공공 데이터 기반이지만 차갑지 않게
- 신뢰 가능하지만 어렵지 않게
- 복잡한 정책 정보를 “행동 가능한 정보”로 바꿔주는 제품

## 9-2. 핵심 UX 원칙
1. 중요한 정보부터 먼저 보여준다.
2. 사용자가 모르는 행정 용어는 설명한다.
3. 비용/일정/자격조건을 빠르게 비교하게 한다.
4. 빈 상태와 실패 상태에서도 다음 행동을 안내한다.

## 9-3. 주요 컴포넌트
- App shell
- Global header
- Announcement card
- Filter bar
- Status badge
- Empty state
- Error state
- Profile form section
- Review diff panel
- Admin action footer

## 9-4. 톤앤매너
- 텍스트는 “행정 문구 복붙”보다 “사용자 안내형 문장” 우선
- 단, 관리자 화면은 원문(raw)도 함께 보여줌

---

## 10. 개발 단계 제안

## 1단계: 메인 프론트 뼈대
- React 프로젝트 생성
- 라우터 구성
- auth store 구성
- API client 구성
- 공통 레이아웃 구성

## 2단계: 사용자 MVP
- 목록
- 상세
- 로그인/회원가입
- 마이페이지

## 3단계: 관리자 MVP
- 검수 목록
- 검수 상세
- 검수 액션 연결
- 수집 실행 패널

## 4단계: 확장 기능
- 저장 공고
- 추천
- 알림
- 필터 고도화

## 5단계: 운영 보강
- 에러 처리 정교화
- 접근권한 가드
- 폼 검증 강화
- 반응형 보완

---

## 11. 완료 기준(Definition of Done)

### 사용자 영역
- 회원가입/로그인 가능
- 공고 목록/상세 조회 가능
- 내 프로필 조회/수정 가능
- 인증 상태에 따른 라우트 제어 가능

### 관리자 영역
- 검수 목록/상세 조회 가능
- APPROVE/CORRECT/REJECT/REIMPORT 실행 가능
- ADMIN 권한 가드 적용

### 품질 기준
- 로딩/에러/빈 상태 존재
- API 타입 정의 존재
- 화면/도메인 구조가 기능별로 분리되어 있음
- demo 코드에 의존하지 않음

---

## 12. 메인 버전 의사결정 메모

- demo와 동일한 화면 흐름은 참고하되, 코드는 재사용하지 않는다.
- 메인 버전은 백엔드 API와 DTO를 기준으로 타입을 먼저 설계한다.
- 관리자 검수 화면은 일반 사용자 화면과 같은 중요도로 본다.
