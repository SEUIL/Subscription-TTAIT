# NAVER Maps Geocoding 연동 및 테스트 가이드

## 현재 문서 범위

이 문서는 현재 1차 MVP branch에서 확인해야 할 Naver Geocoding 범위를 정리한다.
현재 deliverable은 `AnnouncementUnit` 좌표 저장, import 후 Geocoding 보강, 좌표/status 조회 노출,
그리고 관련 테스트 검증이다.

`Dynamic Map`, 지도 marker 이동, 프론트 지도 연결은 현재 branch의 완료 기준이 아니다.
해당 내용은 아래 `Future / Reference` 섹션에만 둔다.

## 현재 1차 MVP 범위

| 항목 | 현재 상태 | 범위 |
| - | - | - |
| 좌표 기준 단위 | `AnnouncementUnit` | 현재 MVP |
| 좌표 변환 | `Naver Geocoding` | 현재 MVP |
| 좌표 저장 | `latitude`, `longitude` | 현재 MVP |
| 상태 저장 | `geocodeStatus`, `geocodedAt` | 현재 MVP |
| 조회 응답 | public/admin unit DTO 좌표와 status 노출 | 현재 MVP |
| 지도 화면 | marker 표시와 이동 | Future / Reference |
| RTMS, market snapshot, 비교 계산 | 주변시세 비교 | Future / Reference |

## 현재 branch에서 중요한 파일

백엔드:

- `subscrition/src/main/java/com/ttait/subscription/external/naver/NaverMapProperties.java`
- `subscrition/src/main/java/com/ttait/subscription/external/naver/NaverMapConfig.java`
- `subscrition/src/main/java/com/ttait/subscription/external/naver/NaverGeocodingClient.java`
- `subscrition/src/main/java/com/ttait/subscription/dev/NaverGeocodeTestController.java`
- `subscrition/src/main/java/com/ttait/subscription/config/SecurityConfig.java`

템플릿:

- `subscrition/src/main/resources/templates/naver-geocode-test.html`

참고용 템플릿:

- `subscrition/src/main/resources/templates/naver-map-test.html`

`naver-map-test.html`은 Dynamic Map 동작을 확인하기 위한 참고 자료다.
현재 MVP 완료 기준에는 포함하지 않는다.

## Geocoding 동작 구조

흐름:

1. 신규 LH import가 완료된다.
2. import 성공 후 `AnnouncementUnit` 주소를 기준으로 Geocoding 보강을 실행한다.
3. 백엔드가 Naver Geocoding REST API를 호출한다.
4. 응답 JSON에서 `x`, `y`를 읽는다.
5. `AnnouncementUnit`에 좌표와 Geocoding 상태를 저장한다.

좌표 의미:

- `x`: 경도, `longitude`
- `y`: 위도, `latitude`

Naver 응답의 `x`를 `longitude`로 저장하고, `y`를 `latitude`로 저장해야 한다.
이 매핑은 테스트에서 반드시 확인할 핵심 포인트다.

## 현재 테스트 URL

로컬 백엔드가 떠 있는 상태에서 Geocoding 확인에 아래 URL을 사용한다.

- Geocoding 테스트 페이지: `http://localhost:8080/dev/naver-geocode`
- Geocoding API 직접 호출:

```text
http://localhost:8080/api/dev/naver-geocode?query=서울특별시 성동구 왕십리로 83-21
```

지도 테스트 URL인 `http://localhost:8080/dev/naver-map`은 Dynamic Map 참고용이다.
현재 branch의 완료 기준이나 주변시세 MVP 구현 범위로 보지 않는다.

## 설정 방식

이 프로젝트는 `application.properties`에서 placeholder를 참조하고,
실제 값은 로컬 설정 파일에 넣는 형태로 사용한다.

### application.properties

아래 키가 있어야 한다.

```properties
naver.maps.client-id=${NAVER_MAPS_CLIENT_ID}
naver.maps.client-secret=${NAVER_MAPS_CLIENT_SECRET}
```

### application-local.properties

실제 값은 로컬 개발 환경에서만 설정한다.
문서에는 실제 값을 기록하지 않는다.

```properties
NAVER_MAPS_CLIENT_ID=발급받은_Client_ID
NAVER_MAPS_CLIENT_SECRET=발급받은_Client_Secret
```

## 네이버 클라우드 콘솔 설정

현재 MVP에서 필요한 API는 `Geocoding`이다.

Maps Application에서 아래를 활성화한다.

- `Geocoding`

`Dynamic Map`은 지도 화면 확인이 필요할 때만 별도로 활성화한다.
현재 MVP 완료 기준에는 포함하지 않는다.

## SecurityConfig 반영 내용

로컬 테스트 페이지 접근을 위해 아래 GET 경로를 허용했다.

- `/dev/**`
- `/api/dev/**`

브라우저에서 로그인 없이 Geocoding 테스트 페이지를 열 수 있다.

## 사용 방법

### 1. 백엔드 실행

로컬 프로필로 실행한다.

```powershell
./gradlew.bat bootRun --args="--spring.profiles.active=local"
```

또는 IDE에서 `local` 프로필을 켠 상태로 실행해도 된다.

### 2. Geocoding 테스트

`http://localhost:8080/dev/naver-geocode` 접속 후 주소를 입력한다.

정상 응답 예시:

```json
{
  "addresses": [
    {
      "roadAddress": "서울특별시 성동구 왕십리로 83-21",
      "jibunAddress": "서울특별시 성동구 성수동1가 ...",
      "x": "127.0441234",
      "y": "37.5445678"
    }
  ]
}
```

확인 포인트:

- `addresses[0].x`가 경도, `longitude`
- `addresses[0].y`가 위도, `latitude`
- 좌표가 저장될 때 `x`와 `y`가 서로 바뀌지 않아야 함

## 꼭 알아둘 점

### 1. Geocoding만으로 지도가 뜨는 것은 아님

역할 분리:

- `Geocoding`: 주소를 좌표로 변환
- `Dynamic Map`: 지도를 화면에 표시

현재 MVP는 첫 번째 항목만 완료 기준에 포함한다.
지도 표시는 Future / Reference 범위다.

### 2. `Client Secret`은 프론트에 노출하면 안 됨

현재 구조는 아래 원칙을 따른다.

- 브라우저에는 `Client ID`만 전달
- `Client Secret`은 백엔드에서만 사용
- 문서, 로그, evidence에는 실제 값을 기록하지 않음

## 현재 구현에서 중요한 코드 포인트

### NaverGeocodingClient

역할:

- `maps.apigw.ntruss.com/map-geocode/v2/geocode` 호출
- 헤더에 `clientId`, `clientSecret` 포함
- Naver 응답의 `x`, `y`를 typed result로 변환

핵심 매핑:

```java
longitude = x
latitude = y
```

### NaverGeocodeTestController

역할:

- `/dev/naver-geocode` 템플릿 반환
- `/api/dev/naver-geocode` JSON 응답 반환

`/dev/naver-map` 반환 경로가 있더라도 현재 MVP 완료 기준에는 포함하지 않는다.

## Future / Reference

아래 항목은 현재 branch 구현 범위가 아니다.
후속 작업이나 참고 맥락으로만 본다.

- Dynamic Map 화면 표시
- 지도 marker 이동
- 공고 목록/상세 화면의 지도 연결
- RTMS 실거래가 수집
- 법정동 코드, `LAWD_CD` 매핑
- `market snapshot` 저장
- 주변시세 비교 계산

## 문제 발생 시 체크리스트

### Geocoding 응답이 JSON이 아닐 때

원인 후보:

- 인증키 문제
- 로컬 프로필 미적용
- 서버 예외 발생

현재 테스트 페이지는 실패 시 응답 본문을 그대로 보여주도록 처리되어 있다.

### 좌표가 반대로 저장된 것처럼 보일 때

확인 순서:

1. Naver 응답의 `x`를 `longitude`로 저장했는지 확인한다.
2. Naver 응답의 `y`를 `latitude`로 저장했는지 확인한다.
3. 테스트에서 `x=longitude`, `y=latitude` 매핑을 검증했는지 확인한다.

### 지도가 안 뜰 때

이 항목은 Future / Reference 범위다.
현재 MVP 검증 실패로 보지 않는다.
지도 화면을 별도로 확인할 때만 아래를 본다.

1. `Dynamic Map` 활성화 여부
2. Web 서비스 URL 등록 여부
3. 로컬 프로필 활성화 여부
4. `application.properties` placeholder 존재 여부
5. 로컬 설정의 실제 값 존재 여부
6. 백엔드 재시작 여부

## 한 줄 요약

현재 1차 MVP는 `AnnouncementUnit` 기준 좌표 저장과 Naver Geocoding 검증이다.
Dynamic Map, RTMS, market snapshot, 주변시세 비교 계산은 후속 범위다.
