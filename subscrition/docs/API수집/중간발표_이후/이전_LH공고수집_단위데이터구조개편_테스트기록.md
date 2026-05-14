# [이전] LH 공고 수집 단위 데이터 구조 개편 테스트 기록

> 테스트 일자: 2026-05-13  
> 대상: `subscrition` 정식 백엔드  
> 목적: LH 공고 수집, Gemini PDF 파싱, `announcement_unit` 저장, admin/public API 노출, DB row count 일치 여부를 확인한다.

> 현재 상태: 이 문서는 2026-05-13 시점의 수동 테스트 evidence다. 이후 public detail 계약은 summary/detail 전용으로 복구되었고, public detail `units.length`는 현재 검증 기준이 아니라 historical evidence로만 유지한다. 현재 Apidog 기준은 `현행_LH공고수집_단위데이터구조개편_수동테스트가이드.md`를 따른다.

## 1. 테스트 조건

| 항목 | 내용 |
|---|---|
| 실행 방식 | 로컬 `bootRun` |
| 서버 | `http://localhost:8080` |
| admin 계정 | `admin` / `REDACTED_LOCAL_PASSWORD` |
| Gemini 모델 | `gemini-2.5-flash-lite` |
| import 크기 | `size=3` |
| 대상 공고 | `announcementId=7` |
| import panId | `LN-0002821` |

Gemini RPD 제한을 고려해 import 호출은 `size=3`으로 제한했다. 테스트 전 `application.properties`의 `gemini.model`을 `gemini-2.5-flash-lite`로 변경했다.

## 2. 실행 명령

### 서버 실행

```powershell
cd subscrition
.\gradlew bootRun
```

결과:

```text
Started SubscriptionApplication in 8.249 seconds
port 8080 LISTENING
```

### 관리자 로그인

```http
POST /api/auth/login
Content-Type: application/json

{
  "loginId": "admin",
  "password": "REDACTED_LOCAL_PASSWORD"
}
```

결과:

```text
login=OK
userId=1
loginId=admin
role=ADMIN
token=REDACTED
```

### LH 공고 수집

```http
POST /api/admin/import/lh?page=1&size=3
Authorization: Bearer REDACTED
```

결과:

```text
import page=1 size=3 imported=1 failed=0 elapsedSec=35
```

## 3. bootRun 로그 확인

확인된 주요 로그:

```text
Started SubscriptionApplication in 8.249 seconds (process running for 8.606)
PDF found for panId=LN-0002821: https://apply.lh.or.kr/lhapply/lhFile.do?fileid=66763119
Gemini PDF parse: url=https://apply.lh.or.kr/lhapply/lhFile.do?fileid=66763119, bytes=147399
```

로그 기준으로 LH 상세 처리 후 PDF URL을 찾았고, PDF bytes를 Gemini로 직접 파싱했다. `failed=0`이므로 실패 panId 분석은 필요하지 않았다. Gemini 429 또는 rate limit block도 확인되지 않았다.

## 4. Admin Review Detail 확인

대상 공고:

```text
announcementId=7
reviewStatus=PENDING
units_count=1
```

admin detail에서 확인한 unit 요약:

| 항목 | 값 |
|---|---|
| `unitId` | `43` |
| `unitOrder` | `0` |
| `unitSource` | `PDF_AI` |
| `matchSource` | `AI` |
| `confidenceLevel` | `HIGH` |
| `rawText` | 존재 |
| `sourceUnitKey` | 존재 |
| `depositAmount` | `12465000` |
| `monthlyRentAmount` | `346200` |
| `salePriceMin` / `salePriceMax` | `null` / `null` |

추가 확인:

| 필드 | 결과 |
|---|---|
| `noticeType` | 존재 |
| `scheduleDetailsJson` | 존재 |
| `importantNotesRaw` | 존재 |
| `salePriceRaw` | 없음. 임대 공고라 정상 범위 |

원본 JSON evidence: `.sisyphus/evidence/task-3-admin-detail.json`

## 5. 승인 및 Public Detail 확인

승인 요청:

```http
POST /api/admin/review/7
Authorization: Bearer REDACTED
Content-Type: application/json

{
  "action": "APPROVE",
  "note": "단위 데이터 구조 개편 테스트 승인"
}
```

결과:

```text
approval=OK target=7
```

public detail 요청:

```http
GET /api/announcements/7
```

결과:

```text
public_status=OK
units_count=1
rawText_exposed=False
sourceUnitKey_exposed=False
```

public API는 승인 후 정상 조회되었고, `units[]` 1개를 반환했다. admin-only 필드인 `rawText`, `sourceUnitKey`는 public 응답에 노출되지 않았다.

원본 JSON evidence: `.sisyphus/evidence/task-4-public-detail.json`

## 6. DB `announcement_unit` 확인

실행한 조회:

```sql
SELECT COUNT(*) AS unit_count
FROM announcement_unit
WHERE announcement_id = 7 AND deleted = 0;
```

결과:

```text
unit_count = 1
```

대표 row:

| 컬럼 | 값 |
|---|---|
| `id` | `43` |
| `unit_source` | `PDF_AI` |
| `source_unit_key` | `pdf-8080106df285cf75e77ddf7d16df` |
| `unit_order` | `0` |
| `deposit_amount` | `12465000` |
| `monthly_rent_amount` | `346200` |
| `sale_price_min` | `NULL` |
| `sale_price_max` | `NULL` |

비교 결과:

| 기준 | count |
|---|---:|
| admin detail `units.length` | 1 |
| public detail `units.length` | 1 |
| DB `announcement_unit` row count | 1 |

위 public detail `units.length` 값은 당시 테스트 응답에서 관찰한 historical evidence다. 현재 이 계획의 public detail은 summary/detail 전용 응답이며 `units`를 노출하지 않는다.

현재 검증 기준은 DB `announcement_unit` row count와 admin detail `units.length` 비교다. 위 기록에서는 두 값이 모두 1로 일치한다.

## 7. 특이사항

- PowerShell 출력/저장 과정에서 일부 한글 JSON evidence가 mojibake 형태로 보일 수 있다. 구조 필드, count, boolean 검증에는 영향이 없었다.
- 수집된 대상은 상가 임대 공고로 보이며, `salePriceRaw`, `salePriceMin`, `salePriceMax`가 없는 것은 임대 공고 범위에서 정상이다.
- 보증금/월세 값은 Gemini 파싱 결과 기준으로 저장되었다. 금액 단위 적합성은 별도 검수 항목으로 남길 수 있다.
- 이번 테스트에서는 반복 import에 따른 delete-and-replace 안정성은 추가 Gemini quota 소비를 피하기 위해 별도 실행하지 않았다.

## 8. 최종 판정

| 항목 | 결과 |
|---|---|
| Gemini Flash Lite 설정 | PASS |
| 서버 기동 | PASS |
| admin 로그인 | PASS |
| `size=3` LH import | PASS |
| PDF URL 탐지 | PASS |
| Gemini PDF 파싱 호출 | PASS |
| admin detail `units[]` | PASS |
| admin raw/source 필드 | PASS |
| 승인 후 public detail | PASS |
| public admin-only 필드 미노출 | PASS |
| DB row count 일치 | PASS |

최종 verdict: **PASS**

## 9. 후속 확인 권장

- 임대 주택, 분양/분양전환, 다중 단지 공고를 추가로 각각 1건씩 확인하면 케이스 커버리지가 좋아진다.
- Gemini quota 여유가 있을 때 같은 공고 reimport를 반복 실행해 `announcement_unit` row가 중복 생성되지 않고 유지되는지 확인한다.
- PowerShell/콘솔 인코딩을 UTF-8로 맞춘 뒤 evidence JSON을 다시 저장하면 한글 가독성이 좋아진다.
