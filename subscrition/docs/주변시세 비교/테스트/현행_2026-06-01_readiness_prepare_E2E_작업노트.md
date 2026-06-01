# [현행] readiness/prepare E2E 작업노트 2026-06-01

## 목적

주변시세 비교 UI/API가 실제 운영 데이터 준비 상태를 확인하고 준비 실행까지 이어질 수 있는지 local E2E로 검증한다.

## 테스트 전 구현 요약

- `AdminAnnouncementUnitResponse`에 주변시세 준비 필드 추가
  - `legalDongCode`, `lawdCd`, `addressStatus`, `addressMessage`, `addressNormalizedAt`
- read-only readiness API 추가
  - `GET /api/admin/market/announcements/{announcementId}/readiness`
  - unit별 blocker: `UNIT_LAWD_CD_MISSING`, `UNIT_AREA_MISSING`, `SNAPSHOT_NOT_FOUND`, `INSUFFICIENT_DATA`, `READY`
  - `rtmsServiceKeyConfigured`는 boolean만 반환하고 key 값은 반환하지 않음
- synchronous prepare API 추가
  - `POST /api/admin/market/announcements/{announcementId}/prepare`
  - 선택적으로 `normalize-units` 실행 후 기존 `rtms-snapshot` batch service 재사용
  - `numOfRows <= 100`, `maxPages <= 10`, batch count `<= 20`, 조회 기간 최대 12개월 제한
- 설정 정리
  - `external.lh.service-key=${LH_SERVICE_KEY:}`
  - `rtms.service-key=${RTMS_SERVICE_KEY:}`
- 문서 업데이트
  - `현행_주변시세비교_백엔드필요사항_정리.md`에 readiness/prepare 계약 반영

## 테스트 전 검증 완료

- `./gradlew test --tests "*AdminMarketReadinessServiceTest" --tests "*AdminMarketPrepareServiceTest"` 성공
- `./gradlew clean test` 성공
- `./gradlew build` 성공
- `git diff --check` 문제 없음
- service key 값은 노트/로그에 기록하지 않음

## E2E 계획

1. local MySQL 실행 확인
2. backend `bootRun` 실행
3. admin token 확보
4. 법정동 seed import
5. `normalize-units`
6. readiness API 확인
7. prepare API 실행
8. public market-comparison API 확인
9. 결과와 blocker를 이 노트 하단에 추가 기록

## E2E 결과

### 실행 환경

- 실행일: 2026-06-01
- Backend: local `./gradlew bootRun`, `http://localhost:8080`
- DB: local Docker MySQL `ttait_subscription`
- 테스트 fixture:
  - `source_notice_id=e2e-readiness-prepare-20260601`
  - `announcementId=1`
  - `unitId=1`
  - `sourceType=APT_RENT`
  - `dealYmFrom=202405`, `dealYmTo=202405`
  - `lawdCd=28237`, `exclusiveArea=59.84`
- `RTMS_SERVICE_KEY`: 미설정 확인. key 값은 기록하지 않음.

### 실행 결과

| 단계 | HTTP | 결과 | 비고 |
| - | -: | - | - |
| Swagger/backend surface | 200 | reachable | `/swagger-ui/index.html` 기준 |
| ADMIN login | 200 | token 발급 | token 값 미기록 |
| 법정동 mapping upsert | 200 | `requestedCount=1`, `updatedCount=1` | `부평구/부평동/2823710100` |
| normalize-units | 200 | `processedCount=1`, `successCount=1`, `noLawdCodeCount=0` | unit `lawdCd=28237` 설정 확인 |
| readiness | 200 | `readyUnitCount=1`, `blockedUnitCount=0`, blocker=`READY` | `rtmsServiceKeyConfigured=false`, snapshot=`OK` |
| prepare | 400 | `RTMS service key is missing` | 예상 blocker. 실제 RTMS 수집은 key 없어서 미실행 |
| public comparison | 200 | `status=COMPARABLE` | snapshot `sampleCount=1`, deposit/rent 비교값 반환 |

### 관찰 사항

- readiness API는 `RTMS_SERVICE_KEY` 미설정 상태에서도 read-only 상태 확인을 정상 반환했다.
- prepare API는 eligible unit이 있는 상태에서 RTMS client 진입 후 `RTMS service key is missing`을 400으로 반환했다.
- public comparison API는 준비된 snapshot fixture 기준 `COMPARABLE`을 반환했다.
- 최초 smoke에서 shell/terminal JSON 인코딩 경로 때문에 테스트 fixture 주소가 mojibake로 저장되어 normalize가 `NO_LAWD_CODE`가 됐다. Fixture 주소를 DB UTF-8 literal로 보정한 뒤 normalize/readiness/comparison은 통과했다.

### 남은 확인

- 실제 `RTMS_SERVICE_KEY`가 있는 환경에서 prepare가 RTMS 수집 + snapshot 집계까지 `SUCCESS`를 반환하는지 추가 확인 필요.
- 현재 local smoke는 최소 fixture 기반 검증이며, 실제 LH import 데이터 기반 full E2E는 별도 실행 필요.
