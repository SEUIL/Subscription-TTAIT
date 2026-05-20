# NAVER Maps 연동 및 테스트 가이드

## 개요

오늘 작업한 내용은 크게 2가지다.

1. 주소를 위도/경도로 변환하는 `Geocoding` 테스트
2. 변환된 좌표를 네이버 지도에 실제로 표시하는 `Dynamic Map` 테스트

현재 기준으로 로컬에서 두 기능 모두 확인 가능한 상태다.

## 이번에 추가한 파일

백엔드:

- `subscrition/src/main/java/com/ttait/subscription/external/naver/NaverMapProperties.java`
- `subscrition/src/main/java/com/ttait/subscription/external/naver/NaverMapConfig.java`
- `subscrition/src/main/java/com/ttait/subscription/external/naver/NaverGeocodingClient.java`
- `subscrition/src/main/java/com/ttait/subscription/dev/NaverGeocodeTestController.java`
- `subscrition/src/main/java/com/ttait/subscription/config/SecurityConfig.java`

템플릿:

- `subscrition/src/main/resources/templates/naver-geocode-test.html`
- `subscrition/src/main/resources/templates/naver-map-test.html`

## 동작 구조

### 1. Geocoding

흐름:

1. 사용자가 주소 입력
2. 백엔드가 Naver Geocoding REST API 호출
3. 응답 JSON에서 `x`, `y` 추출

의미:

- `x`: 경도
- `y`: 위도

### 2. Dynamic Map

흐름:

1. 서버가 Thymeleaf 템플릿에 `Client ID` 주입
2. 브라우저가 네이버 지도 JS SDK 로드
3. 기본 지도 표시
4. 주소 입력 시 `/api/dev/naver-geocode` 호출
5. 받은 좌표로 지도 중심 이동 + 마커 표시

## 현재 테스트 URL

로컬 백엔드가 떠 있는 상태에서 아래 URL로 확인한다.

- Geocoding 테스트: `http://localhost:8080/dev/naver-geocode`
- 지도 테스트: `http://localhost:8080/dev/naver-map`
- Geocoding API 직접 호출: `http://localhost:8080/api/dev/naver-geocode?query=서울특별시 성동구 왕십리로 83-21`

## 설정 방식

이 프로젝트는 `application.properties`에서 placeholder를 참조하고, 실제 값은 로컬 설정 파일에 넣는 형태로 사용한다.

### application.properties

아래 키가 있어야 한다.

```properties
naver.maps.client-id=${NAVER_MAPS_CLIENT_ID}
naver.maps.client-secret=${NAVER_MAPS_CLIENT_SECRET}
```

### application-local.properties

실제 값은 아래처럼 넣는다.

```properties
NAVER_MAPS_CLIENT_ID=발급받은_Client_ID
NAVER_MAPS_CLIENT_SECRET=발급받은_Client_Secret
```

## 네이버 클라우드 콘솔 설정

Maps Application에서 아래를 활성화해야 한다.

- `Dynamic Map`
- `Geocoding`

Web 서비스 URL은 로컬 테스트 기준으로 아래를 등록하면 된다.

- `http://localhost`
- `http://127.0.0.1`

참고:

- 네이버 공식 가이드는 서비스 URL에 포트나 URI를 제외하고 host만 등록하는 방식을 권장한다.
- 예: `http://localhost:8080/dev/naver-map` 대신 `http://localhost`

## SecurityConfig 반영 내용

로컬 테스트 페이지 접근을 위해 아래 GET 경로를 허용했다.

- `/dev/**`
- `/api/dev/**`

즉 브라우저에서 로그인 없이 테스트 페이지를 열 수 있다.

## 사용 방법

### 1. 백엔드 실행

로컬 프로필로 실행한다.

예시:

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

- `addresses[0].x`가 경도
- `addresses[0].y`가 위도

### 3. 지도 테스트

`http://localhost:8080/dev/naver-map` 접속 후:

1. 기본 지도가 뜨는지 확인
2. 주소 입력
3. 버튼 클릭
4. 마커가 해당 위치로 이동하는지 확인

결과창에는 다음 정보가 출력된다.

- 입력 주소
- 도로명 주소
- 지번 주소
- 경도
- 위도

## 꼭 알아둘 점

### 1. `63342` 같은 정적 HTML 미리보기로 열면 안 됨

예:

- `http://localhost:63342/.../naver-map-test.html`

이 방식은 Spring/Thymeleaf를 거치지 않아서 `Client ID`가 주입되지 않는다.

정상 테스트는 반드시 아래처럼 서버 URL로 확인해야 한다.

- `http://localhost:8080/dev/naver-map`

### 2. Geocoding만으로 지도가 뜨는 것은 아님

역할 분리:

- `Geocoding`: 주소를 좌표로 변환
- `Dynamic Map`: 지도를 화면에 표시

즉 지도 표시는 `Dynamic Map`, 좌표 계산은 `Geocoding`이 담당한다.

### 3. `Client Secret`은 프론트에 노출하면 안 됨

현재 구조는 안전하게 처리되어 있다.

- 브라우저에는 `Client ID`만 전달
- `Client Secret`은 백엔드에서만 사용

## 현재 구현에서 중요한 코드 포인트

### NaverGeocodingClient

역할:

- `maps.apigw.ntruss.com/map-geocode/v2/geocode` 호출
- 헤더에 `clientId`, `clientSecret` 포함

핵심:

```java
.header("x-ncp-apigw-api-key-id", properties.clientId())
.header("x-ncp-apigw-api-key", properties.clientSecret())
```

### NaverGeocodeTestController

역할:

- `/dev/naver-geocode` 템플릿 반환
- `/dev/naver-map` 템플릿 반환
- `/api/dev/naver-geocode` JSON 응답 반환

## 다음 단계 추천

실서비스 반영은 아래 흐름으로 가는 것을 추천한다.

1. 공고 등록 시 주소 입력
2. 백엔드에서 Geocoding 호출
3. DB에 `latitude`, `longitude` 저장
4. 공고 목록/상세 조회 시 좌표도 함께 응답
5. 프론트에서 지도 마커 표시

추천 DB 필드:

- `latitude`
- `longitude`
- `geocode_status`
- `geocoded_at`

## 문제 발생 시 체크리스트

### 지도가 안 뜰 때

확인 순서:

1. `Dynamic Map` 체크 여부
2. Web 서비스 URL 등록 여부
3. 로컬 프로필 활성화 여부
4. `application.properties` placeholder 존재 여부
5. `application-local.properties` 실제 값 존재 여부
6. 백엔드 재시작 여부

### `Client ID가 비어 있습니다`가 뜰 때

원인:

- 템플릿에 `Client ID` 주입이 안 된 상태

확인:

1. `http://localhost:8080/dev/naver-map`로 접속했는지
2. `63342` 정적 미리보기로 연 것은 아닌지
3. `NAVER_MAPS_CLIENT_ID` 값이 실제로 설정돼 있는지

### Geocoding 응답이 JSON이 아닐 때

원인 후보:

- 인증키 문제
- 로컬 프로필 미적용
- 서버 예외 발생

현재 테스트 페이지는 실패 시 응답 본문을 그대로 보여주도록 처리되어 있다.

## 한 줄 요약

현재 로컬에서 네이버 지도 연동은 아래 흐름으로 확인 가능하다.

- `/dev/naver-geocode`: 주소 -> 좌표 확인
- `/dev/naver-map`: 좌표 -> 지도 마커 확인

이제 다음 작업은 `공고 등록/조회 API`에 좌표 저장 로직을 붙이는 단계다.
