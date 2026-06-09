# 프론트엔드 핸드오프: 내 조건 충족도 체크리스트 API

## 1. API 개요

| 항목 | 내용 |
|---|---|
| 목적 | 현재 로그인 사용자의 저장된 프로필과 public-visible 공고의 파싱된 자격 조건을 비교해 체크리스트를 반환 |
| Method | `GET` |
| Path | `/api/announcements/{announcementId}/eligibility-checklist` |
| 인증 | 필요. 기존 JWT 인증을 사용해야 하며, 백엔드는 controller에서 `CurrentUser.id()`로 강제 확인 |
| Path variable | `announcementId`: 공고 ID |
| Response header | `Cache-Control: private, no-store` |
| 프론트 위치 제안 | 공고 상세 화면의 “내 조건 충족도” 섹션 또는 CTA 주변 |

> 주의: `GET /api/announcements/**` 보안 설정은 public permit일 수 있지만, 이 endpoint는 백엔드 controller 내부에서 로그인 사용자를 직접 요구한다. 프론트는 반드시 로그인 상태에서 호출해야 한다.

## 2. 응답 DTO contract

```ts
type EligibilityChecklistResponse = {
  announcementId: number;
  summaryStatus: EligibilitySummaryStatus;
  summaryMessage: string;
  metCount: number;
  notMetCount: number;
  needsVerificationCount: number;
  notApplicableCount: number;
  items: EligibilityChecklistItemResponse[];
  disclaimer: string;
};

type EligibilityChecklistItemResponse = {
  key: EligibilityChecklistItemKey;
  group: string;
  label: string;
  status: EligibilityCheckStatus;
  severity: string;
  reason: string;
  userValue: string;
  announcementCondition: string;
  actionLabel: string | null;
  actionTarget: string;
};
```

### 예시 응답

```json
{
  "announcementId": 1,
  "summaryStatus": "REVIEW_REQUIRED",
  "summaryMessage": "대체로 확인 가능하지만 공고 원문 또는 프로필 보완 확인이 필요합니다.",
  "metCount": 5,
  "notMetCount": 0,
  "needsVerificationCount": 2,
  "notApplicableCount": 4,
  "items": [
    {
      "key": "AGE",
      "group": "기본 자격",
      "label": "나이",
      "status": "MET",
      "severity": "INFO",
      "reason": "저장된 나이가 파싱된 나이 조건 범위에 포함됩니다.",
      "userValue": "30세",
      "announcementCondition": "19세 ~ 39세",
      "actionLabel": null,
      "actionTarget": "NONE"
    }
  ],
  "disclaimer": "이 결과는 저장된 프로필과 파싱된 공고 정보를 기준으로 한 참고용 체크입니다. 최종 신청 가능 여부는 공고 원문에서 확인해야 합니다."
}
```

## 3. Enum 값

### `EligibilityCheckStatus`

| 값 | UI 의미 |
|---|---|
| `MET` | 충족 |
| `NOT_MET` | 미충족, 신청 가능성 blocker |
| `NEEDS_VERIFICATION` | 원문/프로필 추가 확인 필요 |
| `NOT_APPLICABLE` | 해당 조건 없음 또는 MVP 판단 대상 아님 |

### `EligibilitySummaryStatus`

| 값 | UI 의미 |
|---|---|
| `LIKELY_READY` | 저장 정보 기준 주요 조건 충족 가능성이 높음 |
| `REVIEW_REQUIRED` | blocker는 없지만 원문 또는 프로필 확인 필요 |
| `HAS_BLOCKERS` | 하나 이상의 `NOT_MET` 조건 존재 |
| `INSUFFICIENT_DATA` | 파싱된 자격 조건이 없거나 판단 데이터 부족 |

## 4. Item key 목록

프론트는 `key` 기준으로 아이콘, 정렬, 상세 설명 UI를 매핑할 수 있다. 현재 백엔드 MVP는 아래 key를 항상 같은 순서로 반환한다.

| key | label 예시 | group 예시 |
|---|---|---|
| `AGE` | 나이 | 기본 자격 |
| `MARITAL` | 혼인 상태 | 가구 조건 |
| `NEWLYWED` | 신혼부부 | 가구 조건 |
| `CHILDREN` | 자녀 수 | 가구 조건 |
| `HOMELESS` | 무주택 | 주택 보유 |
| `LOW_INCOME` | 저소득/수급 | 소득·자산 |
| `ELDERLY` | 고령자 | 기본 자격 |
| `DEPOSIT_BUDGET` | 보증금 예산 | 비용 |
| `MONTHLY_RENT_BUDGET` | 월세 예산 | 비용 |
| `APPLICATION_PERIOD` | 신청 기간 | 일정 |
| `INCOME_ASSETS` | 소득·자산 상세 기준 | 소득·자산 |

## 5. Error contract

기존 `GlobalExceptionHandler` 형식을 따른다.

```ts
type ApiError = {
  timestamp: string;
  message: string;
};
```

| HTTP status | message | 프론트 처리 제안 |
|---|---|---|
| `401 Unauthorized` | `authentication required` | 로그인 유도. 공고 상세는 유지하되 체크리스트 영역에 로그인 CTA 표시 |
| `400 Bad Request` | `profile setup required` | 프로필 설정/수정 화면으로 이동할 수 있는 CTA 표시 |
| `404 Not Found` | `announcement not found` | public-visible 공고가 아니거나 없는 공고. 체크리스트 영역에 “공고 정보를 확인할 수 없음” 표시 |
| `500 Internal Server Error` | `서버 내부 오류: ...` | 일반 오류 토스트 또는 재시도 버튼 표시 |

## 6. 프론트 구현 가이드

1. 공고 상세 화면 진입 후 로그인 상태라면 `GET /api/announcements/{announcementId}/eligibility-checklist`를 호출한다.
2. 비로그인 상태에서는 호출하지 않거나, 호출 결과 `401`을 받아 로그인 CTA로 처리한다.
3. `summaryStatus`를 상단 badge/alert로 표시한다.
   - `LIKELY_READY`: 긍정 색상
   - `REVIEW_REQUIRED`: 경고/주의 색상
   - `HAS_BLOCKERS`: 위험 색상
   - `INSUFFICIENT_DATA`: 중립 색상
4. `items`는 `group`별로 묶거나 현재 반환 순서를 그대로 사용한다.
5. 각 item은 `status`, `label`, `reason`, `userValue`, `announcementCondition`, `actionLabel`을 함께 보여준다.
6. `disclaimer`는 체크리스트 하단에 항상 표시한다. 이 기능은 최종 신청 가능 여부 판정이 아니라 참고용 체크다.
7. `NEEDS_VERIFICATION`은 실패가 아니라 “원문/프로필 확인 필요”로 표현한다.
8. `INCOME_ASSETS`는 raw 기준 문구를 내려주지 않으므로, 프론트에서 원문 링크 또는 공고 상세의 원문 URL CTA를 함께 제공하는 것이 좋다.

## 7. 데이터 노출 및 보안 notes

- 백엔드는 `eligibilityRaw`, `specialSupplyRaw`, `incomeAssetCriteriaRaw` 원문 텍스트를 공개 DTO에 포함하지 않는다.
- `INCOME_ASSETS` 항목은 raw text가 있으면 `NEEDS_VERIFICATION`만 반환하고, 원문 기준 문구 자체는 복사하지 않는다.
- 이 endpoint는 현재 사용자 프로필을 사용하므로 backend가 `Cache-Control: private, no-store`를 내려 공용 CDN/proxy/browser cache 저장을 방지한다.
- 응답에는 체크 판단에 필요한 요약 값(`30세`, `5000만원` 등)이 포함될 수 있다. 소득·자산 원문 기준과 민감한 raw 파싱 텍스트는 포함하지 않는다.
