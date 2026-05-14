# [현행] 관리자 LH 공고 수집 API 가이드

> 작성일: 2026-05-14  
> 대상: `subscrition` 관리자 API  
> 목적: 현재 관리자용 LH 공고 수집 API를 Apidog 또는 테스트 코드 기준으로 확인한다.

## 1. 관리자 LH 수집 API 구성

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/admin/import/lh?page=&size=` | 기존 legacy/full import |
| `POST` | `/api/admin/import/lh/candidates/collect?page=&size=` | 후보 수집. Gemini 호출 없음 |
| `GET` | `/api/admin/import/lh/candidates?page=&size=&status=` | 후보 목록 조회 |
| `POST` | `/api/admin/import/lh/selected` | 선택 후보 import |
| `POST` | `/api/admin/import/lh/{announcementId}/force-reparse` | 단건 강제 재파싱 |

모든 경로는 `/api/admin/**` 아래에 있으므로 기존 admin security 정책을 따른다.

## 2. legacy/full import

```http
POST /api/admin/import/lh?page=1&size=3
Authorization: Bearer {{accessToken}}
```

응답은 기존 호환을 위해 내부 metadata를 노출하지 않는다.

```json
{
  "imported": 1,
  "failed": 0
}
```

## 3. 후보 수집

```http
POST /api/admin/import/lh/candidates/collect?page=1&size=10
Authorization: Bearer {{accessToken}}
```

이 API는 LH list/detail JSON, PDF URL, dedupe 상태를 `lh_import_candidate`에 저장한다. 이 단계에서는 Gemini를 호출하지 않는다.

주요 응답 필드:

| 필드 | 설명 |
|---|---|
| `fetched` | LH 목록에서 받은 항목 수 |
| `scanned` | 후보 판단을 수행한 항목 수 |
| `skippedLand` | 토지 공고 제외 수 |
| `candidates[].id` | 선택 import에 사용할 candidate id |
| `candidates[].panId` | LH `PAN_ID` |
| `candidates[].alreadyImported` | 기존 정식 공고 연결 여부 |
| `candidates[].canParse` | 선택 import 가능 여부 |
| `candidates[].dedupeStatus` | `NEW`, `UNCHANGED_SKIP_GEMINI`, `CHANGED_REPARSE` 등 |

## 4. 후보 목록 조회

```http
GET /api/admin/import/lh/candidates?page=0&size=20&status=COLLECTED
Authorization: Bearer {{accessToken}}
```

`status`는 생략 가능하며, 사용할 수 있는 값은 `COLLECTED`, `IMPORTED`, `FAILED`, `SKIPPED`다.

## 5. 선택 후보 import

```http
POST /api/admin/import/lh/selected
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "candidateIds": [1, 2],
  "force": false
}
```

`force`를 생략하면 `false`로 처리한다. 요청한 candidate id만 import 대상이 된다.

응답 카운터:

| 필드 | 설명 |
|---|---|
| `fetched` | 요청한 candidate id 수 |
| `scanned` | 처리 대상 후보 수 |
| `skippedLand` | 토지 공고 skip 수 |
| `unchanged` | 기존 fingerprint와 같아 저장/Gemini를 건너뛴 수 |
| `geminiSkipped` | Gemini 호출을 하지 않은 수 |
| `imported` | 저장 또는 갱신 완료 수 |
| `reparsed` | 변경/실패재시도/강제재파싱으로 다시 파싱한 수 |
| `failed` | 실패 수 |

## 6. 단건 강제 재파싱

```http
POST /api/admin/import/lh/{{announcementId}}/force-reparse
Authorization: Bearer {{accessToken}}
```

기존 LH 공고 하나를 LH 상세/PDF/Gemini 경로로 다시 보낸다. Gemini quota를 사용할 수 있으므로 필요한 경우에만 호출한다.

## 7. 검증 기준

- 후보 수집과 후보 목록 조회만 호출했을 때는 bootRun 로그에 Gemini parse 로그가 없어야 한다.
- 선택 import 또는 force reparse는 Gemini 호출이 발생할 수 있다.
- legacy import는 계속 사용할 수 있지만 반복 테스트 시 dedupe로 변경 없는 공고의 Gemini 호출이 skip될 수 있다.
- live 테스트는 `size=3`처럼 작은 값부터 진행한다.
