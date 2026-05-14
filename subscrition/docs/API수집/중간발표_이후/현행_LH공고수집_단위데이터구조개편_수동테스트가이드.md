# [현행] LH 공고 수집 단위 데이터 구조 개편 Apidog 테스트 가이드

> 대상: `subscrition` 정식 백엔드  
> 목적: Apidog로 LH 공고 수집, Gemini PDF 파싱, `announcement_unit` 저장, admin API 단위 노출, public detail 조회, DB row count 일치 여부를 직접 확인한다.  
> 기준 모델: `gemini-2.5-flash-lite`

## 1. 사전 확인

`subscrition/src/main/resources/application.properties`에서 Gemini 모델이 아래 값인지 확인한다.

```properties
gemini.model=gemini-2.5-flash-lite
```

Gemini RPD 제한을 고려해 import 호출은 반드시 `size=3`으로 실행한다. `size`를 키우면 Gemini 호출량이 늘어날 수 있다.

## 2. 서버 실행

API 요청은 Apidog에서 실행하되, 백엔드는 로컬에서 먼저 켜야 한다.

```powershell
cd subscrition
.\gradlew bootRun
```

정상 기준:

```text
Started SubscriptionApplication
port 8080 LISTENING
```

## 3. Apidog 환경 변수 준비

Apidog에서 로컬 테스트용 Environment를 하나 만들고 아래 변수를 등록한다.

| 변수명 | 값 | 설명 |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | 로컬 백엔드 주소 |
| `accessToken` | 비워둠 | 로그인 후 발급받은 JWT |
| `announcementId` | 비워둠 | admin detail/public detail/approve에 사용할 공고 ID |

요청 URL은 `{{baseUrl}}/...` 형식으로 작성한다. admin API 요청은 Auth 탭에서 Bearer Token을 선택하고 token 값에 `{{accessToken}}`을 넣는다.

## 4. 요청 1: 관리자 로그인

| 항목 | 값 |
|---|---|
| Method | `POST` |
| URL | `{{baseUrl}}/api/auth/login` |
| Headers | `Content-Type: application/json` |
| Body | JSON |

```json
{
  "loginId": "admin",
  "password": "<관리자_비밀번호>"
}
```

정상 기준:

- HTTP 200
- `loginId = admin`
- `role = ADMIN`
- `accessToken` 존재

로그인 성공 후 응답의 `accessToken` 값을 Apidog 환경 변수 `accessToken`에 저장한다. 자동 추출을 쓰지 않는다면 응답에서 토큰만 복사해 변수에 붙여 넣어도 된다.

## 5. 요청 2: LH import 실행, 레거시 전체 수집

기존 수동 수집 API는 유지된다. 이 API는 LH 목록 페이지를 바로 읽고 상세 조회, PDF 탐지, Gemini 파싱, 저장까지 한 번에 실행하는 레거시 전체 import 경로다. 새 후보 API가 추가됐지만 이 엔드포인트가 제거된 것은 아니다.

| 항목 | 값 |
|---|---|
| Method | `POST` |
| URL | `{{baseUrl}}/api/admin/import/lh?page=1&size=3` |
| Auth | Bearer Token: `{{accessToken}}` |
| Query | `page`: LH 목록 페이지, 기본값 `1` |
| Query | `size`: LH 목록 크기, 기본값 `10` |

정상 기대값:

```json
{
  "imported": 1,
  "failed": 0
}
```

이 응답은 호환성을 위해 `imported`, `failed`만 노출한다. 내부에서는 중복 판단으로 변경 없는 공고의 Gemini 호출을 건너뛸 수 있지만, 레거시 응답 body에는 `unchanged`, `geminiSkipped`, `reparsed`가 나오지 않는다.

`failed > 0`이면 bootRun 로그에서 실패 `panId`, 예외 메시지, Gemini quota 또는 외부 API 오류 여부를 먼저 확인한다.

## 6. 요청 3: 후보 공고 수집

후보 수집 API는 LH 목록과 상세 JSON, PDF URL, 중복 판단 메타데이터를 `lh_import_candidate`에 저장한다. 이 단계는 Gemini를 호출하지 않는다. 실제 PDF 파싱은 선택 import 또는 강제 재파싱에서만 실행된다.

| 항목 | 값 |
|---|---|
| Method | `POST` |
| URL | `{{baseUrl}}/api/admin/import/lh/candidates/collect?page=1&size=10` |
| Auth | Bearer Token: `{{accessToken}}` |
| Query | `page`: LH 목록 페이지, 기본값 `1` |
| Query | `size`: LH 목록 크기, 기본값 `10` |

정상 기대값:

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

응답 필드 의미:

| 필드 | 의미 |
|---|---|
| `fetched` | LH 목록 응답에서 받은 항목 수 |
| `scanned` | 후보 판단을 시도한 항목 수 |
| `skippedLand` | 토지 공고라 import 대상에서 제외된 수 |
| `candidates[].id` | 선택 import에 사용할 후보 ID |
| `candidates[].panId` | LH 원본 공고 ID |
| `candidates[].status` | 후보 상태. `COLLECTED`, `IMPORTED`, `FAILED`, `SKIPPED` 중 하나 |
| `candidates[].alreadyImported` | 기존 `announcement`와 연결된 공고인지 여부 |
| `candidates[].canParse` | 토지 공고가 아니고 PDF URL이 있어서 선택 import 가능한지 여부 |
| `candidates[].dedupeStatus` | 중복 판단 결과. 예: `NEW`, `UNCHANGED_SKIP_GEMINI`, `CHANGED_REPARSE`, `FAILED_RETRY`, `LAND_SKIP`, `NO_PDF` |

`alreadyImported=true`는 이미 저장된 LH 공고라는 뜻이다. 이 경우에도 `dedupeStatus`가 `CHANGED_REPARSE`이면 원본 JSON 또는 PDF URL이 바뀐 상태라 재파싱 대상이다.

## 7. 요청 4: 후보 목록 조회

후보 목록 API는 저장된 후보를 페이지 단위로 조회한다. 이 단계도 Gemini를 호출하지 않는다. Apidog에서는 `status`를 바꿔 선택 import 대상을 좁혀 본다.

| 항목 | 값 |
|---|---|
| Method | `GET` |
| URL | `{{baseUrl}}/api/admin/import/lh/candidates?page=0&size=20&status=COLLECTED` |
| Auth | Bearer Token: `{{accessToken}}` |
| Query | `page`: 후보 목록 페이지, 기본값 `0` |
| Query | `size`: 후보 목록 크기, 기본값 `20` |
| Query | `status`: 선택값. 생략하면 전체, 지정 시 `COLLECTED`, `IMPORTED`, `FAILED`, `SKIPPED` 사용 |

정상 기대값:

```json
{
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
  ],
  "totalCount": 1
}
```

`status`에 지원하지 않는 값을 넣으면 400 응답과 `invalid candidate status` 메시지가 나온다.

## 8. 요청 5: 선택 후보 import

선택 import API는 요청 body의 후보 ID만 파싱하고 저장한다. 이 단계가 Gemini 호출 경로다. 단, 중복 판단 결과가 변경 없음이면 Gemini를 건너뛴다.

| 항목 | 값 |
|---|---|
| Method | `POST` |
| URL | `{{baseUrl}}/api/admin/import/lh/selected` |
| Auth | Bearer Token: `{{accessToken}}` |
| Headers | `Content-Type: application/json` |
| Body | JSON |

기본 실행 body:

```json
{
  "candidateIds": [1],
  "force": false
}
```

`force`를 생략하면 `false`로 처리한다.

```json
{
  "candidateIds": [1]
}
```

정상 기대값:

```json
{
  "fetched": 1,
  "scanned": 1,
  "skippedLand": 0,
  "unchanged": 0,
  "geminiSkipped": 0,
  "imported": 1,
  "reparsed": 0,
  "failed": 0
}
```

응답 카운터 의미:

| 필드 | 의미 |
|---|---|
| `fetched` | 요청한 후보 ID 수 |
| `scanned` | 처리 대상으로 확인한 후보 수 |
| `skippedLand` | 토지 공고라 건너뛴 수 |
| `unchanged` | LH 목록 JSON, 상세 JSON, PDF URL이 이전 성공 파싱과 같아서 저장과 Gemini를 건너뛴 수 |
| `geminiSkipped` | Gemini 호출을 하지 않은 수. 변경 없음, PDF 없음, 토지 제외 같은 사유가 포함될 수 있음 |
| `imported` | 저장 또는 갱신까지 완료한 수 |
| `reparsed` | 기존 공고를 다시 파싱한 수. `CHANGED_REPARSE`, `FAILED_RETRY`, `FORCE_REPARSE`가 포함됨 |
| `failed` | 처리 중 실패한 수 |

`changed`는 별도 응답 필드가 아니라 `dedupeStatus=CHANGED_REPARSE`와 `reparsed`로 확인한다. 의미는 LH 목록 JSON, 상세 JSON, PDF URL 중 하나가 이전 fingerprint와 달라 Gemini 재파싱이 필요하다는 뜻이다.

`force=true`는 중복 판단이 변경 없음이어도 강제로 재파싱하는 옵션이다. 비용이 큰 Gemini 호출이 발생할 수 있으므로 Apidog 기본 예시는 `force=false` 또는 생략을 쓴다.

## 9. 요청 6: 단일 공고 강제 재파싱

단일 강제 재파싱 API는 이미 저장된 `announcementId` 하나를 다시 LH API와 PDF 파싱 경로로 보낸다. 이 단계도 Gemini 호출 경로다.

| 항목 | 값 |
|---|---|
| Method | `POST` |
| URL | `{{baseUrl}}/api/admin/import/lh/{{announcementId}}/force-reparse` |
| Auth | Bearer Token: `{{accessToken}}` |

정상 기대값:

```json
{
  "fetched": 1,
  "scanned": 1,
  "skippedLand": 0,
  "unchanged": 0,
  "geminiSkipped": 0,
  "imported": 1,
  "reparsed": 1,
  "failed": 0
}
```

## 10. 스케줄러 동작 메모

매일 실행되는 LH 스케줄러는 전체 LH 파이프라인을 유지한다. 페이지 `1`부터 `10`까지, `size=100`으로 수집하며 LH 목록이 끝났다는 `endOfList`일 때만 중단한다. 변경 없는 공고만 나온 페이지라도 중단하지 않는다.

스케줄러도 fingerprint 중복 판단을 사용한다. 이미 성공 파싱된 공고의 LH 목록 JSON, 상세 JSON, PDF URL이 모두 같으면 `unchanged`로 집계하고 Gemini 호출을 건너뛰어 `geminiSkipped`에 반영한다.

## 11. bootRun 로그 확인

서버를 실행한 PowerShell 창에서 아래 로그가 보이는지 확인한다.

```text
PDF found for panId=...
Gemini PDF parse: url=..., bytes=...
```

두 로그가 보이면 LH 상세 처리, PDF URL 탐지, Gemini PDF 파싱까지 진행된 것이다. 후보 수집과 후보 목록 조회만 실행했다면 이 로그가 없어야 정상이다.

## 12. 요청 7: 검수 대상 공고 찾기

| 항목 | 값 |
|---|---|
| Method | `GET` |
| URL | `{{baseUrl}}/api/admin/review?status=PENDING&page=0&size=20` |
| Auth | Bearer Token: `{{accessToken}}` |

응답의 `content[]`에서 방금 import된 것으로 보이는 공고를 찾고, 해당 `announcementId`를 Apidog 환경 변수 `announcementId`에 저장한다.

확인할 필드:

| 필드 |
|---|
| `announcementId` |
| `noticeName` |
| `regionLevel1` |
| `reviewStatus` |

## 13. 요청 8: Admin detail 확인

| 항목 | 값 |
|---|---|
| Method | `GET` |
| URL | `{{baseUrl}}/api/admin/review/{{announcementId}}` |
| Auth | Bearer Token: `{{accessToken}}` |

확인 포인트:

| 항목 |
|---|
| `units` 배열 존재 |
| `units.length >= 1` |
| unit 안에 `unitSource`, `matchSource`, `confidenceLevel` 존재 |
| admin 응답에는 검수용 필드인 `rawText`, `sourceUnitKey`가 노출되어도 정상 |

## 14. 요청 9: 승인 처리

| 항목 | 값 |
|---|---|
| Method | `POST` |
| URL | `{{baseUrl}}/api/admin/review/{{announcementId}}` |
| Auth | Bearer Token: `{{accessToken}}` |
| Headers | `Content-Type: application/json` |
| Body | JSON |

```json
{
  "action": "APPROVE",
  "note": "Apidog 직접 테스트 승인"
}
```

응답 body가 비어 있어도 HTTP 200이면 정상이다.

## 15. 요청 10: Public detail 확인

| 항목 | 값 |
|---|---|
| Method | `GET` |
| URL | `{{baseUrl}}/api/announcements/{{announcementId}}` |
| Auth | 없음 |

확인 포인트:

| 항목 |
|---|
| public detail은 이번 계획에서 공고 요약/상세 필드만 노출한다. |
| public detail 응답에는 `units` 배열이 없다. |
| public detail 응답에는 admin-only unit/raw 필드인 `rawText`, `sourceUnitKey`가 노출되지 않는다. |

승인 전 public detail이 404로 나오는 것은 정상 범위다. public API는 `APPROVED` 또는 `CORRECTED` 상태의 공고만 노출한다.

## 16. DB 확인

API 응답 확인 후 MySQL에서 대상 공고의 unit row count를 확인한다.

```sql
SELECT COUNT(*) AS unit_count
FROM announcement_unit
WHERE announcement_id = <announcementId>
  AND deleted = 0;
```

정상 기준:

| 기준 |
|---|
| DB `unit_count` |
| admin detail `units.length` |

위 2개 값이 같아야 한다. public detail은 이번 계획에서 summary/detail 전용 응답을 유지하므로 이 비교 대상에 넣지 않는다.

## 17. PASS 기준

| 기준 |
|---|
| 서버가 정상 실행된다. |
| Apidog 관리자 로그인 요청이 성공한다. |
| 레거시 `POST /api/admin/import/lh?page=N&size=3` 또는 새 후보 수집, 목록 조회, 선택 import 흐름이 성공한다. |
| 후보 수집과 후보 목록 조회만 실행한 경우 Gemini 로그가 생기지 않는다. |
| 선택 import 또는 강제 재파싱을 실행한 경우 결과 카운터가 의도와 맞다. |
| import 결과에서 `failed=0`이다. |
| Gemini 파싱 경로를 실행했다면 bootRun 로그에 `PDF found`, `Gemini PDF parse`가 확인된다. |
| admin detail에 `units[]`가 존재한다. |
| 승인 후 public detail 조회가 성공한다. |
| public detail에 `rawText`, `sourceUnitKey`가 노출되지 않는다. |
| DB `announcement_unit` row count와 admin detail `units.length`가 일치한다. |

## 18. Apidog 요청 순서 요약

| 순서 | 이름 | Method | URL | Auth |
|---:|---|---|---|---|
| 1 | 관리자 로그인 | `POST` | `{{baseUrl}}/api/auth/login` | 없음 |
| 2A | 레거시 LH 전체 import | `POST` | `{{baseUrl}}/api/admin/import/lh?page=1&size=3` | Bearer `{{accessToken}}` |
| 2B | 후보 수집 | `POST` | `{{baseUrl}}/api/admin/import/lh/candidates/collect?page=1&size=10` | Bearer `{{accessToken}}` |
| 2C | 후보 목록 | `GET` | `{{baseUrl}}/api/admin/import/lh/candidates?page=0&size=20&status=COLLECTED` | Bearer `{{accessToken}}` |
| 2D | 선택 후보 import | `POST` | `{{baseUrl}}/api/admin/import/lh/selected` | Bearer `{{accessToken}}` |
| 2E | 단일 강제 재파싱 | `POST` | `{{baseUrl}}/api/admin/import/lh/{{announcementId}}/force-reparse` | Bearer `{{accessToken}}` |
| 3 | 검수 목록 | `GET` | `{{baseUrl}}/api/admin/review?status=PENDING&page=0&size=20` | Bearer `{{accessToken}}` |
| 4 | Admin detail | `GET` | `{{baseUrl}}/api/admin/review/{{announcementId}}` | Bearer `{{accessToken}}` |
| 5 | 승인 처리 | `POST` | `{{baseUrl}}/api/admin/review/{{announcementId}}` | Bearer `{{accessToken}}` |
| 6 | Public detail | `GET` | `{{baseUrl}}/api/announcements/{{announcementId}}` | 없음 |

## 19. 2026-05-13 기준 검증 예시

아래 값은 레거시 전체 import 경로로 검증한 기존 기록이다. 새 후보 API가 추가된 뒤에도 레거시 경로가 유지되는지 비교할 때 참고한다.

| 항목 | 값 |
|---|---|
| Gemini 모델 | `gemini-2.5-flash-lite` |
| import 요청 | `POST /api/admin/import/lh?page=1&size=3` |
| import 결과 | `imported=1`, `failed=0` |
| import panId | `LN-0002821` |
| 대상 공고 | `announcementId=7` |
| unit | `unitId=43`, `unitSource=PDF_AI`, `matchSource=AI`, `confidenceLevel=HIGH` |
| admin detail | `units_count=1` |
| public detail | 조회 성공, `units` 없음 |
| DB count | `announcement_unit` active row `1` |
| 최종 판정 | `PASS` |

자세한 실행 기록은 `현행_LH공고수집_단위데이터구조개편_테스트기록.md`를 참고한다.

## 20. `announcement` 관련 테이블 관계

### 구조 
```
announcement 1개
├─ 공고 기본 정보
│  ├─ 공고 ID: id
│  ├─ 공고명: noticeName
│  ├─ 공급기관명: providerName
│  ├─ 원본 공고 ID: sourceNoticeId
│  ├─ 원본 공고 URL: sourceNoticeUrl
│  ├─ 공고 상태: noticeStatus
│  ├─ 공고일: announcementDate
│  ├─ 신청 시작일: applicationStartDate
│  ├─ 신청 마감일: applicationEndDate
│  ├─ 당첨자 발표일: winnerAnnouncementDate
│  ├─ 지역: regionLevel1, regionLevel2
│  ├─ 주소: fullAddress
│  ├─ 단지명: complexName
│  ├─ 대표 공급유형: supplyTypeRaw, supplyTypeNormalized
│  ├─ 대표 주택유형: houseTypeRaw, houseTypeNormalized
│  ├─ 대표 보증금: depositAmount
│  ├─ 대표 월세: monthlyRentAmount
│  └─ 대표 공급세대수: supplyHouseholdCount
│
├─ announcement_detail 1개
│  ├─ 신청 일시 원문: applicationDatetimeText
│  ├─ 서류 제출 시작일: documentSubmitStartDate
│  ├─ 서류 제출 마감일: documentSubmitEndDate
│  ├─ 계약 시작일: contractStartDate
│  ├─ 계약 마감일: contractEndDate
│  ├─ 단지명: complexName
│  ├─ 단지 주소: complexAddress
│  ├─ 단지 상세 주소: complexDetailAddress
│  ├─ 전체 세대 수: householdCount
│  ├─ 난방 방식: heatingType
│  ├─ 전용면적 원문: exclusiveAreaText
│  ├─ 전용면적 숫자값: exclusiveAreaValue
│  ├─ 입주 예정월: moveInExpectedYm
│  ├─ 안내사항 원문: guideText
│  ├─ 문의 전화번호: contactPhone
│  ├─ 문의처 주소: contactAddress
│  ├─ 문의처 안내 원문: contactGuideText
│  ├─ 공급세대수 추출 원문: supplyHouseholdCountRaw
│  ├─ 공급세대수 추출 근거: supplyHouseholdCountBasis
│  ├─ 공급세대수 신뢰도: supplyHouseholdCountConfidence
│  ├─ 보증금/월세 추출 원문: depositMonthlyRentRaw
│  ├─ 소득/자산 기준 원문: incomeAssetCriteriaRaw
│  ├─ 문의처 추출 원문: contactRaw
│  ├─ 자격조건 원문: eligibilityRaw
│  ├─ 공고 유형: noticeType
│  ├─ 분양가 원문: salePriceRaw
│  ├─ 상세 일정 JSON: scheduleDetailsJson
│  └─ 중요 유의사항 원문: importantNotesRaw
│
├─ announcement_eligibility 1개
│  ├─ 최소 나이: ageMin
│  ├─ 최대 나이: ageMax
│  ├─ 나이 조건 원문: ageRawText
│  ├─ 혼인 조건 유형: maritalTargetType
│  ├─ 혼인 기간 제한: marriageYearLimit
│  ├─ 혼인 조건 원문: maritalRawText
│  ├─ 최소 자녀 수: childrenMinCount
│  ├─ 자녀 조건 원문: childrenRawText
│  ├─ 무주택 필요 여부: homelessRequired
│  ├─ 무주택 조건 원문: homelessRawText
│  ├─ 저소득 필요 여부: lowIncomeRequired
│  ├─ 소득/자산 기준 원문: incomeAssetCriteriaRaw
│  ├─ 고령자 필요 여부: elderlyRequired
│  ├─ 고령자 최소 나이: elderlyAgeMin
│  ├─ 고령자 조건 원문: elderlyRawText
│  ├─ 전체 자격조건 원문: eligibilityRaw
│  ├─ 특별공급 조건 원문: specialSupplyRaw
│  ├─ 검수 상태: reviewStatus
│  ├─ 검수자: reviewedBy
│  ├─ 검수 시각: reviewedAt
│  └─ 검수 메모: reviewNote
│
└─ announcement_unit 여러 개
   ├─ unit 1
   │  ├─ 단위 ID: id
   │  ├─ 표시 순서: unitOrder
   │  ├─ 데이터 출처: unitSource
   │  ├─ 원본 row 식별키: sourceUnitKey
   │  ├─ 단지명: complexName
   │  ├─ 주소: fullAddress
   │  ├─ 지역: regionLevel1, regionLevel2
   │  ├─ 공급유형 원문: supplyTypeRaw
   │  ├─ 공급유형 정규화값: supplyTypeNormalized
   │  ├─ 주택유형 원문: houseTypeRaw
   │  ├─ 주택유형 정규화값: houseTypeNormalized
   │  ├─ 전용면적 원문: exclusiveAreaText
   │  ├─ 전용면적 숫자값: exclusiveAreaValue
   │  ├─ 보증금: depositAmount
   │  ├─ 월세: monthlyRentAmount
   │  ├─ 최소 분양가: salePriceMin
   │  ├─ 최대 분양가: salePriceMax
   │  ├─ 분양가 원문: salePriceRaw
   │  ├─ 공급세대수: supplyHouseholdCount
   │  ├─ 추출 원문: rawText
   │  ├─ 매칭 근거: matchSource
   │  └─ 신뢰도: confidenceLevel
   │
   ├─ unit 2
   │  └─ unit 1과 같은 필드 구조
   │
   └─ unit N
      └─ unit 1과 같은 필드 구조
```

LH import가 성공하면 공고 1건을 중심으로 아래 4개 테이블이 함께 채워진다.

```text
announcement 1 ── 0..1 announcement_detail
announcement 1 ── 1    announcement_eligibility
announcement 1 ── 0..N announcement_unit
```

### 15.1 `announcement`: 공고 parent

`announcement`는 공고의 기준 parent 테이블이다. LH 원본 공고 1건은 `source_primary = LH`, `source_notice_id = PAN_ID` 조합으로 식별된다.

주요 역할은 아래와 같다.

- 공고명, 공급기관, 원본 URL, 공고 상태, 신청 시작/마감일 같은 목록/상세 공통 정보를 가진다.
- 지역, 주소, 단지명, 공급유형, 주택유형, 보증금, 월세, 공급세대수 같은 summary 값을 가진다.
- summary 값은 호환용 대표값이다. 단위별 실제 값은 `announcement_unit`에 저장되고, import 후 unit row 기준으로 다시 계산된다.
- `deleted`, `deleted_at`을 가진 soft delete 대상이다.

import 시에는 `NoticeImportPersistenceService.upsertLh()`가 `sourcePrimary=LH`, `sourceNoticeId=panId`로 기존 공고를 찾고, 없으면 새로 만든다. 이후 `updateFromImport()`로 LH 목록 API의 기본값을 반영한다.

### 15.2 `announcement_detail`: 공고 상세/원문 보강 정보

`announcement_detail`은 `announcement`와 1:1 관계다.

```text
announcement_detail.announcement_id -> announcement.id
unique = true
```

주요 역할은 아래와 같다.

- 신청 일시, 서류 제출 기간, 계약 기간, 단지 주소, 난방 방식, 전용면적, 입주 예정월 같은 상세 정보를 가진다.
- LH 상세 API의 `dsSplScdl`, `dsSbd`, `dsEtcInfo`, `dsCtrtPlc`에서 온 값을 저장한다.
- Gemini PDF 파싱 결과 중 공고 단위 원문도 저장한다.
- 예: `noticeType`, `salePriceRaw`, `scheduleDetailsJson`, `importantNotesRaw`, `depositMonthlyRentRaw`, `supplyHouseholdCountRaw`, `eligibilityRaw`.
- admin/public 상세 응답에서 공고 설명과 원문성 정보를 보강하는 데 쓰인다.
- `deleted`, `deleted_at`을 가진 soft delete 대상이다.

import 시에는 `upsertLhDetail()`이 `findByAnnouncementIdAndDeletedFalse()`로 기존 detail을 찾고, 없으면 생성한다. PDF 파싱 결과가 있으면 `updatePdfParseResult()`로 Gemini 추출 원문/일정/유의사항을 추가 반영한다.

### 15.3 `announcement_eligibility`: 자격조건 + 검수 상태

`announcement_eligibility`도 `announcement`와 1:1 관계다.

```text
announcement_eligibility.announcement_id -> announcement.id
unique = true
```

주요 역할은 아래와 같다.

- 나이, 혼인, 자녀, 무주택, 저소득, 고령자 조건 같은 신청 자격조건을 가진다.
- Gemini PDF 파싱 결과의 eligibility 영역에서 채워진다.
- 관리자 검수 상태를 가진다.
- `reviewStatus`: `PENDING`, `APPROVED`, `CORRECTED`, `REJECTED`, `RE_IMPORT`.
- `reviewedBy`, `reviewedAt`, `reviewNote`로 검수 이력을 기록한다.

이 테이블은 public 노출 여부를 결정하는 핵심이다. public 공고 목록/상세 조회는 `announcement_eligibility.reviewStatus`가 `APPROVED` 또는 `CORRECTED`인 공고만 조회한다. 그래서 import 직후 `PENDING` 상태에서는 public detail이 404가 될 수 있고, admin에서 `APPROVE` 후 public detail이 조회된다.

검수 API와의 연결은 아래와 같다.

- `POST /api/admin/review/{announcementId}` + `APPROVE`: `reviewStatus`를 `APPROVED`로 변경한다.
- `CORRECT`: 일부 자격조건과 summary 값을 수정하고 `CORRECTED`로 변경한다.
- `REJECT`: `REJECTED`로 변경한다.
- `REIMPORT`: LH 재수집/재파싱 후 `PENDING`으로 되돌린다.

PDF 파싱 실패 시에도 검수 큐에 올리기 위해 빈 `announcement_eligibility`가 생성될 수 있다. 이 경우에도 기본 상태는 `PENDING`이다.

### 15.4 `announcement_unit`: 실제 신청/공급 단위 row

`announcement_unit`은 `announcement`와 N:1 관계다. 공고 1건에 여러 공급 단위가 붙을 수 있다.

```text
announcement_unit.announcement_id -> announcement.id
announcement 1개 : announcement_unit 여러 개
unique(announcement_id, unit_source, source_unit_key)
```

주요 역할은 아래와 같다.

- 한 공고 안의 실제 신청 가능한 공급 단위를 row 단위로 저장한다.
- 단지, 주소, 공급유형, 주택유형, 전용면적, 보증금, 월세, 분양가, 공급세대수를 unit별로 가진다.
- `unitSource`로 출처를 구분한다.
- `LH_API`: LH 상세 API row만으로 만든 unit.
- `PDF_AI`: Gemini PDF 파싱에서만 나온 unit.
- `MERGED`: LH row와 PDF unit을 매칭해서 합친 unit.
- `matchSource`는 매칭 근거를 나타낸다. 현재 흐름에서는 rule 기반 LH 단독이면 `RULE`, PDF가 결합되면 `AI`가 들어간다.
- `confidenceLevel`은 PDF/Gemini 추출 신뢰도를 나타낸다.
- admin 응답에는 검수용 `rawText`, `sourceUnitKey`가 노출된다.
- public detail 응답은 이번 계획에서 unit 배열 자체를 노출하지 않는다. 따라서 `rawText`, `sourceUnitKey`도 없다.

import 시에는 `replaceUnits()`가 먼저 기존 unit row를 `announcementId` 기준으로 제거한 뒤 다시 저장한다. 이후 LH `dsSbd` 후보와 Gemini `units[]`를 주소, 단지명, 전용면적, row 순서 기준으로 매칭한다.

저장 규칙은 아래와 같다.

- LH 후보만 있고 PDF 매칭이 없으면 `LH_API` unit으로 저장한다.
- LH 후보와 PDF unit이 매칭되면 `MERGED` unit으로 저장한다.
- PDF unit이 남아 있고 LH 후보와 매칭되지 않으면 `PDF_AI` unit으로 저장한다.

unit 저장 후 `AnnouncementUnitSummaryService.applySummary()`가 unit row들을 읽어 `announcement`의 대표 주소, 단지명, 주택유형, 최소 보증금, 최소 월세, 공급세대수 합계를 다시 계산한다.

### 15.5 import 요청 기준 저장 흐름

Apidog에서 아래 요청을 호출하면 이 흐름이 시작된다.

```text
POST {{baseUrl}}/api/admin/import/lh?page=1&size=3
```

처리 순서는 아래와 같다.

1. `ImportController.importLh()`가 `NoticeImportOrchestrator.importLhNotices(page, size)`를 호출한다.
2. LH 목록 API에서 `dsList`를 가져온다.
3. 토지 공고(`UPP_AIS_TP_CD = 01`)는 서비스 대상이 아니라 skip한다.
4. 각 공고 item마다 `upsertLh()`로 `announcement`를 생성 또는 갱신한다.
5. LH 상세 API를 호출한다.
6. 상세 응답에서 PDF URL을 찾으면 `PdfParsingService`가 Gemini로 PDF를 파싱한다.
7. `upsertLhDetail()`이 `announcement_detail`을 생성 또는 갱신한다.
8. Gemini eligibility 결과가 있으면 `announcement_eligibility`를 생성 또는 갱신한다. 실패해도 검수 큐용 `PENDING` row를 보장한다.
9. `replaceUnits()`가 `announcement_unit`을 delete-and-replace 방식으로 재구성한다.
10. unit row 기준으로 `announcement` summary 값을 다시 계산한다.
11. admin review API에서는 `announcement`, `announcement_detail`, `announcement_eligibility`, `announcement_unit`을 합쳐 검수 상세를 보여준다.
12. admin이 `APPROVE`하면 `announcement_eligibility.reviewStatus`가 `APPROVED`가 되고, public API에서 해당 공고가 조회 가능해진다.

### 15.6 API 응답에서 보이는 차이

| API | 참조 테이블 | 특징 |
|---|---|---|
| `GET /api/admin/review?status=PENDING` | `announcement_eligibility` + `announcement` | 검수 상태 기준 목록이다. import 직후 공고를 찾을 때 사용한다. |
| `GET /api/admin/review/{announcementId}` | 4개 테이블 전체 | 관리자 검수용 상세다. `rawText`, `sourceUnitKey` 같은 검수용 unit 필드가 포함된다. |
| `POST /api/admin/review/{announcementId}` | `announcement_eligibility` 중심 | `APPROVE`, `CORRECT`, `REJECT`, `REIMPORT`로 검수 상태를 변경한다. |
| `GET /api/announcements/{announcementId}` | `announcement`, `announcement_detail` | public 상세다. `APPROVED`/`CORRECTED` 공고만 보이며 admin-only unit/raw 필드는 빠진다. |

테스트할 때는 `announcement_unit` row count와 admin detail `units.length`가 같은지 확인하면 단위 데이터 저장과 관리자 검수 API 노출이 일관적인지 빠르게 검증할 수 있다. public detail은 이번 계획에서 summary/detail 전용 응답을 유지하며 admin-only unit/raw 필드를 노출하지 않는다.
