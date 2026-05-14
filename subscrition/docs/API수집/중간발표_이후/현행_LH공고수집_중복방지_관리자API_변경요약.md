# [현행] LH 공고 수집 중복 방지 및 관리자 API 변경 요약

> 작성일: 2026-05-14  
> 대상: `subscrition` 정식 백엔드  
> 목적: 중간발표 이후 LH 공고 수집 구조가 무엇에서 무엇으로 바뀌었는지 정리한다.  
> 범위: 기존 `/api/admin/import/lh` 유지, 관리자 후보 수집/선택 import 추가, scheduler dedupe, Gemini 호출 절감, Apidog 문서 반영.

## 1. 한 줄 요약

기존 구조는 관리자가 `POST /api/admin/import/lh?page=&size=`를 호출하면 LH 목록 조회부터 Gemini PDF 파싱과 정식 DB 저장까지 바로 실행되는 방식이었다. 현재 구조는 기존 API를 유지하면서, 별도 관리자 API로 `raw 후보 수집 -> 후보 조회 -> 선택한 후보만 파싱` 흐름을 추가했고, scheduler와 import 경로에는 fingerprint 기반 중복 판단을 넣어 변경 없는 공고는 Gemini를 다시 호출하지 않도록 바뀌었다.

## 2. 전체 흐름 변경

| 구분 | 이전 | 현재 |
|---|---|---|
| 관리자 수동 수집 | `/api/admin/import/lh?page=&size=` 하나로 목록, 상세, PDF, Gemini, DB 저장까지 즉시 실행 | 기존 API는 legacy/full import로 유지하고, 후보 수집/목록/선택 import/강제 재파싱 API를 별도로 추가 |
| 후보 확인 | 파싱 전에 관리자가 raw 후보를 따로 볼 수 없음 | `lh_import_candidate`에 LH list/detail JSON, PDF URL, dedupe 상태를 저장하고 조회 가능 |
| Gemini 호출 | 수동 import 또는 scheduler가 같은 공고를 다시 만나면 재호출 가능성이 큼 | canonical hash와 fingerprint가 같으면 `UNCHANGED_SKIP_GEMINI`로 Gemini 호출 생략 |
| scheduler 중단 조건 | `imported=0 && failed=0`이면 다음 페이지 스캔 중단 위험 | LH page가 비었거나 `endOfList=true`일 때만 중단. unchanged-only page도 계속 스캔 |
| force reparse | 기존 reimport/review 흐름 중심 | 관리자 API에서 기존 LH 공고 단건 강제 재파싱 가능 |
| public API | unit/raw 필드를 public detail로 노출하는 방향의 이전 기록이 있었음 | 이번 작업 범위에서는 public API 계약 변경 없이 유지. unit/raw는 admin 검수/DB 중심으로 확인 |

## 3. API 변경

### 3.1 유지된 API

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| Path | `/api/admin/import/lh?page=&size=` |
| 상태 | 유지 |
| 역할 | LH 목록 조회부터 상세/PDF/Gemini/DB 저장까지 수행하는 legacy/full import |
| 응답 | 기존 호환을 위해 `imported`, `failed` 중심 |

### 3.2 새로 추가된 관리자 API

| Method | Path | 역할 | Gemini 호출 여부 |
|---|---|---|---|
| `POST` | `/api/admin/import/lh/candidates/collect?page=&size=` | LH 목록/상세를 조회해 후보를 `lh_import_candidate`에 저장 | 호출 안 함 |
| `GET` | `/api/admin/import/lh/candidates?page=&size=&status=` | 저장된 후보 목록과 dedupe 상태 조회 | 호출 안 함 |
| `POST` | `/api/admin/import/lh/selected` | 요청한 candidate id만 정식 import하고 실행 카운터 반환 | 필요 시 호출 |
| `POST` | `/api/admin/import/lh/{announcementId}/force-reparse` | 기존 LH 공고 단건 강제 재파싱 | 호출 가능 |

### 3.3 선택 import 응답 해석

`POST /api/admin/import/lh/selected`의 응답은 import 실행 결과를 집계한 카운터다. 이 응답은 "공고 저장/파싱 플로우가 실행됐는지"를 알려주지만, Gemini 결과 내부의 `eligibility` 생성 여부나 관리자 검수 row 생성 여부까지 직접 알려주지는 않는다.

예시 응답:

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

위 응답의 의미는 아래와 같다.

| 필드 | 의미 |
|---|---|
| `fetched` | 요청 body의 `candidateIds` 중 처리 대상으로 가져온 후보 수 |
| `scanned` | import 로직에서 검사한 후보 수 |
| `skippedLand` | 토지 공고로 판단되어 제외된 수 |
| `unchanged` | 기존 공고와 fingerprint가 같아 정식 저장/재파싱을 생략한 수 |
| `geminiSkipped` | Gemini 호출을 생략한 수 |
| `imported` | 정식 공고 import 플로우가 성공한 수 |
| `reparsed` | 기존 공고를 변경 감지 또는 force로 재파싱한 수 |
| `failed` | 처리 실패 수 |

주의할 점:

- `imported=1`은 `announcement` 저장과 import 플로우 성공을 의미한다.
- `imported=1`이 `announcement_eligibility` 생성까지 보장하지는 않는다.
- Gemini `PDF_AI_JSON`에서 `eligibility=null`이면 자격조건 검수 row가 생성되지 않을 수 있다.
- 예를 들어 상가 분양 공고는 `units`, `salePriceRaw`, `houseType=상가` 등이 파싱되어도 청년/신혼부부/무주택/소득자산 자격조건이 없어 `eligibility=null`일 수 있다.

따라서 selected import 이후 관리자가 검수 대상 생성 여부까지 확인하려면 아래 API를 이어서 호출한다.

| 확인 목적 | API |
|---|---|
| 공고 기본 저장 여부 | `GET /api/announcements` |
| 검수 대기 목록 | `GET /api/admin/review?status=PENDING&page=0&size=20` |
| 전체 검수 목록 | `GET /api/admin/review?page=0&size=20` |
| 검수 상태별 건수 | `GET /api/admin/review/stats` |

## 4. DB/엔티티 변경

| 구분 | 이전 | 현재 |
|---|---|---|
| LH 공고 식별 | `Announcement.sourcePrimary/sourceNoticeId`를 사용했지만 제약과 정리 흐름이 약함 | `source_primary + source_notice_id` unique constraint `uk_announcement_source_notice`로 LH `PAN_ID` 기준 식별 강화 |
| import fingerprint | 별도 공식 dedupe 메타데이터 없음 | `announcement_import_fingerprint`로 list/detail/PDF/Gemini hash와 parse status 저장 |
| 관리자 후보 staging | `announcement_parse_raw`는 announcement 저장 후 raw 보관 용도라 후보 저장에 부적합 | `lh_import_candidate`를 별도 staging 테이블로 사용 |
| 후보 raw 데이터 | 파싱 전 후보 raw 저장 흐름 없음 | `listRawJson`, `detailRawJson`, `listHash`, `detailHash`, `pdfUrl`, `dedupeStatus`, `collectedAt` 저장 |
| 검수 대상 row | 공고 저장과 검수 row 생성 관계를 구분하기 어려움 | `announcement_eligibility`는 Gemini 결과의 `eligibility`가 있거나 PDF 파싱 실패 fallback일 때 생성. `PDF_AI_JSON.eligibility=null`이면 공고는 저장되어도 검수 목록에는 안 나올 수 있음 |

## 5. 중복 판단 방식 변경

| 항목 | 이전 | 현재 |
|---|---|---|
| 기준 | `PAN_ID` 또는 저장 여부 중심으로 판단하기 쉬움 | `PAN_ID`는 기존 공고를 찾는 키로만 사용하고, skip 여부는 canonical hash와 PDF URL로 판단 |
| JSON hash | raw 문자열 순서 차이에 취약할 수 있음 | `CanonicalJsonHasher`가 object key를 정렬한 뒤 SHA-256 hash 생성 |
| parse status | 이전 실패 상태를 dedupe와 함께 다루기 어려움 | `PENDING`, `PARSED`, `FAILED` 상태 저장. 이전 실패는 동일 fingerprint여도 retry 대상 |
| force | 일반 dedupe 판단과 분리 부족 | `force=true`면 변경 없음이어도 `FORCE_REPARSE`로 재파싱 |

## 6. scheduler 변경

| 항목 | 이전 | 현재 |
|---|---|---|
| 호출 경로 | legacy import 결과의 `imported/failed`만 보고 판단 | scheduler 전용 `importLhNoticesForScheduler(page, 100)` 사용 |
| page loop | `MAX_PAGES=10`, `BATCH_SIZE=100` | 유지 |
| stop 조건 | `imported=0 && failed=0`이면 조기 중단 가능 | `endOfList=true`일 때만 중단 |
| 로그 | imported/failed 중심 | fetched, scanned, imported, unchanged, geminiSkipped, failed 집계 |
| force reparse | scheduler에서 force 가능성 차단 필요 | scheduler mode는 `force=false`, force는 관리자 단건/선택 경로에서만 사용 |

## 7. Apidog 테스트 기준 변경

| 구분 | 이전 기준 | 현재 기준 |
|---|---|---|
| 기본 수동 테스트 | `/api/admin/import/lh?page=1&size=3` 중심 | legacy API 테스트도 유지하되, 후보 수집/목록/선택 import/force reparse도 별도 테스트 |
| 후보 수집 확인 | 없음 | collect/list 호출만으로 Gemini 로그가 생기지 않아야 함 |
| 선택 import 확인 | 없음 | 요청한 `candidateIds`만 import되어야 함 |
| 선택 import 응답 해석 | 없음 | `imported=1`은 import 플로우 성공 의미. `eligibility` 생성 여부는 `/api/admin/review`, `/api/admin/review/stats`, `PDF_AI_JSON`으로 후속 확인 |
| dedupe 확인 | 반복 호출 시 Gemini 재호출 여부 확인 어려움 | `unchanged`, `geminiSkipped`, `dedupeStatus`로 판단 |
| public detail 확인 | 이전 기록에는 public `units` 노출을 확인하는 내용이 있었음 | 현재 계획에서는 public detail을 summary/detail 전용 계약으로 유지. unit count는 DB와 admin detail 기준으로 확인 |

## 8. 검증 결과

자동 검증 evidence는 아래 파일에 남겼다.

| 파일 | 내용 |
|---|---|
| `.sisyphus/evidence/task-8-gradle-test.txt` | targeted regression test와 full `gradlew test` 결과 |
| `.sisyphus/evidence/task-8-gradle-build.txt` | full `gradlew build` 결과 |

검증된 항목은 아래와 같다.

- legacy `POST /api/admin/import/lh` 호환 유지
- scheduler unchanged-only page continuation
- candidate collect/list no-Gemini 동작
- selected import selected-only 동작
- force reparse path
- dedupe decision: new, unchanged, changed, failed retry, force, no PDF, land skip
- Apidog 문서의 legacy/new admin API 설명

Live LH/Gemini/Apidog 테스트는 quota 소모와 외부 의존성을 피하기 위해 자동 검증에서는 실행하지 않았다. 실제 수동 테스트는 `현행_LH공고수집_단위데이터구조개편_수동테스트가이드.md` 기준으로 작은 `size`부터 진행한다.

## 9. 관련 문서 상태

| 문서 | 현재 상태 | 비고 |
|---|---|---|
| `현행_LH공고수집_단위데이터구조개편_수동테스트가이드.md` | 현행 유지 | 새 admin API와 dedupe 기준이 반영된 Apidog 기준 문서 |
| `이전_LH공고수집_단위데이터구조개편_작업기록.md` | 이전 기록 | unit 구조 개편 당시 기록. public API units 노출 내용은 현재 계약과 다름 |
| `이전_LH공고수집_단위데이터구조개편_테스트기록.md` | 이전 기록 | 2026-05-13 수동 테스트 evidence. public units 관찰값은 historical evidence로만 유지 |
| `이전_LH공고수집_수동테스트_검증보고서.md` | 이전 기록 | dedupe/admin 후보 API 도입 전 legacy import 검증 보고서 |
