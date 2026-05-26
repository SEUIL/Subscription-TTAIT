# [이전] 좌표/Geocoding 1차 MVP Apidog 수동 테스트 가이드

<!-- markdownlint-disable MD013 MD024 -->

## 1. 문서 목적

이 문서는 주변시세 비교 1차 MVP에서 완료한 `AnnouncementUnit` 좌표 저장과 Naver Geocoding 보강 기능을 Apidog로 수동 검증하기 위한 테스트 가이드다.

테스트 목표는 아래 네 가지다.

1. Naver Geocoding API 연동이 local profile에서 동작하는지 확인한다.
2. LH import 또는 reimport 이후 `AnnouncementUnit` 단위로 좌표와 Geocoding 상태가 저장되는지 확인한다.
3. 관리자 검수 상세 응답에서 unit별 `latitude`, `longitude`, `geocodeStatus`, `geocodedAt`, `geocodeMessage`가 확인되는지 검증한다.
4. public/admin 응답 노출 범위가 의도와 맞는지 확인한다.

## 2. 현재 MVP 테스트 범위

| 구분 | 테스트 포함 여부 | 확인 방식 |
| - | - | - |
| Naver Geocoding 단건 호출 | 포함 | `/api/dev/naver-geocode` |
| import 후 Geocoding 보강 | 포함 | 관리자 import API + admin review detail |
| reimport 후 Geocoding 보강 | 포함 | force reparse API + admin review detail |
| 좌표/status DB 저장 | 포함 | admin review detail 또는 DB 직접 확인 |
| public DTO 좌표/status 노출 | 부분 확인 | 현재 public detail은 unit 배열을 직접 내려주지 않으므로 자동 테스트 기준 확인 |
| Dynamic Map, marker 표시 | 제외 | 후속 UI 작업 |
| RTMS, 법정동 코드, 주변시세 비교 | 제외 | 후속 sprint 작업 |

## 3. 사전 준비

### 3.1 Backend 실행

local profile로 실행한다.

```powershell
cd subscrition
.\gradlew bootRun --args="--spring.profiles.active=local"
```

실행 로그에서 아래를 확인한다.

- active profile이 `local`인지 확인
- Tomcat port가 `8080`인지 확인
- application startup failure가 없는지 확인

### 3.2 환경 변수와 외부 설정

Naver Geocoding 단건 테스트와 import 보강 테스트는 local 환경에 아래 값이 있어야 한다.

| 설정 | 목적 |
| - | - |
| `NAVER_MAPS_CLIENT_ID` | Naver Geocoding 요청 client id |
| `NAVER_MAPS_CLIENT_SECRET` | Naver Geocoding 요청 secret |
| DB 접속 정보 | import 결과와 unit row 저장 확인 |
| Gemini/LH 관련 설정 | 실제 LH import/reimport 경로 확인 시 필요 |

문서나 Apidog example에는 실제 secret 값을 기록하지 않는다.

### 3.3 Apidog Environment 변수

Apidog에 environment를 하나 만든다.

| 변수명 | 예시값 | 설명 |
| - | - | - |
| `baseUrl` | `http://localhost:8080` | local backend base URL |
| `adminToken` | 직접 입력 | ADMIN JWT access token |
| `announcementId` | 테스트 중 저장 | import 또는 목록에서 확인한 공고 ID |
| `candidateId` | 테스트 중 저장 | LH 후보 선택 import 시 사용할 candidate ID |
| `geocodeQuery` | `서울특별시 강남구 학동로 426` | 단건 Geocoding 확인 주소 |

관리자 API는 `Authorization: Bearer {{adminToken}}` 헤더가 필요하다.

## 4. 전체 테스트 흐름

Apidog request group은 아래 순서로 만든다.

| 순서 | 그룹 | 목적 |
| - | - | - |
| 1 | Health / Startup | backend와 local profile 동작 확인 |
| 2 | Dev Geocoding | Naver Geocoding 단건 호출 확인 |
| 3 | Admin Import | legacy import 또는 LH 후보 수집/선택 import 확인 |
| 4 | Admin Review Detail | unit별 좌표/status 응답 확인 |
| 5 | Negative / Edge | 결과 없음, 잘못된 주소, 인증 실패 등 확인 |
| 6 | DB Cross-check | 필요 시 `announcement_unit` 직접 확인 |

## 5. 시나리오 1: Backend와 Swagger 접근 확인

### API

```http
GET {{baseUrl}}/swagger-ui/index.html
```

### 기대 결과

- HTTP `200` 또는 Swagger UI HTML 응답
- 실패하면 backend 실행, profile, port, security 설정을 먼저 확인한다.

## 6. 시나리오 2: Naver Geocoding 단건 정상 호출

### API

```http
GET {{baseUrl}}/api/dev/naver-geocode?query={{geocodeQuery}}
```

`/api/dev/**`는 `SecurityConfig`에서 GET 공개 경로다. 별도 JWT 없이 호출 가능하다.

### 기대 응답 예시

```json
{
  "status": "SUCCESS",
  "latitude": 37.517305,
  "longitude": 127.047502,
  "message": null
}
```

### 체크 포인트

- `status`가 `SUCCESS`인지 확인한다.
- `latitude`와 `longitude`가 `null`이 아닌지 확인한다.
- Naver 응답의 `x`는 `longitude`, `y`는 `latitude`로 저장되어야 한다.
- `message`는 성공 시 `null`일 수 있다.

## 7. 시나리오 3: Naver Geocoding 결과 없음 또는 실패 확인

### API

```http
GET {{baseUrl}}/api/dev/naver-geocode?query=존재하지않는주소테스트값12345
```

### 기대 결과

아래 둘 중 하나로 판단한다.

| 상태 | 의미 |
| - | - |
| `NO_RESULT` | Naver 응답에 주소 결과가 없음 |
| `FAILED` | Naver 응답 파싱 실패, API 예외, 좌표 누락/형식 오류 |

### 체크 포인트

- 실패 케이스에서도 backend가 500으로 죽는지보다 응답 body의 `status`, `message`를 먼저 확인한다.
- 실제 외부 API 정책상 입력값과 시점에 따라 `NO_RESULT`와 `FAILED`는 달라질 수 있다.
- secret 값이 응답에 노출되면 안 된다.

## 8. 시나리오 4: 관리자 인증 준비

관리자 API는 `/api/admin/**` 경로이며 `ADMIN` 권한 JWT가 필요하다.

### 로그인 API

```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "loginId": "관리자_ID",
  "password": "관리자_비밀번호"
}
```

### 기대 응답

```json
{
  "userId": 1,
  "loginId": "admin",
  "accessToken": "JWT_ACCESS_TOKEN",
  "profileCompleted": true,
  "role": "ADMIN"
}
```

### Apidog 처리

- 응답의 `accessToken`을 `adminToken` environment 변수에 저장한다.
- 이후 관리자 API에는 아래 header를 붙인다.

```http
Authorization: Bearer {{adminToken}}
```

관리자 계정이 없으면 DB에 이미 존재하는 ADMIN 계정 또는 팀에서 사용하는 local admin 계정을 사용한다. 임의 계정 생성으로 ADMIN 권한이 자동 부여되는지는 별도 확인이 필요하다.

## 9. 시나리오 5: LH import 실행

### 권장 흐름

DB를 reset하고 관리자 계정만 만든 local 수동 테스트에서는 legacy import를 우선 사용한다.
이 경로는 후보 staging을 거치지 않고 LH 목록, 상세, PDF AI 파싱, 정식 DB 저장을 한 번에 실행하므로 좌표/Geocoding MVP 확인에 더 단순하다.

```http
POST {{baseUrl}}/api/admin/import/lh?page=1&size=3
Authorization: Bearer {{adminToken}}
```

기대 응답:

```json
{
  "imported": 1,
  "failed": 0
}
```

legacy import 응답은 내부 상세 카운터를 노출하지 않는다. 따라서 실제 검증은 이후 `GET /api/admin/review`와 DB cross-check로 수행한다.

### 후보 선택 흐름

후보를 먼저 보고 선택 import해야 하는 운영 관리자 흐름을 확인할 때는 아래 후보 수집 API를 사용한다.

### API

```http
POST {{baseUrl}}/api/admin/import/lh/candidates/collect?page=1&size=10
Authorization: Bearer {{adminToken}}
```

### 기대 결과

- HTTP `200`
- LH 후보 수집 결과가 반환된다.
- 응답에서 후보 ID를 확인해 `candidateId`로 저장한다.

외부 LH API 설정 또는 네트워크 문제로 실패하면 이 단계는 환경 blocker로 기록한다.

## 10. 시나리오 6: 선택 후보 import 실행

### API

```http
POST {{baseUrl}}/api/admin/import/lh/selected
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "candidateIds": [{{candidateId}}],
  "force": false
}
```

### 기대 응답

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

### 체크 포인트

- `failed`가 `0`인지 확인한다.
- 신규 또는 변경 공고라면 `imported` 또는 `reparsed`가 증가할 수 있다.
- 같은 공고를 반복 실행하면 dedupe 정책에 따라 `unchanged`, `geminiSkipped`가 증가할 수 있다.
- import 성공 후 내부에서 Geocoding enrichment가 best-effort로 실행된다.
- 좌표/Geocoding MVP만 빠르게 확인할 때는 legacy import 흐름을 우선 사용해도 된다.

## 11. 시나리오 7: 관리자 검수 상세에서 unit 좌표/status 확인

### API

```http
GET {{baseUrl}}/api/admin/review/{{announcementId}}
Authorization: Bearer {{adminToken}}
```

`announcementId`는 import 결과, 관리자 검수 목록, DB, 또는 Location/목록 응답에서 확인한다.

### 기대 응답 중 확인할 필드

```json
{
  "announcementId": 1,
  "units": [
    {
      "unitId": 10,
      "fullAddress": "서울특별시 강남구 학동로 426",
      "latitude": 37.517305,
      "longitude": 127.047502,
      "geocodeStatus": "SUCCESS",
      "geocodedAt": "2026-05-20T18:00:00",
      "geocodeMessage": null
    }
  ]
}
```

### 체크 포인트

| 필드 | 기대값 |
| - | - |
| `units` | 1개 이상이면 unit 단위 확인 가능 |
| `units[].fullAddress` | 공백이 아닌 주소면 Geocoding 대상 |
| `units[].latitude` | `SUCCESS`일 때 `null` 아님 |
| `units[].longitude` | `SUCCESS`일 때 `null` 아님 |
| `units[].geocodeStatus` | `SUCCESS`, `NO_RESULT`, `FAILED`, `SKIPPED_NO_ADDRESS`, `NOT_REQUESTED` 중 하나 |
| `units[].geocodedAt` | Geocoding 처리 후 `null`이 아니어야 함 |
| `units[].geocodeMessage` | admin 응답에서만 확인 가능 |

`geocodeStatus`가 `NOT_REQUESTED`로 남아 있으면 import 후 enrichment가 실행되지 않았거나, 해당 row가 아직 처리 대상에 들어가지 않은 상태다.

## 12. 시나리오 8: force reparse 후 Geocoding 재보강 확인

### API

```http
POST {{baseUrl}}/api/admin/import/lh/{{announcementId}}/force-reparse
Authorization: Bearer {{adminToken}}
```

### 기대 결과

- HTTP `200`
- `failed`가 `0`인지 확인한다.
- reimport는 unit을 delete-and-replace하므로, 이후 admin review detail에서 새 unit row의 Geocoding 상태를 다시 확인한다.

### 후속 확인

```http
GET {{baseUrl}}/api/admin/review/{{announcementId}}
Authorization: Bearer {{adminToken}}
```

확인할 것:

- `units[].geocodeStatus`가 `SUCCESS`, `NO_RESULT`, `FAILED`, `SKIPPED_NO_ADDRESS` 중 하나로 처리됐는지 확인한다.
- `NOT_REQUESTED`가 남아 있으면 reimport 후 enrichment 호출 여부를 로그/DB로 확인한다.
- reimport 중 Geocoding 실패가 있어도 import 자체가 실패 처리되지 않아야 한다.

## 13. 시나리오 9: public 조회 응답 경계 확인

### API

```http
GET {{baseUrl}}/api/announcements/{{announcementId}}
```

### 현재 주의점

현재 public detail 응답인 `AnnouncementDetailResponse`는 unit 배열을 직접 포함하지 않는다. 따라서 public API만으로 unit별 `latitude`, `longitude`, `geocodeStatus`를 확인하는 흐름은 제한적이다.

이번 MVP에서 public unit DTO 매핑은 자동 테스트로 검증되어 있다. Apidog 수동 테스트에서는 unit별 좌표/status 확인 기준을 `GET /api/admin/review/{announcementId}`로 둔다.

## 14. 시나리오 10: DB cross-check

Apidog 응답만으로 판단이 애매하면 DB에서 `announcement_unit`을 직접 확인한다.

예시 SQL:

```sql
select
    id,
    announcement_id,
    full_address,
    latitude,
    longitude,
    geocode_status,
    geocode_message,
    geocoded_at,
    deleted
from announcement_unit
where announcement_id = {announcementId}
order by unit_order asc;
```

확인 기준:

- active row는 `deleted = false`여야 한다.
- 주소가 있는 row는 Geocoding 처리 후 `NOT_REQUESTED`에 머물면 안 된다.
- `SUCCESS` row는 `latitude`, `longitude`, `geocoded_at`이 있어야 한다.
- `NO_RESULT`, `FAILED`, `SKIPPED_NO_ADDRESS` row는 좌표가 `null`일 수 있다.

좌표 성공 검증용 공고를 찾는 SQL:

```sql
select
    a.id as announcement_id,
    a.notice_name,
    e.review_status,
    count(u.id) as unit_count,
    sum(case when u.full_address is not null and trim(u.full_address) <> '' then 1 else 0 end) as address_unit_count,
    sum(case when u.geocode_status = 'SUCCESS'
              and u.latitude is not null
              and u.longitude is not null
              and u.geocoded_at is not null
             then 1 else 0 end) as success_geocode_count
from announcement a
join announcement_eligibility e
  on e.announcement_id = a.id
join announcement_unit u
  on u.announcement_id = a.id
 and u.deleted = false
where a.deleted = false
group by a.id, a.notice_name, e.review_status
having unit_count > 0
   and address_unit_count > 0
   and success_geocode_count > 0
order by a.id desc;
```

`units=[]` 공고는 import 실패가 아니라 단위 row 미추출 공고다. 이번 좌표/Geocoding MVP 수동 검증 대상에서는 제외하고, 다음 스프린트에서 재파싱/수동 보완/비교 제외 정책을 정한다.

## 15. 단위 테스트식 체크리스트

Apidog에서 각 요청을 실행한 뒤 아래 항목을 수동 assertion처럼 확인한다.

### 15.1 Naver 응답 매핑

| 케이스 | 입력 | 기대 결과 |
| - | - | - |
| 정상 주소 | 실제 주소 | `status=SUCCESS`, `longitude`, `latitude` 존재 |
| 없는 주소 | 임의 문자열 | `status=NO_RESULT` 또는 `FAILED` |
| API 설정 문제 | Naver env 누락/오류 | secret 노출 없이 실패 메시지 확인 |

### 15.2 import/reimport 보강

| 케이스 | 실행 | 기대 결과 |
| - | - | - |
| 신규 import | selected import | import 성공 후 unit Geocoding 상태 변경 |
| reimport | force reparse | unit 재생성 후 Geocoding 상태 재적용 |
| Geocoding 실패 | 외부 API 실패 또는 결과 없음 | import `failed`로 전파되지 않음 |

### 15.3 DTO 노출

| 응답 | 포함 | 제외 |
| - | - | - |
| admin review detail `units[]` | `latitude`, `longitude`, `geocodeStatus`, `geocodedAt`, `geocodeMessage` | Naver raw payload, secret |
| public unit DTO 자동 테스트 기준 | `latitude`, `longitude`, `geocodeStatus`, `geocodedAt` | `geocodeMessage`, raw/source review field |

## 16. 실패 시 분기 기준

| 증상 | 우선 확인 |
| - | - |
| `/api/dev/naver-geocode` 401/403 | local profile, SecurityConfig, URL method가 GET인지 확인 |
| `/api/dev/naver-geocode` 500 | Naver env, client id/secret, 네트워크, Naver 콘솔 설정 확인 |
| admin API 401 | `Authorization: Bearer {{adminToken}}` 누락 또는 만료 |
| admin API 403 | token role이 `ADMIN`인지 확인 |
| import `failed > 0` | LH/Gemini/PDF 파싱 실패인지, Geocoding 실패인지 로그로 구분 |
| `units=[]` | 단위 row 미추출 공고. 이번 MVP 검증 대상에서 제외하고 후속 backlog로 분류 |
| unit이 `NOT_REQUESTED` | enrichment 호출 여부, 해당 unit의 `fullAddress`, repository 대상 조건 확인 |
| `SUCCESS`인데 좌표가 null | bug 가능성이 높음. 저장 로직과 DB schema 확인 |
| 좌표가 반대로 보임 | Naver `x=longitude`, `y=latitude` 매핑 확인 |

## 17. Apidog 정리 방식 권장

Apidog collection은 아래처럼 구성한다.

```text
주변시세 좌표/Geocoding 1차 MVP
├─ 00. Environment Check
│  └─ Swagger UI 확인
├─ 01. Dev Geocoding
│  ├─ 정상 주소 Geocoding
│  └─ 결과 없음/실패 Geocoding
├─ 02. Auth
│  └─ Admin Login
├─ 03. LH Import
│  ├─ legacy import
│  ├─ 후보 수집
│  ├─ 선택 import
│  └─ force reparse
├─ 04. Review Detail
│  └─ unit 좌표/status 확인
└─ 05. Negative
   ├─ admin token 없음
   ├─ admin 권한 없음
   └─ 잘못된 announcementId
```

각 request에는 `Description`에 아래를 남긴다.

- 목적
- 사전 조건
- 기대 status code
- 반드시 볼 response field
- 실패 시 확인할 로그/DB 포인트

## 18. 최종 통과 기준

아래 조건을 모두 만족하면 Apidog 수동 검증을 통과로 본다.

- local profile backend가 정상 기동한다.
- `/api/dev/naver-geocode` 정상 주소 호출이 HTTP `200`과 `SUCCESS` 좌표 응답을 반환한다.
- 관리자 import 또는 force reparse가 HTTP `200`으로 완료되고 import `failed`가 `0`이다.
- `GET /api/admin/review/{announcementId}`의 `units[]`에서 좌표와 Geocoding 상태를 확인할 수 있다.
- Geocoding 실패가 import 전체 실패로 전파되지 않는다.
- 실패/결과 없음 케이스에서 secret이나 raw external payload가 노출되지 않는다.

### 18.1 2026-05-21 완료 기록

DB reset 후 관리자 계정만 생성한 local 환경에서 legacy import로 수동 검증을 완료했다.

| 항목 | 확인값 |
| - | - |
| import 경로 | `POST /api/admin/import/lh?page=1&size=3` |
| 확인 API | `GET /api/admin/review/6` |
| `announcementId` | `6` |
| `unitId` | `2` |
| `fullAddress` | `김포시 마산동` |
| `geocodeStatus` | `SUCCESS` |
| `latitude` | `37.6436520` |
| `longitude` | `126.6412840` |
| `geocodedAt` | `2026-05-21T11:17:51.116245` |
| `reviewStatus` | `PENDING` |

위 결과로 `LH import -> AnnouncementUnit 저장 -> Naver Geocoding 보강 -> admin review detail 좌표/status 노출` 흐름은 1차 MVP 기준 통과로 본다.
