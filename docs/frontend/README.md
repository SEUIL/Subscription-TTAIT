# 프론트 문서 인덱스

이 폴더는 **demo 버전 프론트**와 **subscription 메인 버전 프론트**를 병렬로 작업할 수 있도록 기준 문서를 정리한 폴더다.

## 문서 구성

- `FRONTEND_WORK_SPLIT_GUIDE.md`
  - 두 버전을 동시에 진행할 때의 역할 분리, 산출물 기준, 공통 룰
- `DEMO_FRONTEND_SPEC.md`
  - `demo/frontEnd/app` 기준 데모 프론트 기능서 + 화면 설계서 + 디자인 기획서
- `MAIN_FRONTEND_SPEC.md`
  - `subscrition` 백엔드 기준 메인 프론트 기능서 + 화면 설계서 + 디자인 기획서

## 작업 원칙 요약

1. **demo는 전시/검증용 프론트**로 본다.
   - 빠른 시안 검증
   - UX 흐름 확인
   - mock 데이터 기반 데모
   - 필요 시 백엔드 연동은 최소 범위만 적용

2. **main은 실제 서비스용 프론트**로 본다.
   - 실 API 연동
   - 인증/권한 반영
   - 운영 기준 예외 처리
   - 관리자 검수 화면 포함

3. **두 버전은 코드와 일정 모두 분리**한다.
   - demo 변경이 main 구조를 흔들지 않도록 한다.
   - main의 API/권한 제약이 demo UX 실험을 막지 않도록 한다.

## 현재 기준 경로

- demo 프론트: `demo/frontEnd/app`
- 메인 백엔드: `subscrition/src/main/java/com/ttait/subscription`
- 메인 참고 문서:
  - `subscrition/명세서 및 참고파일 모음/PROJECT_ANALYSIS.md`
  - `subscrition/명세서 및 참고파일 모음/API_IMPORT_FEATURE.md`
