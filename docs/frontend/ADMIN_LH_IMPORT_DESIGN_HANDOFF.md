# 관리자 LH 공고 수집 화면 디자인 핸드오프

> 대상: 디자인팀 / 프론트엔드 작업자  
> 기준 경로: `ttait_Front`, `app`  
> 목적: 기존 관리자 LH 수집 화면을 현행 백엔드 API 구조에 맞게 확장하기 위한 디자인 수정사항 정리

## 1. 작업 배경

현재 프론트의 `/admin/import` 화면은 기존 legacy import API만 호출하는 단순 실행 화면이다.

현행 백엔드는 LH 공고 수집 흐름이 아래처럼 바뀌었다.


### Backend contract summary

- Candidate collection stores candidates in `lh_import_candidate` only.
- Candidate collection/list APIs must not call Gemini.
- Selected import stores or updates only the `candidateIds` as formal `announcement` rows.
- During selected import, `dedupeStatus` and `fingerprint` decide whether Gemini is called or skipped.
- Successful import does not mean public exposure. Public APIs show only `announcement_eligibility.reviewStatus` = `APPROVED` or `CORRECTED`.
- `AdminReviewDetailPage.units[]` is admin review data based on `announcement_unit`.
- `rawText` and `sourceUnitKey` are admin-only fields and must not be exposed in public screens.

```text
기존: LH 전체 import 한 번 실행
현재: 후보 수집 -> 후보 목록 확인 -> 선택 import -> 검수 -> 승인
```

따라서 디자인 작업의 핵심은 “수집 실행 버튼 하나”를 “관리자가 후보를 보고 선택 처리하는 운영 도구”로 확장하는 것이다.

## 2. 참고해야 할 기존 디자인 방향

기존 디자인 시스템은 `ttait_Front` 기준을 따른다.

| 항목 | 기준 |
|---|---|
| 제품 톤 | 따뜻하고 접근성 좋은 공공주택 탐색 서비스 |
| 시각 방향 | Airbnb inspired, 흰 배경, 카드 중심, 강한 단일 red accent |
| 메인 컬러 | `#ff385c` |
| 텍스트 컬러 | `#222222`, 보조 `#6a6a6a` |
| 배경 | 기본 `#ffffff`, 관리자/마이페이지 계열은 `#f2f2f2` 사용 가능 |
| 폰트 | `Noto Sans KR` |
| 컴포넌트 언어 | pill, badge, card, table-like list, rounded panel |

관리자 화면은 사용자용 목록/상세보다 더 조밀해도 되지만, 기존 브랜드의 둥근 카드, 명확한 배지, 부드러운 그림자 톤은 유지한다.

## 3. 현재 프론트 상태

실제 프론트 앱은 `app` 폴더에 있다.

| 경로 | 현재 역할 |
|---|---|
| `app/src/pages/AdminImportPage.jsx` | LH 공고 수집 화면. 현재 legacy import 중심 |
| `app/src/pages/AdminReviewListPage.jsx` | AI 파싱 검수 목록 |
| `app/src/pages/AdminReviewDetailPage.jsx` | AI 파싱 검수 상세 |
| `app/src/pages/AdminDashboardPage.jsx` | 관리자 대시보드 |
| `app/src/styles/global.css` | 디자인 토큰 일부 반영 |

현재 `/admin/import`는 `page`, `size` 입력 후 `POST /api/admin/import/lh?page=&size=`를 호출한다. 새 후보 수집/목록/선택 import UI는 아직 화면에 반영되어 있지 않다.

## 4. 새 화면 목표

관리자가 아래 질문에 바로 답할 수 있어야 한다.

| 질문 | 화면에서 보여줘야 할 것 |
|---|---|
| 이번에 LH에서 몇 건을 가져왔나? | `fetched`, `scanned`, `skippedLand` 요약 카드 |
| 어떤 후보가 import 가능한가? | 후보 테이블의 `canParse`, `isLandNotice`, `pdfUrl` |
| 이미 들어온 공고인가? | `alreadyImported`, `dedupeStatus` 배지 |
| 재파싱이 필요한가? | `CHANGED_REPARSE`, `FAILED_RETRY`, `FORCE_REPARSE` 강조 |
| Gemini 비용이 발생하는가? | 선택 import / force reparse 버튼 주변 경고 |
| import 후 어디서 검수하나? | 검수 목록으로 이동 CTA |

## 5. 권장 IA

`/admin/import`를 하나의 운영 페이지로 유지하되, 내부를 3개 영역으로 나눈다.

```text
관리자 LH 공고 수집
├─ 상단 요약 / 안내
├─ Step 1. 후보 수집
├─ Step 2. 후보 목록 및 선택 import
└─ Step 3. 실행 결과 / 다음 액션
```

### 5.1 상단 요약

상단에는 이 화면이 legacy import가 아니라 새 후보 기반 import 흐름임을 설명한다.

권장 문구:

```text
LH 공고를 먼저 후보로 수집한 뒤, 관리자가 선택한 공고만 AI 파싱합니다.
후보 수집과 목록 조회 단계에서는 Gemini 호출이 발생하지 않습니다.
```

상단 오른쪽에는 보조 버튼을 둔다.

| 버튼 | 역할 |
|---|---|
| `검수 목록 보기` | `/admin/review?status=PENDING` 이동 |
| `관리자 대시보드` | `/admin` 이동 |

## 6. 화면 상세 설계

### 6.1 Step 1. 후보 수집 카드

목적은 LH 목록/상세 JSON과 PDF URL을 후보 테이블에 저장하는 것이다. 이 단계에서는 Gemini를 호출하지 않는다.

필드:

| UI 요소 | 기본값 | 설명 |
|---|---:|---|
| `page` number input | `1` | LH 목록 페이지 |
| `size` number input | `10` | 후보 수집 크기 |
| `후보 수집 실행` button | - | `POST /api/admin/import/lh/candidates/collect` 호출 |

결과 요약 카드:

| 값 | 의미 |
|---|---|
| `fetched` | LH 목록에서 받은 항목 수 |
| `scanned` | 후보 판단을 수행한 항목 수 |
| `skippedLand` | 토지 공고 제외 수 |

디자인 포인트:

- 성공 시 연한 green/blue 계열 요약 카드 사용
- `skippedLand`는 회색 또는 주황 배지로 표현
- “Gemini 호출 없음” 안내 배지를 카드 상단에 표시

### 6.2 Step 2. 후보 목록 테이블

후보 목록은 이 화면의 핵심이다.

필터:

| 필터 | 값 |
|---|---|
| 상태 | `전체`, `COLLECTED`, `IMPORTED`, `FAILED`, `SKIPPED` |
| 페이지 | `page=0` 기반 |
| 페이지 크기 | 기본 `20` |

테이블 컬럼 권장안:

| 컬럼 | 표시 방식 | 설명 |
|---|---|---|
| 선택 | checkbox | `canParse=false`면 disabled |
| 후보 ID | text | 선택 import에 쓰는 `id` |
| 공고명 | title + `panId` 보조 텍스트 | 긴 제목은 2줄 clamp |
| 지역 | text/badge | `region` |
| 후보 상태 | badge | `COLLECTED`, `IMPORTED`, `FAILED`, `SKIPPED` |
| 중복 판단 | badge | `dedupeStatus` |
| import 가능 | icon/text | `canParse` |
| 기존 저장 | icon/text | `alreadyImported` |
| PDF | link/icon | `pdfUrl` 있으면 외부 링크 |
| 원문 | link/icon | `sourceNoticeUrl` |

모바일 대응:

- 테이블을 그대로 줄이지 말고 candidate card list로 전환한다.
- 카드 상단: checkbox + 상태 배지
- 카드 본문: 공고명, 지역, `panId`
- 카드 하단: dedupe badge, PDF/원문 버튼

### 6.3 Step 2 액션 바

후보를 선택하면 하단 또는 테이블 상단에 sticky action bar를 보여준다.

표시 정보:

| 항목 | 예시 |
|---|---|
| 선택 수 | `3개 후보 선택됨` |
| 예상 작업 | `선택 import 실행` |
| force 옵션 | `강제 재파싱` checkbox |

버튼:

| 버튼 | 스타일 | 설명 |
|---|---|---|
| `선택 후보 import` | primary dark 또는 brand red | `POST /api/admin/import/lh/selected` |
| `선택 해제` | secondary | checkbox 초기화 |

`force=true` 체크 시 확인 모달을 반드시 둔다.

확인 모달 문구:

```text
강제 재파싱은 변경 없는 공고도 Gemini를 다시 호출할 수 있습니다.
정말 선택한 후보를 강제 재파싱할까요?
```

### 6.4 Step 3. 실행 결과 패널

선택 import 또는 force reparse 후 아래 카운터를 보여준다.

| 필드 | 디자인 표현 |
|---|---|
| `fetched` | 중립 |
| `scanned` | 중립 |
| `skippedLand` | warning |
| `unchanged` | info |
| `geminiSkipped` | success/info |
| `imported` | success |
| `reparsed` | warning/accent |
| `failed` | error |

`failed > 0`이면 결과 패널 최상단에 빨간 에러 영역을 둔다.

권장 문구:

```text
일부 후보 처리에 실패했습니다. 서버 로그에서 실패 panId와 Gemini quota 또는 외부 API 오류를 확인해주세요.
```

성공 후 CTA:

| CTA | 이동 |
|---|---|
| `검수 대기 목록 보기` | `/admin/review?status=PENDING` |
| `후보 목록 새로고침` | 현재 목록 reload |

## 7. 상태 배지 디자인 규칙

### 7.1 후보 상태

| 상태 | 라벨 | 색상 방향 |
|---|---|---|
| `COLLECTED` | 수집됨 | blue/info |
| `IMPORTED` | import 완료 | green/success |
| `FAILED` | 실패 | red/error |
| `SKIPPED` | 제외 | gray/neutral |

### 7.2 중복 판단 상태

| 상태 | 사용자 표시명 | 강조도 |
|---|---|---|
| `NEW` | 신규 | 높음 |
| `UNCHANGED_SKIP_GEMINI` | 변경 없음 | 낮음 |
| `CHANGED_REPARSE` | 변경 감지 | 높음 |
| `FAILED_RETRY` | 실패 재시도 | 높음 |
| `FORCE_REPARSE` | 강제 재파싱 | 높음 |
| `NO_PDF` | PDF 없음 | 중간 |
| `LAND_SKIP` | 토지 공고 제외 | 낮음 |

권장 색상:

| 의미 | 색상 |
|---|---|
| 신규/주요 액션 | `#ff385c` 또는 `#fff0f3` |
| 성공/import 완료 | green tint |
| 변경/재파싱 필요 | orange tint |
| 실패 | red tint |
| 변경 없음/skip | gray tint |

## 8. Admin Review Detail 수정사항

`AdminReviewDetailPage`도 unit 데이터 표시가 중요하다.

추가 또는 강화할 섹션:

```text
공급 단위 검수
```

이 섹션은 `units[]`를 표로 보여준다.

필수 노출 필드:

| 필드 | 표시명 |
|---|---|
| `unitOrder` | 순서 |
| `unitSource` | 출처 |
| `matchSource` | 매칭 근거 |
| `confidenceLevel` | 신뢰도 |
| `complexName` | 단지명 |
| `fullAddress` | 주소 |
| `supplyTypeRaw` / `supplyTypeNormalized` | 공급유형 |
| `houseTypeRaw` / `houseTypeNormalized` | 주택유형 |
| `exclusiveAreaText` | 전용면적 |
| `depositAmount` | 보증금 |
| `monthlyRentAmount` | 월세 |
| `salePriceMin` / `salePriceMax` | 분양가 |
| `supplyHouseholdCount` | 공급세대수 |
| `rawText` | 추출 원문 |
| `sourceUnitKey` | 원본 row key |

디자인 주의:

- `rawText`는 길기 때문에 기본은 접힘 처리한다.
- `confidenceLevel=LOW`는 관리자 눈에 띄게 표시한다.
- `unitSource=PDF_AI`는 AI 추출값임을 명확히 보여준다.
- `unitSource=MERGED`는 LH API와 PDF AI가 병합된 값이므로 가장 신뢰도 높은 형태처럼 보일 수 있다.

## 9. Public Detail 관련 주의

현행 기준에서 public detail은 `units[]`를 노출하지 않는다.

디자인팀이 사용자 상세 화면을 수정할 때 아래를 혼동하면 안 된다.

| 항목 | public 노출 여부 |
|---|---|
| `units[]` | 현재 기준 노출 안 함 |
| `rawText` | 노출 금지 |
| `sourceUnitKey` | 노출 금지 |
| `announcement` summary 값 | 노출 가능 |
| `announcement_detail` summary/detail 값 | 노출 가능 |

public 공고는 검수 상태가 `APPROVED` 또는 `CORRECTED`일 때만 조회된다. import 직후 `PENDING` 상태에서는 public detail이 404일 수 있다.

## 10. API 명세 요약

### 후보 수집

```http
POST /api/admin/import/lh/candidates/collect?page=1&size=10
Authorization: Bearer {{accessToken}}
```

응답 예시:

```json
{
  "fetched": 10,
  "scanned": 10,
  "skippedLand": 0,
  "candidates": [
    {
      "id": 1,
      "panId": "LN-0000000",
      "title": "공고명",
      "region": "서울특별시",
      "status": "COLLECTED",
      "sourceNoticeUrl": "https://apply.lh.or.kr/...",
      "pdfUrl": "https://apply.lh.or.kr/...",
      "isLandNotice": false,
      "alreadyImported": false,
      "canParse": true,
      "dedupeStatus": "NEW"
    }
  ]
}
```

### 후보 목록

```http
GET /api/admin/import/lh/candidates?page=0&size=20&status=COLLECTED
Authorization: Bearer {{accessToken}}
```

응답 예시:

```json
{
  "candidates": [],
  "totalCount": 0
}
```

### 선택 import

```http
POST /api/admin/import/lh/selected
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "candidateIds": [1, 2],
  "force": false
}
```

응답 예시:

```json
{
  "fetched": 2,
  "scanned": 2,
  "skippedLand": 0,
  "unchanged": 0,
  "geminiSkipped": 0,
  "imported": 2,
  "reparsed": 0,
  "failed": 0
}
```

### 단건 강제 재파싱

```http
POST /api/admin/import/lh/{{announcementId}}/force-reparse
Authorization: Bearer {{accessToken}}
```

## 11. 프론트 작업 범위

### 1차 필수

| 작업 | 설명 |
|---|---|
| `/admin/import` IA 재구성 | 후보 수집, 후보 목록, 선택 import 결과까지 한 화면에서 처리 |
| 후보 목록 테이블 | 상태 필터, checkbox, dedupe badge, 링크 버튼 포함 |
| 선택 import action bar | 선택 수, force 옵션, 실행 버튼 |
| 결과 카운터 패널 | import 결과를 성공/경고/실패로 시각화 |
| 검수 목록 CTA | import 후 `/admin/review?status=PENDING` 이동 |

### 2차 권장

| 작업 | 설명 |
|---|---|
| Admin Review Detail unit table 강화 | `units[]` 검수 UX 개선 |
| force reparse confirm modal | 비용 발생 액션 보호 |
| 후보 목록 모바일 카드뷰 | 작은 화면에서 테이블 대체 |
| empty/loading/error 상태 | 운영 도구 신뢰감 확보 |

## 12. 디자인팀 체크리스트

- [ ] 기존 `ttait_Front`의 red accent, rounded card, Noto Sans KR 톤 유지
- [ ] 후보 수집과 선택 import를 시각적으로 분리
- [ ] “Gemini 호출 없음” 단계와 “Gemini 호출 가능” 단계를 명확히 구분
- [ ] `dedupeStatus` 배지 variant 정의
- [ ] `status` 배지 variant 정의
- [ ] `canParse=false` 후보의 disabled 상태 정의
- [ ] `force=true` 확인 모달 디자인
- [ ] import 결과 카운터 카드 디자인
- [ ] Admin detail의 unit table / rawText expand 패턴 정의
- [ ] public detail에는 unit/raw 필드가 없다는 점 명시

## 13. 꼭 피해야 할 것

- 후보 수집 버튼을 누르자마자 Gemini 파싱이 되는 것처럼 표현하지 않는다.
- `imported=1`을 “검수 완료”로 표현하지 않는다. import 후에도 admin review 승인 단계가 필요하다.
- `unchanged`를 실패처럼 보이게 하지 않는다. 변경 없음으로 Gemini를 아낀 정상 결과다.
- `rawText`, `sourceUnitKey`를 public 사용자 화면에 노출하는 디자인을 만들지 않는다.
- `force=true`를 기본값처럼 보이게 하지 않는다.
- 공급세대수는 과거 검증에서 오인 가능성이 있었으므로 관리자 검수 대상 값처럼 보여준다.

## 14. 디자인 산출물 요청안

디자인팀에는 아래 산출물을 요청하면 된다.

| 산출물 | 내용 |
|---|---|
| `/admin/import` desktop mockup | 후보 수집, 후보 목록, action bar, 결과 패널 포함 |
| `/admin/import` mobile mockup | 테이블 대신 카드 리스트 패턴 |
| badge variant sheet | `status`, `dedupeStatus`, `confidenceLevel`, `unitSource` |
| force reparse modal | 강제 재파싱 경고/확인 모달 |
| Admin Review Detail unit section | unit table, rawText expand, low confidence 표시 |

## 15. 한 줄 요약

이번 디자인 수정의 핵심은 `/admin/import`를 단순 수집 실행 페이지에서 “LH 후보를 선별하고, Gemini 비용을 통제하며, 검수로 넘기는 관리자 운영 콘솔”로 바꾸는 것이다.
