# Front App

`frontDemo`는 원본 디자인 레퍼런스입니다.

실제 프론트 작업용 정적 앱은 `app` 아래에 구성했습니다.

- `list.html`: 공고 목록
- `detail.html`: 공고 상세
- `auth.html`: 로그인 / 회원가입
- `mypage.html`: 마이페이지
- `recommend.html`: 추천 공고

구조:

- `assets/css`: 토큰, 공통 스타일, 컴포넌트 스타일, 페이지 보정 스타일
- `assets/js/api.js`: API 인터페이스와 현재 목업 fallback
- `assets/js/components.js`: 헤더, 푸터, 공고 카드 등 공통 렌더러
- `assets/js/*-page.js`: 페이지별 진입 스크립트

현재는 API 우선 인터페이스를 유지하면서 `USE_MOCK = true`로 동작합니다.
