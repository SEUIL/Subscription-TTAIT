# 주변시세 readiness / prepare UI 디자인 핸드오프

## 산출물

| 파일 | 용도 |
|---|---|
| `admin_market_readiness_handoff.html` | 관리자 readiness 조회, prepare 실행, public/detail 상태 분기를 확인하는 정적 프로토타입 |
| `AdminMarketReadinessSection.jsx` | 정적 프로토타입용 React 목업 컴포넌트. `AdminReviewDetailPage` 등에 이식하려면 모듈화 필요 |

확인 URL:
- repo root(`C:\Users\kjm90\Desktop\subscrition`)에서 서버 실행 시: `http://localhost:8000/ttait_Front/ui_kits/web/admin_market_readiness_handoff.html`
- `ttait_Front` 폴더에서 서버 실행 시: `http://localhost:8000/ui_kits/web/admin_market_readiness_handoff.html`

> 주의: `AdminMarketReadinessSection.jsx`는 `import React`, `export default`가 없는 static prototype용 파일입니다. `Object.assign(window, ...)` 방식으로 HTML에서 불러오므로 실제 프론트 앱에 그대로 import할 수 없습니다.

---

## 디자인 원칙

1. **admin readiness와 public comparison 상태명을 분리합니다.**
   - admin readiness: `marketReady=true` 또는 `blocker=READY`
   - public comparison: `status=COMPARABLE`
   - 일반 상세 화면에서는 `READY`라는 backend blocker명을 직접 노출하지 않습니다.
2. **프론트는 `marketReady`/`status`를 1차 기준으로 UI를 분기합니다.**
   - admin: `marketReady=true`이면 준비 완료로 표시합니다.
   - public: `status=COMPARABLE`이면 비교 카드/CTA를 활성화합니다.
   - blocked 상태는 `blocker`와 `snapshotStatus`로 이유를 표시합니다.
3. **prepare는 관리자 액션입니다.**
   - 일반 사용자 상세 화면에서는 시세 데이터 준비 버튼을 노출하지 않고, 비활성/안내 카드만 보여줍니다.
   - 관리자 화면에서만 `lawdCd`와 `exclusiveAreaValue`가 있는 prepare 후보에 CTA를 노출합니다.
4. **기존 청약가/임대료는 계속 보여주고, 시세비교만 disabled 처리합니다.**
   - `INSUFFICIENT_DATA`, `UNIT_LAWD_CD_MISSING`, `UNIT_AREA_MISSING` 모두 금액 정보 자체를 숨기지 않습니다.
5. **동기 prepare API는 중복 클릭 방지가 필수입니다.**
   - 버튼 loading/disabled, “데이터 수집 및 집계 중” 문구, 완료 후 readiness 재조회까지 한 플로우로 설계했습니다.

---

## UI 상태 매핑

| Backend state | Surface | Badge | 사용자/관리자 문구 | CTA |
|---|---|---|---|---|
| `marketReady=true` 또는 `blocker=READY` | admin readiness | 준비 완료 | 주변 거래 snapshot이 준비됐습니다. | public COMPARABLE 확인 |
| `status=COMPARABLE` | public comparison | 준비 완료 | 주변 거래 기준을 생성했어요. | 시세비교 보기 |
| `UNIT_LAWD_CD_MISSING` | admin/public | 정보 부족 | 주소 정규화가 필요합니다. | 없음 / 주소 보완 |
| `UNIT_AREA_MISSING` | admin/public | 정보 부족 | 전용면적 정보가 필요합니다. | 없음 / 면적 보완 |
| `SNAPSHOT_NOT_FOUND` | admin | 데이터 없음 | 최근 6개월 거래 데이터를 수집해 비교 기준을 생성합니다. | 시세 데이터 준비 |
| `SNAPSHOT_NOT_FOUND` | public | 데이터 없음 | 시세 데이터를 준비 중입니다. | 시세비교 disabled |
| `INSUFFICIENT_DATA` | admin/public | 표본 부족 | 주변 거래 표본이 부족해 신뢰도 있는 비교가 어렵습니다. | 시세비교 disabled |
| `rtmsServiceKeyConfigured=false` | admin/debug | 설정 필요 | RTMS 서비스키 미설정 | alert + prepare disabled |

### addressStatus 실제 값

목업 sample은 실제 backend enum에 맞춰 아래 값만 사용합니다.

- `NOT_REQUESTED`
- `SUCCESS`
- `NO_ADDRESS`
- `NO_LAWD_CODE`

사용하지 않는 값: `NORMALIZED`, `FAILED`

### readiness unit 필드 누락 없이 반영

`MarketReadinessResponse.units[]`에는 `recommendedSourceType`이 포함됩니다. 관리자 화면에서는 `sourceType` 조회 조건과 함께 unit별 추천 sourceType을 작게 표시합니다.

---

## 권장 화면 배치

### 관리자 검수 상세 (`AdminReviewDetailPage`)

- `ReviewUnitsSection` 바로 아래 또는 우측 관리자 패널에 “주변시세 준비 상태” 섹션 추가
- 상단: 조회 조건(`sourceType`, `dealYmFrom~dealYmTo`) + KPI(`readyUnitCount`, `blockedUnitCount`)
- 본문: unit별 readiness 테이블
- `lawdCd`와 `exclusiveAreaValue`가 있는 unit은 prepare 후보가 될 수 있습니다.
- `SNAPSHOT_NOT_FOUND`는 기본 prepare 대상입니다.
- `INSUFFICIENT_DATA` 재실행은 정책 확정 필요: 기본 디자인은 “재수집 정책 확인” disabled로 두고, 허용 정책이면 “표본 재수집” CTA로 전환합니다.
- RTMS key가 설정되어 있고 prepare 후보가 있으면 상단/행 CTA에 prepare 버튼 노출
- prepare 완료 후 toast → readiness GET 재호출

### 일반 공고 상세 (`AnnouncementDetailPage`)

- 기존 `MarketComparisonSection` 앞단에서 readiness/embedded 상태를 기준으로 카드 상태 변경
- `marketReady=false`인 unit은 **숨기지 말고 비활성 카드**로 노출 권장
- `INSUFFICIENT_DATA`일 때 청약가/임대료는 유지하고 시세비교 버튼만 disabled

---

## API 연결 메모

### readiness 조회

```http
GET /api/admin/market/announcements/{announcementId}/readiness?sourceType=APT_RENT&dealYmFrom=202512&dealYmTo=202605
```

- 기본 `sourceType`: `APT_RENT`
- 기본 기간 권장안: **최근 완료 6개월**. 2026-06-02 기준 예시는 `202512~202605`입니다.
- 테스트/시연 환경에서만 백엔드 예시값 `202401~202406` 고정 사용 여부를 프론트/백엔드가 결정해야 합니다.

### prepare 실행

```http
POST /api/admin/market/announcements/{announcementId}/prepare
Content-Type: application/json

{
  "sourceType": "APT_RENT",
  "dealYm": "202605",
  "dealYmFrom": "202512",
  "dealYmTo": "202605",
  "numOfRows": 100,
  "maxPages": 10,
  "minimumSampleCount": 3,
  "retryNoLawdCode": true
}
```

- 버튼 클릭 즉시 loading/disabled
- 성공/부분성공/대상없음 toast
- 완료 후 readiness 재조회
- 범위 제한: dealYm 최대 12개월, `numOfRows` 1~100, `maxPages` 1~10, batch 최대 20개

---

## 프론트팀에 추가로 넘길 결정사항

1. **기본 조회 기간**: 권장안은 최근 완료 6개월(`202512~202605` 예시). 고정 테스트값(`202401~202406`) 사용 여부 결정 필요.
2. **prepare 버튼 노출 범위**: 디자인은 admin 전용으로 가정했습니다. 일반 사용자는 준비/부족 상태만 안내.
3. **unit 노출 방식**: `marketReady=false` unit도 숨기지 않고 비활성 카드로 노출 권장.
4. **시세비교 disabled 범위**: `INSUFFICIENT_DATA`에서도 기존 청약가/임대료는 보여주고 시세비교만 disabled 권장.
5. **RTMS key 미설정 처리**: `rtmsServiceKeyConfigured=false`이면 관리자 alert 노출, prepare CTA disabled.
6. **주소/면적 보완 액션**: 주소 정규화 재시도 API/관리자 편집 화면 연결 여부는 백엔드/프론트 확인 필요.
7. **public API readiness 포함 여부**: 일반 상세에서 admin readiness API를 직접 호출하지 않을지, public detail 응답에 요약 상태를 내려줄지 결정 필요.
8. **INSUFFICIENT_DATA 재실행 정책**: 표본 부족 상태에서 prepare 재실행을 허용할지, 별도 기간 확장/수동 배치로만 처리할지 확정 필요.
9. **프론트 이식 방식**: 현재 JSX는 static prototype용입니다. 앱 이식 시 `export default`, props 타입, API hook, toast, loading/result state를 모듈화해야 합니다.
