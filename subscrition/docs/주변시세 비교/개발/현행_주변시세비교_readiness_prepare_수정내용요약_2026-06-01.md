# [현행] 주변시세 비교 readiness/prepare 수정 내용 요약 2026-06-01

<!-- markdownlint-disable MD013 -->

## 1. 문서 목적

이 문서는 주변시세 비교 backend readiness/prepare 작업에서 수정한 내용만 별도로 정리한다.

테스트 실행 내역과 local smoke 결과는 `subscrition/docs/주변시세 비교/테스트/현행_2026-06-01_readiness_prepare_E2E_작업노트.md`를 기준으로 본다.

## 2. 수정 배경

주변시세 비교 public API와 RTMS/snapshot batch API는 이미 있었지만, 운영자가 특정 공고가 왜 비교 불가 상태인지 한 번에 확인하거나 공고 단위로 준비 작업을 실행하는 진입점이 부족했다.

이번 수정은 admin 화면 또는 운영자가 다음 흐름을 명확히 실행할 수 있게 하는 것이 목적이다.

```text
공고 unit 상태 확인
→ lawdCd/면적/snapshot blocker 확인
→ 필요한 unit만 RTMS 수집 + snapshot 집계 실행
→ public market-comparison API에서 비교 결과 확인
```

## 3. API 추가

### 3.1 readiness 조회 API

```http
GET /api/admin/market/announcements/{announcementId}/readiness?sourceType=APT_RENT&dealYmFrom=YYYYMM&dealYmTo=YYYYMM
Authorization: Bearer {ADMIN_TOKEN}
```

역할:

- 공고에 속한 unit별 주변시세 준비 상태를 read-only로 반환한다.
- RTMS 수집이나 snapshot 집계는 실행하지 않는다.
- 운영자가 blocker를 먼저 확인할 수 있게 한다.

unit별 blocker:

| blocker | 의미 |
| - | - |
| `UNIT_LAWD_CD_MISSING` | unit의 `lawdCd`가 없어 법정동 seed/import 또는 normalize가 필요함 |
| `UNIT_AREA_MISSING` | unit의 `exclusiveAreaValue`가 없어 면적 기준 snapshot 조회 불가 |
| `SNAPSHOT_NOT_FOUND` | unit 조건에 맞는 snapshot 없음 |
| `INSUFFICIENT_DATA` | snapshot은 있으나 sample 기준 미달 |
| `READY` | public comparison 가능 상태 |

응답에는 `rtmsServiceKeyConfigured` boolean만 포함한다. 실제 service key 값은 응답, 로그, 문서에 남기지 않는다.

### 3.2 announcement 단위 prepare API

```http
POST /api/admin/market/announcements/{announcementId}/prepare
Authorization: Bearer {ADMIN_TOKEN}
Content-Type: application/json
```

요청 핵심 필드:

| 필드 | 역할 |
| - | - |
| `sourceType` | RTMS source type. 기본 사용값은 `APT_RENT` |
| `dealYm` | RTMS 수집 대상 월 |
| `dealYmFrom`, `dealYmTo` | snapshot 집계 기간 |
| `numOfRows`, `maxPages` | RTMS pagination 제한 |
| `minimumSampleCount` | snapshot OK/INSUFFICIENT_DATA 판단 기준 |
| `retryNoLawdCode` | `NO_LAWD_CODE` unit 재정규화 여부 |

처리 흐름:

```text
요청 검증
→ 선택적으로 normalize-units 실행
→ unit별 lawdCd/exclusiveAreaValue 확인
→ 준비 가능한 unit만 batch request로 변환
→ 기존 rtms-snapshot batch service 재사용
→ unit별 skipped/queued 상태와 batch 결과 반환
```

현재 prepare API는 synchronous 방식이다. scheduler, async job id, progress table, polling endpoint는 추가하지 않았다.

## 4. 응답 필드 추가

`AdminAnnouncementUnitResponse`에 주변시세 준비 상태를 확인하기 위한 필드를 추가했다.

| 필드 | 의미 |
| - | - |
| `legalDongCode` | 10자리 법정동 코드 |
| `lawdCd` | RTMS API 호출에 쓰는 5자리 지역 코드 |
| `addressStatus` | 주소 정규화 상태 |
| `addressMessage` | 주소 정규화 실패 또는 안내 메시지 |
| `addressNormalizedAt` | 주소 정규화 시각 |

이 필드는 admin unit 응답에만 추가한 운영/검수용 정보다.

## 5. 설정 수정

service key 설정을 환경변수 placeholder 기준으로 정리했다.

```properties
external.lh.service-key=${LH_SERVICE_KEY:}
rtms.service-key=${RTMS_SERVICE_KEY:}
```

수정 의도:

- LH service key와 RTMS service key를 분리한다.
- local/dev/prod 환경에서 `.env` 또는 배포 환경변수로 주입한다.
- key 값이 코드, 문서, 응답에 직접 남지 않게 한다.

## 6. 안정장치

prepare API에서 외부 RTMS 호출 범위를 제한했다.

| 제한 | 값 |
| - | - |
| 조회 월 range | 최대 12개월 |
| `numOfRows` | 1 이상 100 이하 |
| `maxPages` | 1 이상 10 이하 |
| batch request 수 | 최대 20개 |

없는 공고 ID는 `announcement not found`로 구분한다.

## 7. 주요 추가/수정 파일

| 구분 | 파일 |
| - | - |
| readiness controller | `src/main/java/com/ttait/subscription/admin/controller/AdminMarketReadinessController.java` |
| readiness service | `src/main/java/com/ttait/subscription/admin/service/AdminMarketReadinessService.java` |
| readiness DTO | `src/main/java/com/ttait/subscription/admin/dto/MarketReadinessResponse.java` |
| prepare controller | `src/main/java/com/ttait/subscription/admin/controller/AdminMarketPrepareController.java` |
| prepare service | `src/main/java/com/ttait/subscription/admin/service/AdminMarketPrepareService.java` |
| prepare request DTO | `src/main/java/com/ttait/subscription/admin/dto/MarketPrepareRequest.java` |
| prepare response DTO | `src/main/java/com/ttait/subscription/admin/dto/MarketPrepareResponse.java` |
| admin unit DTO | `src/main/java/com/ttait/subscription/admin/dto/AdminAnnouncementUnitResponse.java` |
| raw count query | `src/main/java/com/ttait/subscription/market/repository/MarketTransactionRawRepository.java` |
| 설정 | `src/main/resources/application.properties` |

## 8. 남은 범위

이번 수정에 포함하지 않은 항목은 다음과 같다.

- scheduler 기반 자동 RTMS/snapshot 준비.
- async job id, progress table, polling API.
- public API에서 자동으로 prepare를 trigger하는 흐름.
- 실제 `RTMS_SERVICE_KEY`가 있는 환경의 full RTMS 수집 검증.
- 운영용 법정동 seed 원본 파일 위치와 갱신 주기 확정.
