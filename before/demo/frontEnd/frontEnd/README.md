# FrontEnd MVP 연동 메모

작업 위치: `C:\Users\kjm90\Desktop\workspace\bc\demo\frontEnd\frontEnd`

이 폴더는 `demo/frontEnd/app` 디자인을 기준으로 유지하면서 `codex/swagger_api_guide.md`, `codex/mvp_backend_summary.md` 기준의 MVP 백엔드와 연결한 결과물입니다.

기본 API 주소

- `http://localhost:8080`
- 필요하면 브라우저 콘솔 또는 로컬스토리지에서 `jg.apiBaseUrl` 값을 바꿔서 다른 서버를 가리킬 수 있습니다.

실제로 연결한 API

- `POST /api/auth/login`
- `POST /api/auth/signup`
- `GET /api/announcements`
- `GET /api/announcements/{announcementId}`
- `GET /api/me`
- `PUT /api/me/profile`
- `GET /api/recommendations`
- `POST /api/announcements/{announcementId}/save`
- `DELETE /api/announcements/{announcementId}/save`
- `GET /api/notifications`
- `PATCH /api/notifications/{notificationId}/read`

mock 또는 로컬 fallback을 쓰는 부분

- 저장한 공고 목록 조회
  - 백엔드 문서에 `GET saved-list` API가 없어 `localStorage`에 저장한 공고 스냅샷으로 구성합니다.
  - 저장/해제 호출은 실제 API를 먼저 시도하고, 목록 표시는 로컬 캐시를 사용합니다.
- 알림 설정 조회/저장
  - 백엔드 API가 없어 `localStorage`를 사용합니다.
- 추천 사유, 추천 점수
  - 추천 목록 API는 공고 목록 형태만 제공하므로 점수/사유 문구는 프론트에서 보강했습니다.
- 시세 비교 데이터 누락
  - 상세 응답의 `marketComparison`가 없으면 mock 문구로 대체합니다.
- 일부 상세 필드 누락
  - 주소, 난방방식, 첨부파일 등 응답값이 없으면 `확인 필요` 또는 기본 안내 문구로 대체합니다.
- 비로그인 상태 추천/마이페이지
  - 인증이 필요한 API를 호출할 수 없을 때 mock 프로필/목업 추천 데이터를 사용합니다.

현재 MVP 기준으로 부족한 백엔드 포인트

- 저장 공고 목록 조회 API 부재
- 알림 설정 저장 API 부재
- 추천 목록용 점수/사유 필드 부재
- 필터 옵션 API는 존재하지만 현재 UI는 기존 디자인 유지를 위해 정적 옵션 유지

동작 메모

- 로그인/회원가입 성공 시 access token을 `localStorage`에 저장합니다.
- 저장 버튼은 카드 목록과 상세 화면 양쪽에서 동작합니다.
- 마이페이지 프로필 저장은 실제 `PUT /api/me/profile` 호출을 시도하고, 실패 시 로컬 데이터라도 유지합니다.
