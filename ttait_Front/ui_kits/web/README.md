# 집구해 Web UI Kit

Interactive click-through prototype of all 4 core screens.

## Screens
| Screen | Description |
|--------|-------------|
| Main Listing | Category pills, filter tags, 4-col card grid |
| Detail | Hero, info grid, timeline, market comparison, sticky sidebar |
| Login / Sign-up | Tab switch, 2-step sign-up, category preference grid |
| My Page | Profile form, saved listings, notification centre |

## Files
| File | Role |
|------|------|
| `index.html` | Entry point — React shell, routing, shared CSS vars |
| `Header.jsx` | Sticky site header (logo, search, bell, avatar) |
| `FilterBar.jsx` | Category pill bar + filter tag row |
| `ListingCard.jsx` | Card component with image, status badge, heart, tags |
| `DetailPage.jsx` | Full detail layout — left column + sticky right card |
| `AuthPage.jsx` | Login / 2-step sign-up / success state |
| `MyPage.jsx` | Sidebar + profile/saved/notifications sections |

## Usage
Open `index.html` in a browser. Navigate between screens using the header
avatar (→ My Page), back buttons, and card clicks.

## Design Notes
- Font: Noto Sans KR 400/500/600/700
- Brand red: #ff385c
- Three-layer card shadow for all elevated surfaces
- Components are cosmetic-only — no real API calls

## 관리자 DTO 필드 재설계 핸드오프

`docs/frontend/ADMIN_DTO_FIELD_DESIGN_HANDOFF.md` 기준으로 관리자 DTO 필드 노출 정책과 화면 배치를 다시 설계했습니다.

| 파일 | 설명 |
|---|---|
| `admin_handoff.html` | 대시보드, 검수 목록, 상세 검수, 공급 단위까지 P0/P1/P2 필드 배치를 확인하는 정적 프로토타입 |
| `AdminImportPage.jsx` | `/admin/import` 후보 수집/목록/선택 import/결과/force 모달 목업 |
| `AdminReviewUnitsSection.jsx` | `AdminReviewDetailPage`의 `units[]` 검수 섹션 목업 |
| `ADMIN_LH_IMPORT_HANDOFF.md` | 다음 프론트 작업자용 이식 메모 |

확인 URL: 로컬 서버 실행 후 `http://localhost:8000/ui_kits/web/admin_handoff.html`

## 주변시세 readiness / prepare 디자인 핸드오프

| 파일 | 설명 |
|---|---|
| `admin_market_readiness_handoff.html` | readiness 조회, prepare 실행, public/detail 상태 분기를 확인하는 정적 프로토타입 |
| `AdminMarketReadinessSection.jsx` | `AdminReviewDetailPage` 또는 별도 관리자 market 화면에 이식 가능한 React 목업 |
| `ADMIN_MARKET_READINESS_HANDOFF.md` | 프론트팀 전달용 API 연결/상태 매핑/결정사항 메모 |

확인 URL:
- repo root에서 서버 실행 시 `http://localhost:8000/ttait_Front/ui_kits/web/admin_market_readiness_handoff.html`
- `ttait_Front`에서 서버 실행 시 `http://localhost:8000/ui_kits/web/admin_market_readiness_handoff.html`
