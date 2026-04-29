# Figma 이관용 컴포넌트 표

## 1. 목적

이 문서는 `demo`와 `main` 프론트에서 공통으로 사용할 수 있는 UI를 **Figma 컴포넌트 / Variant / Property 구조**로 옮기기 쉽게 정리한 표다.

원칙:
- 디자인 언어는 공통
- 구현 코드는 분리
- Figma에서는 먼저 공통 UI Kit를 만들고, 이후 demo/main 화면에 각각 조합한다.

---

## 2. Figma 파일 구조 권장

```text
00_Foundation
  - Color
  - Typography
  - Spacing
  - Radius
  - Shadow
  - Icon

01_Components
  - Button
  - Input
  - Chip
  - Badge
  - Card
  - Header
  - Footer
  - Tabs
  - Sidebar
  - Timeline
  - Toggle
  - Empty State

02_Patterns
  - Filter Bar
  - Listing Grid
  - Detail Info Section
  - Profile Form Section
  - Notification List
  - Admin Review Panel

03_Pages_Demo
  - List
  - Detail
  - Auth
  - MyPage
  - Recommend

04_Pages_Main
  - List
  - Detail
  - Login
  - Signup
  - MyPage
  - Admin Review List
  - Admin Review Detail
```

---

## 3. Foundation 표

## 3-1. Color Token

| Token | 용도 | 예시 설명 |
|---|---|---|
| `color.primary` | 브랜드 메인 | 주요 CTA, 활성 상태 |
| `color.primary.hover` | 메인 hover | 버튼 hover |
| `color.primary.soft` | 메인 연한 배경 | 선택 chip 배경 |
| `color.text.primary` | 기본 텍스트 | 제목, 본문 |
| `color.text.secondary` | 보조 텍스트 | 메타 정보 |
| `color.text.inverse` | 역상 텍스트 | primary 버튼 텍스트 |
| `color.border.default` | 기본 보더 | input/card/chip |
| `color.surface.default` | 기본 배경 | 페이지 배경 |
| `color.surface.elevated` | 카드 배경 | 카드/패널 |
| `color.status.open` | 모집중 | open badge |
| `color.status.closing` | 마감임박 | closing badge |
| `color.status.closed` | 모집종료 | closed badge |
| `color.success` | 성공 | 완료 메시지 |
| `color.warning` | 경고 | 주의 문구 |
| `color.error` | 오류 | 에러 메시지 |
| `color.info` | 정보 | 안내 배지 |

## 3-2. Typography Token

| Token | 스타일 | 사용처 |
|---|---|---|
| `type.display.lg` | 큰 제목 | 메인 히어로 |
| `type.heading.lg` | 페이지 타이틀 | 상세/추천 상단 |
| `type.heading.md` | 섹션 타이틀 | 카드 섹션 제목 |
| `type.body.md` | 본문 기본 | 일반 설명 |
| `type.body.sm` | 보조 본문 | 메타/설명 |
| `type.label.md` | 버튼/필드 라벨 | 버튼, chip |
| `type.caption` | 캡션 | 보조 안내 |

## 3-3. Spacing / Radius / Shadow

| Token | 권장값 범위 | 사용처 |
|---|---|---|
| `space.4` | 4px | 초소형 간격 |
| `space.8` | 8px | 아이콘-텍스트 간격 |
| `space.12` | 12px | chip/card 내부 |
| `space.16` | 16px | 기본 블록 간격 |
| `space.24` | 24px | 섹션 간격 |
| `space.32` | 32px | 큰 섹션 간격 |
| `radius.sm` | 8px | input/chip |
| `radius.md` | 12px | 버튼/카드 |
| `radius.lg` | 16~20px | 큰 카드/패널 |
| `shadow.sm` | 약한 그림자 | 기본 카드 |
| `shadow.md` | 중간 그림자 | sticky card, modal |

---

## 4. 공통 컴포넌트 표

## 4-1. Button

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Button` |
| Variant 속성 | `style=primary/secondary/ghost`, `size=sm/md/lg`, `state=default/hover/pressed/disabled`, `icon=none/left/right` |
| 기본 사용처 | 로그인, 저장, 신청, 관리자 액션 |
| 공통 규칙 | 높이 규격 고정, 텍스트 중앙 정렬, radius 일관성 유지 |
| demo 우선형 | primary, ghost 위주 |
| main 추가형 | disabled, loading, destructive |

## 4-2. Input

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Input / Field` |
| Variant 속성 | `type=text/password/email/number/select`, `state=default/focus/error/disabled`, `label=show/hide`, `help=show/hide` |
| 기본 사용처 | 로그인, 회원가입, 프로필 수정 |
| 서브 컴포넌트 | label, helper text, right icon, password toggle |
| main 필수 추가 | validation error, readonly, required 표시 |

## 4-3. Chip

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Chip` |
| Variant 속성 | `kind=filter/category/summary`, `state=default/selected/hover/disabled`, `icon=on/off` |
| 기본 사용처 | 필터, 카테고리 선택, 추천 요약 |
| demo 핵심 | 클릭 반응이 명확해야 함 |
| main 핵심 | 선택/해제/비활성 상태 구분 강화 |

## 4-4. Badge

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Badge / Status` |
| Variant 속성 | `tone=open/closing/closed/info/warning/success`, `size=sm/md` |
| 기본 사용처 | 모집상태, 알림 상태, 검수 상태 |
| main 추가형 | `review=pending/approved/corrected/rejected` |

## 4-5. Tabs

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Tabs` |
| Variant 속성 | `type=line/segment`, `state=default/active/hover`, `count=2/3/4` |
| 기본 사용처 | 로그인/회원가입 전환, 관리자 상태 필터 |
| 설계 포인트 | 탭 active 기준이 분명해야 함 |

## 4-6. Toggle

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Toggle` |
| Variant 속성 | `state=on/off/disabled`, `label=show/hide` |
| 기본 사용처 | 알림 설정 |
| main 중요도 | 접근성 라벨 포함 고려 |

---

## 5. 도메인 컴포넌트 표

## 5-1. Listing Card

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Card / Listing` |
| Variant 속성 | `status=open/closing/closed`, `type=rent/sale`, `saved=true/false`, `size=default/compact` |
| 포함 요소 | 썸네일, 기관명, 제목, 지역, 가격, 마감 문구, 태그, 저장 액션 |
| 사용 페이지 | 목록, 추천, 저장 공고 |
| demo 핵심 | 시각적 매력, 정보 요약 |
| main 핵심 | 데이터 정합성, 텍스트 overflow 대응 |

## 5-2. Filter Pill Bar

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Pattern / Filter Pill Bar` |
| Variant 속성 | `scroll=true/false`, `selected=single/multi` |
| 포함 요소 | 공급유형 아이콘, 레이블, 선택 상태 |
| 사용 페이지 | 공고 목록 |
| 설계 포인트 | 모바일 가로 스크롤 대응 고려 |

## 5-3. Header

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Header / Global` |
| Variant 속성 | `type=main/detail/auth/admin`, `loggedIn=true/false`, `device=desktop/mobile` |
| 포함 요소 | 로고, 검색 또는 내비, 로그인/프로필 액션 |
| demo 사용 | main/detail/auth 중심 |
| main 사용 | user/admin 분기 포함 |

## 5-4. Footer

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Footer / Global` |
| Variant 속성 | `type=default/minimal` |
| 사용 페이지 | 목록, 추천, 일반 페이지 |
| 설계 포인트 | demo는 간단하게, main은 정책 링크 확장 가능 |

## 5-5. Detail Info Grid

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Pattern / Detail Info Grid` |
| Variant 속성 | `columns=2/3`, `density=default/compact` |
| 포함 요소 | label/value 쌍 |
| 사용 페이지 | 공고 상세 |
| main 중요도 | 긴 값 줄바꿈 규칙 필요 |

## 5-6. Timeline

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Timeline / Application` |
| Variant 속성 | `state=default/current/completed`, `orientation=vertical` |
| 포함 요소 | 날짜, 단계명, 설명, D-day badge |
| 사용 페이지 | 공고 상세 |
| 설계 포인트 | 현재 단계 강조가 핵심 |

## 5-7. Sticky Summary Card

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Card / Sticky Summary` |
| Variant 속성 | `type=rent/sale`, `state=default` |
| 포함 요소 | 핵심 가격, 마감 정보, CTA, 빠른 정보 |
| 사용 페이지 | 상세 우측 패널 |
| demo 중요도 | 신청 전환 유도 |
| main 중요도 | 원문 링크와 CTA 안정성 |

## 5-8. Notification Item

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Item / Notification` |
| Variant 속성 | `read=true/false`, `type=deadline/recommend/system` |
| 포함 요소 | 읽음 점, 제목, 메시지, 시간 |
| 사용 페이지 | 마이페이지 알림 |
| main 추가형 | 클릭/읽음 처리 상태 |

## 5-9. Sidebar Navigation

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Sidebar / Account` |
| Variant 속성 | `section=profile/saved/notifications/admin`, `state=default/active` |
| 포함 요소 | 프로필 요약, 메뉴, 로그아웃 |
| 사용 페이지 | 마이페이지, 관리자 페이지 |

## 5-10. Empty State

| 항목 | 내용 |
|---|---|
| Figma 이름 | `State / Empty` |
| Variant 속성 | `type=list/saved/notification/search/error403` |
| 포함 요소 | 아이콘/일러스트, 제목, 설명, CTA |
| main 중요도 | 빈 결과에서도 다음 행동 제시 |

## 5-11. Loading State

| 항목 | 내용 |
|---|---|
| Figma 이름 | `State / Loading` |
| Variant 속성 | `type=card-list/detail/form/table` |
| 사용 페이지 | 전 페이지 공통 |
| main 중요도 | 스켈레톤 우선 설계 권장 |

---

## 6. 관리자 전용 컴포넌트 표

## 6-1. Review Status Badge

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Badge / Review Status` |
| Variant 속성 | `status=pending/approved/corrected/rejected` |
| 사용 페이지 | 관리자 검수 목록/상세 |

## 6-2. Review Row Item

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Item / Review Row` |
| Variant 속성 | `status=pending/approved/corrected/rejected`, `selected=true/false` |
| 포함 요소 | 공고명, 기관, 상태, 요약값, 검수자/일시 |
| 사용 페이지 | 관리자 검수 목록 |

## 6-3. Review Diff Panel

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Panel / Review Diff` |
| Variant 속성 | `type=raw-vs-ai/raw-vs-corrected`, `density=default/compact` |
| 포함 요소 | 원문, AI 추출값, 수정값 |
| 사용 페이지 | 관리자 검수 상세 |
| main 중요도 | 비교가 직관적이어야 함 |

## 6-4. Admin Action Bar

| 항목 | 내용 |
|---|---|
| Figma 이름 | `Bar / Admin Actions` |
| Variant 속성 | `state=default/loading/disabled` |
| 포함 요소 | APPROVE, CORRECT, REJECT, REIMPORT 버튼 |
| 사용 페이지 | 관리자 검수 상세 |

---

## 7. 페이지별 조합 가이드

| 페이지 | 필요한 핵심 컴포넌트 |
|---|---|
| Demo 목록 | Header, Filter Pill Bar, Chip, Listing Card, Footer |
| Demo 상세 | Header, Badge, Detail Info Grid, Timeline, Sticky Summary Card |
| Demo 인증 | Header/Auth Header, Tabs, Input, Button, Chip |
| Demo 마이페이지 | Header, Sidebar Navigation, Input, Notification Item, Toggle |
| Demo 추천 | Header, Summary Chip, Listing Card, Footer |
| Main 목록 | Header, Filter Bar, Listing Card, Loading/Empty/Error State |
| Main 상세 | Header, Badge, Detail Info Grid, Timeline, Sticky Summary Card, Error State |
| Main 로그인/회원가입 | Tabs, Input, Button, Validation Message |
| Main 마이페이지 | Sidebar Navigation, Profile Form Section, Empty State |
| Main 관리자 목록 | Header/Admin, Tabs, Review Status Badge, Review Row Item, Loading State |
| Main 관리자 상세 | Review Diff Panel, Admin Action Bar, Badge, Form Field |

---

## 8. Figma 컴포넌트 네이밍 규칙 권장

```text
Button/Primary
Button/Secondary
Input/Text
Input/Select
Chip/Filter
Chip/Category
Badge/Status
Card/Listing
Card/StickySummary
Item/Notification
Panel/ReviewDiff
State/Empty
State/Loading
```

Variant property 예시:

```text
style=primary
size=md
state=default
icon=left
```

---

## 9. 우선 제작 순서

### 1차로 꼭 만들 컴포넌트
1. Button
2. Input
3. Chip
4. Badge
5. Listing Card
6. Header
7. Timeline
8. Empty State

### 2차
9. Sidebar Navigation
10. Notification Item
11. Sticky Summary Card
12. Tabs
13. Toggle

### 3차
14. Review Status Badge
15. Review Row Item
16. Review Diff Panel
17. Admin Action Bar

---

## 10. 실무 팁

- Figma에서 먼저 **Foundation → Atomic Component → Pattern → Page** 순서로 만든다.
- demo와 main은 페이지를 분리하되, 가능한 한 같은 컴포넌트 라이브러리를 참조한다.
- 단, admin UI는 일반 사용자 UI와 분리된 섹션으로 두는 것이 관리가 쉽다.
