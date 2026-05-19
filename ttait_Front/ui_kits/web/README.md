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

## 관리자 LH 후보 import 핸드오프

프론트 연동 전 관리자 화면 디자인 목업이 추가되었습니다.

| 파일 | 설명 |
|---|---|
| `admin_handoff.html` | LH 후보 import 콘솔 + 검수 상세 units 섹션 확인용 진입점 |
| `AdminImportPage.jsx` | `/admin/import` 후보 수집/목록/선택 import/결과/force 모달 목업 |
| `AdminReviewUnitsSection.jsx` | `AdminReviewDetailPage`의 `units[]` 검수 섹션 목업 |
| `ADMIN_LH_IMPORT_HANDOFF.md` | 다음 프론트 작업자용 이식 메모 |

확인 URL: 로컬 서버 실행 후 `http://localhost:8000/ui_kits/web/admin_handoff.html`
