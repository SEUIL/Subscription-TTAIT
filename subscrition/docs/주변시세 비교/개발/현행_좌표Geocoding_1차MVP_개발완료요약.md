# [현행] 좌표/Geocoding 1차 MVP 개발 완료 요약

<!-- markdownlint-disable MD013 -->

## 1. 문서 목적

이 문서는 주변시세 비교 기능의 1차 MVP 중 `AnnouncementUnit` 좌표 저장과 Naver Geocoding 보강 작업의 개발 완료 내용을 정리한다.

남은 후속 작업은 `subscrition/docs/주변시세 비교/기획+설계/현행_구현순차내역.md`의 `후속 순서`를 기준으로 본다. 이 문서에서는 후속 backlog를 중복 정리하지 않고, 이번 개발에서 완료된 범위와 검증 결과만 기록한다.

## 2. 완료 범위

| 영역 | 완료 내용 |
| - | - |
| 좌표 기준 단위 | `AnnouncementUnit`을 좌표 저장 기준으로 확정 |
| 좌표 필드 | `latitude`, `longitude` 추가 |
| Geocoding 상태 | `GeocodeStatus`, `geocodeMessage`, `geocodedAt` 추가 |
| 상태 전이 | 성공, 결과 없음, 실패, 주소 없음 skip 상태 처리 |
| Naver 응답 처리 | `x=longitude`, `y=latitude` 매핑을 typed result/adapter로 분리 |
| import 연결 | LH 상세 저장 후 신규 unit 대상 Geocoding 보강 실행 |
| 실패 격리 | Naver/API/runtime 실패가 import 성공 처리를 깨지 않도록 best-effort 처리 |
| 조회 응답 | public/admin unit DTO에 좌표와 Geocoding 상태 노출 |
| tests-after | domain, repository, adapter, enrichment, orchestrator, DTO 테스트 추가 |
| evidence | task 6/7 Gradle test/build evidence 기록 |

## 3. 주요 구현 파일

| 구분 | 파일 |
| - | - |
| domain | `subscrition/src/main/java/com/ttait/subscription/announcement/domain/AnnouncementUnit.java` |
| domain enum | `subscrition/src/main/java/com/ttait/subscription/announcement/domain/GeocodeStatus.java` |
| repository | `subscrition/src/main/java/com/ttait/subscription/announcement/repository/AnnouncementUnitRepository.java` |
| Naver client | `subscrition/src/main/java/com/ttait/subscription/external/naver/NaverGeocodingClient.java` |
| Naver adapter | `subscrition/src/main/java/com/ttait/subscription/external/naver/NaverGeocodingResponseAdapter.java` |
| Naver result | `subscrition/src/main/java/com/ttait/subscription/external/naver/NaverGeocodingResult.java` |
| enrichment | `subscrition/src/main/java/com/ttait/subscription/external/service/AnnouncementUnitGeocodingEnrichmentService.java` |
| import wiring | `subscrition/src/main/java/com/ttait/subscription/external/service/NoticeImportOrchestrator.java` |
| public DTO | `subscrition/src/main/java/com/ttait/subscription/announcement/dto/AnnouncementUnitResponse.java` |
| admin DTO | `subscrition/src/main/java/com/ttait/subscription/admin/dto/AdminAnnouncementUnitResponse.java` |

## 4. 주요 테스트 파일

| 검증 대상 | 파일 |
| - | - |
| domain 상태 전이 | `subscrition/src/test/java/com/ttait/subscription/announcement/domain/AnnouncementUnitTest.java` |
| repository 조회 | `subscrition/src/test/java/com/ttait/subscription/announcement/repository/AnnouncementUnitRepositoryTest.java` |
| Naver 응답 매핑 | `subscrition/src/test/java/com/ttait/subscription/external/naver/NaverGeocodingResponseAdapterTest.java` |
| enrichment 동작 | `subscrition/src/test/java/com/ttait/subscription/external/service/AnnouncementUnitGeocodingEnrichmentServiceTest.java` |
| import/reimport 연결 | `subscrition/src/test/java/com/ttait/subscription/external/service/NoticeImportOrchestratorTest.java` |
| public DTO | `subscrition/src/test/java/com/ttait/subscription/announcement/dto/AnnouncementUnitResponseTest.java` |
| admin DTO | `subscrition/src/test/java/com/ttait/subscription/admin/dto/AdminAnnouncementUnitResponseTest.java` |

## 5. 검증 결과

| 검증 | 결과 | 비고 |
| - | - | - |
| focused orchestrator test | 통과 | import/reimport Geocoding wiring 검증 |
| external package test | 통과 | Naver adapter와 enrichment failure isolation 검증 |
| `cd subscrition; .\gradlew test` | 통과 | 전체 test suite |
| `cd subscrition; .\gradlew build` | 통과 | 전체 build |
| runtime smoke test | 통과 | local profile boot 후 `/api/dev/naver-geocode` HTTP 200 확인 |
| Apidog 수동 테스트 | 통과 | legacy LH import 후 admin review detail에서 unit 좌표/status 저장 확인 |
| Java LSP | 미실행 | `jdtls` 미설치로 Gradle 검증으로 대체 |

Evidence 파일:

- `.sisyphus/evidence/task-6-tests-after.txt`
- `.sisyphus/evidence/task-6-tests-after-error.txt`
- `.sisyphus/evidence/task-7-gradle-test.txt`
- `.sisyphus/evidence/task-7-gradle-build.txt`

## 6. 현재 제외 범위

이번 개발은 좌표와 Geocoding 기반 구축까지만 완료했다. 아래 항목은 현재 branch 구현 범위가 아니다.

| 항목 | 상태 |
| - | - |
| 법정동 코드, `LAWD_CD` | 후속 작업 |
| RTMS 실거래가 수집 | 후속 작업 |
| `market snapshot` 저장 | 후속 작업 |
| 주변시세 비교 API | 후속 작업 |
| Dynamic Map과 marker 표시 | 후속 작업 |
| 공고 상세 지도, 리스트, 요약 연결 | 후속 작업 |

후속 순서와 상세 내용은 `subscrition/docs/주변시세 비교/기획+설계/현행_구현순차내역.md`를 기준으로 확인한다.

## 7. Apidog 수동 테스트 완료 결과

2026-05-21 local 환경에서 DB reset 후 관리자 계정만 생성한 상태로 legacy LH import 경로를 사용해 수동 검증했다.

검증 흐름:

1. `POST /api/admin/import/lh?page=1&size=3`로 LH 공고 import.
2. `GET /api/admin/review?page=0&size=50`로 실제 `announcementId` 확인.
3. `GET /api/admin/review/{announcementId}`로 unit 좌표와 Geocoding 상태 확인.

통과 사례:

| 항목 | 확인값 |
| - | - |
| `announcementId` | `6` |
| 공고명 | `[경기북부] 26년 1차 김포시 분양전환형 든든전세주택 입주자 모집공고(기존임차인)` |
| `unitId` | `2` |
| `fullAddress` | `김포시 마산동` |
| `geocodeStatus` | `SUCCESS` |
| `latitude` | `37.6436520` |
| `longitude` | `126.6412840` |
| `geocodedAt` | `2026-05-21T11:17:51.116245` |
| `reviewStatus` | `PENDING` |

따라서 `LH import -> AnnouncementUnit 저장 -> Naver Geocoding 보강 -> admin review detail 좌표/status 노출` 흐름은 1차 MVP 기준으로 정상 동작 확인 완료로 본다.

## 8. 남은 확인 사항

- `units=[]` 공고는 import 실패가 아니라 단위 row 미추출 공고로 분류한다. 이번 좌표/Geocoding MVP 검증 대상에서는 제외하고 후속 backlog로 넘긴다.
- `fullAddress`가 `김포시 마산동`처럼 행정동 수준이면 좌표 저장 검증은 통과지만, 정확한 단지 위치 검증은 아직 아니다.
- 운영 반영 전 Naver 환경 변수와 DB schema 반영 방식을 확인해야 한다.
- 후속 구현의 첫 단계는 `법정동 코드와 주소 정규화` 및 `unit 미추출 공고 처리 정책` 확정이다.
