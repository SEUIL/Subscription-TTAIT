# [현행] LH 공고 수집 중복 방지 및 관리자 선택 흐름 기능 명세

> 작성일: 2026-05-14  
> 대상: `subscrition` 정식 백엔드  
> 목적: 현재 LH 공고 수집, dedupe, scheduler, 관리자 후보 선택 import 흐름을 한 문서에서 설명한다.

## 1. 현재 기능 개요

현재 LH 공고 수집은 두 경로를 동시에 가진다.

| 경로 | 설명 |
|---|---|
| Legacy/full import | 기존 `POST /api/admin/import/lh?page=&size=`. LH 목록, 상세, PDF, Gemini, 정식 DB 저장까지 한 번에 처리한다. |
| 관리자 선택 import | 후보 수집 API로 raw 후보를 저장하고, 관리자가 선택한 후보만 정식 import한다. |

두 경로 모두 기존 LH 공고의 변경 여부를 fingerprint로 판단한다. 변경 없는 공고는 Gemini를 다시 호출하지 않고 기존 파싱 데이터를 보존한다.

## 2. 주요 API

| Method | Path | 역할 |
|---|---|---|
| `POST` | `/api/admin/import/lh?page=&size=` | 기존 legacy/full import. 응답은 `imported`, `failed` 유지 |
| `POST` | `/api/admin/import/lh/candidates/collect?page=&size=` | LH 목록/상세를 후보 테이블에 저장. Gemini 호출 없음 |
| `GET` | `/api/admin/import/lh/candidates?page=&size=&status=` | 저장된 후보 목록과 dedupe 상태 조회 |
| `POST` | `/api/admin/import/lh/selected` | 선택한 candidate id만 정식 import |
| `POST` | `/api/admin/import/lh/{announcementId}/force-reparse` | 기존 LH 공고 단건 강제 재파싱 |

## 3. 현재 처리 흐름

```text
LH 목록 API
  -> 토지 공고 여부 판단
  -> LH 상세 API
  -> PDF URL 추출
  -> dedupe decision
      -> NEW / CHANGED_REPARSE / FAILED_RETRY / FORCE_REPARSE: Gemini 파싱 후 저장
      -> UNCHANGED_SKIP_GEMINI: Gemini 호출 없이 기존 파싱 데이터 보존
      -> LAND_SKIP / NO_PDF: 상황에 따라 skip 또는 metadata 저장
```

관리자 후보 수집은 위 흐름 중 LH 목록, 상세, PDF URL 추출, dedupe 판단까지만 수행한다. `PdfParsingService.parse()`와 Gemini 호출은 선택 import 또는 force reparse 단계에서만 실행한다.

## 4. 저장 구조

| 테이블/엔티티 | 역할 |
|---|---|
| `announcement` | 정식 공고 parent. `source_primary + source_notice_id` unique constraint로 LH `PAN_ID` 기준 중복 방지 |
| `announcement_import_fingerprint` | 정식 공고의 list/detail/PDF/Gemini hash와 parse status 저장 |
| `lh_import_candidate` | 관리자 선택 import 전 후보 staging. list/detail raw JSON, hash, PDF URL, dedupe 상태 저장 |
| `announcement_parse_raw` | 정식 공고 저장 후 raw 보관. 후보 staging 용도로 사용하지 않음 |
| `announcement_unit` | PDF/LH 단위 row 저장. admin review와 DB 검증 기준으로 사용 |

## 5. dedupe decision

| decision | 의미 | Gemini 호출 |
|---|---|---|
| `NEW` | 기존 fingerprint 없음 | 호출 |
| `UNCHANGED_SKIP_GEMINI` | list/detail hash와 PDF URL이 기존 성공 파싱과 같음 | 호출 안 함 |
| `CHANGED_REPARSE` | list/detail/PDF URL 중 하나가 바뀜 | 호출 |
| `FAILED_RETRY` | 이전 파싱 실패 상태라 재시도 필요 | 호출 |
| `FORCE_REPARSE` | 관리자가 강제 재파싱 요청 | 호출 |
| `LAND_SKIP` | 토지 공고라 서비스 대상 제외 | 호출 안 함 |
| `NO_PDF` | 상세에서 PDF URL을 찾지 못함 | 호출 안 함 |

JSON hash는 object key 순서 차이에 흔들리지 않도록 canonical JSON을 만든 뒤 SHA-256으로 계산한다.

## 6. scheduler 현재 기준

| 항목 | 현재 값 |
|---|---|
| 실행 시각 | 매일 02:00, `@Scheduled(cron = "0 0 2 * * *")` |
| page 범위 | 1~10 |
| size | 100 |
| force 여부 | `false` |
| 중단 조건 | LH 목록이 비었거나 `endOfList=true`일 때만 중단 |

변경 없는 공고만 있는 페이지는 `imported=0`, `failed=0`이 될 수 있지만 더 이상 page loop를 멈추지 않는다.

## 7. public API 기준

이번 LH import/dedupe/admin API 작업은 public announcement endpoint 계약을 바꾸지 않는다.

- public detail은 summary/detail 중심 응답을 유지한다.
- admin-only raw/unit 검수 필드(`rawText`, `sourceUnitKey`, `unitSource`, `matchSource`, `confidenceLevel`)는 public detail에 노출하지 않는다.
- unit row count 검증은 DB `announcement_unit`과 admin detail `units.length` 기준으로 한다.

## 8. 검증 기준

자동 검증은 mock 기반 테스트와 Gradle build로 수행한다.

| 검증 | 기준 |
|---|---|
| targeted regression | legacy import, dedupe, scheduler, admin candidate API, DTO JSON 테스트 통과 |
| full test | `cd subscrition; .\gradlew test` 성공 |
| build | `cd subscrition; .\gradlew build` 성공 |
| live LH/Gemini | quota 방지를 위해 자동 검증에서는 생략. Apidog 수동 테스트 시 작은 `size` 사용 |
