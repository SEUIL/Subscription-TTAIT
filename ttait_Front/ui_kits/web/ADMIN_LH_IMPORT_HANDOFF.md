# 관리자 LH 후보 import UI 핸드오프

이 산출물은 `C:\Users\kjm90\Desktop\subscrition\docs\frontend\ADMIN_LH_IMPORT_DESIGN_HANDOFF.md` 내용을 기반으로, 현재 디자인 시스템(`ttait_Front`)의 색상/타입/카드/배지 패턴을 사용해 만든 **프론트 연동 전 디자인 목업**입니다.

## 확인 방법

프로젝트 루트에서 로컬 서버를 실행한 뒤 확인합니다.

```bash
python -m http.server 8000
```

- 관리자 import 목업: `http://localhost:8000/ui_kits/web/admin_handoff.html`

> 기존 UI kit과 동일하게 React/Babel CDN을 사용하는 목업이므로 `file://` 직접 실행은 권장하지 않습니다.

## 추가/수정 파일

| 파일 | 역할 |
|---|---|
| `ui_kits/web/admin_handoff.html` | 탭으로 `/admin/import` 목업과 검수 상세 units 섹션을 확인하는 진입점 |
| `ui_kits/web/AdminImportPage.jsx` | LH 후보 수집, 후보 목록, 선택 import, 결과 패널, force reparse 확인 모달 목업 |
| `ui_kits/web/AdminReviewUnitsSection.jsx` | `AdminReviewDetailPage`에 이식할 `units[]` 검수 섹션 목업 |
| `ui_kits/web/ADMIN_LH_IMPORT_HANDOFF.md` | 프론트 작업자용 구현 메모 |

## 디자인 적용 기준

- 브랜드 컬러: `#ff385c`
- 본문/버튼 다크: `#222222`
- 보조 텍스트: `#6a6a6a`
- 관리자 계열 배경: `#f2f2f2`
- 폰트: `Noto Sans KR`
- 카드: 20px radius + 기존 3-layer shadow
- 배지: pill 형태, status/dedupe/confidence/source별 semantic tint

## Backend integration contract

- Collect candidates: save to `lh_import_candidate`; no Gemini call.
- List candidates: read/filter saved candidates; no Gemini call.
- Selected import: save/update only selected `candidateIds` as `announcement`.
- Gemini decision: based on `dedupeStatus` and `fingerprint`; `UNCHANGED_SKIP_GEMINI` and `geminiSkipped` are normal cost-saving results.
- Public exposure: only after `announcement_eligibility.reviewStatus` becomes `APPROVED` or `CORRECTED`.
- Units: `AdminReviewDetailPage.units[]` is based on `announcement_unit`; never expose `rawText` or `sourceUnitKey` publicly.

## `/admin/import` 이식 메모

### 화면 IA

1. 상단 안내/CTA
   - 후보 기반 import 흐름 설명
   - `검수 대기 목록 보기` CTA (`/admin/review?status=PENDING`)
   - `관리자 대시보드` CTA (`/admin`)
2. Step 1 후보 수집
   - `page`, `size` 입력
   - `후보 수집 실행` 버튼
   - `fetched`, `scanned`, `skippedLand` 요약 카드
   - Gemini 호출 없음 안내 배지
3. Step 2 후보 목록
   - status filter, page, size
   - checkbox, candidate id, title/panId, region, status, dedupeStatus, canParse, alreadyImported, PDF/원문 링크
   - 모바일에서는 테이블 대신 카드 목록
4. sticky action bar
   - 선택 개수
   - `강제 재파싱` checkbox
   - `선택 후보 import`, `선택 해제`
5. Step 3 결과
   - fetched/scanned/skippedLand/unchanged/geminiSkipped/imported/reparsed/failed
   - failed > 0이면 오류 callout
   - 검수 대기 목록 CTA

### 연동 예정 API

```http
POST /api/admin/import/lh/candidates/collect?page=1&size=10
GET  /api/admin/import/lh/candidates?page=0&size=20&status=COLLECTED
POST /api/admin/import/lh/selected
```

`POST /api/admin/import/lh/selected` body 예시:

```json
{
  "candidateIds": [12041, 12039],
  "force": false
}
```

## 배지 variant

### 후보 status

| 값 | 표시 | tone |
|---|---|---|
| `COLLECTED` | 수집됨 | info/blue |
| `IMPORTED` | import 완료 | success/green |
| `FAILED` | 실패 | danger/red |
| `SKIPPED` | 제외 | neutral/gray |

### dedupeStatus

| 값 | 표시 | tone |
|---|---|---|
| `NEW` | 신규 | brand/red tint |
| `UNCHANGED_SKIP_GEMINI` | 변경 없음 | neutral/gray |
| `CHANGED_REPARSE` | 변경 감지 | warning/orange |
| `FAILED_RETRY` | 실패 재시도 | danger/red |
| `FORCE_REPARSE` | 강제 재파싱 | warning/orange |
| `NO_PDF` | PDF 없음 | muted warning |
| `LAND_SKIP` | 토지 제외 | neutral/gray |

## force reparse UX

- `force=true`는 기본값으로 강조하지 않습니다.
- 체크 후 import 실행 시 반드시 확인 모달을 띄웁니다.
- 모달 문구는 비용 발생 가능성을 먼저 안내합니다.

## `AdminReviewDetailPage` units 섹션 이식 메모

`AdminReviewUnitsSection.jsx`는 다음 필드를 노출하는 관리자 전용 섹션입니다.

- `unitOrder`
- `unitSource`
- `matchSource`
- `confidenceLevel`
- `complexName`
- `fullAddress`
- `supplyTypeRaw` / `supplyTypeNormalized`
- `houseTypeRaw` / `houseTypeNormalized`
- `exclusiveAreaText`
- `depositAmount`
- `monthlyRentAmount`
- `salePriceMin` / `salePriceMax`
- `supplyHouseholdCount`
- `rawText`
- `sourceUnitKey`

주의:

- `rawText`, `sourceUnitKey`는 public 화면에 노출하지 않습니다.
- `confidenceLevel=LOW`는 관리자에게 원문 대조가 필요하도록 orange warning으로 표시합니다.
- `unitSource=PDF_AI`는 AI 추출값임을 분명히 표시합니다.
- `unitSource=MERGED`는 LH API + PDF AI 병합값으로 success tone을 사용합니다.
