# 현행 upstream/main 병합 코드리뷰 (2026-05-28)

## 목적
upstream/main에 병합된 변경사항이 현재 코드 기준에서 회귀를 만들지 않는지 점검한 결과를 공유한다.

## 결론
빌드와 백엔드 테스트는 모두 통과했지만, 운영 흐름 기준으로는 **완전 안전하다고 보기 어렵다**. 가장 큰 리스크는 `/admin/import` 화면이 백엔드에서 허용하는 `NO_PDF` import 경로를 UI에서 막아버리는 점이고, 대시보드의 “오늘 처리” 카드는 실제 today 필터 없이 전체 검수 목록으로 이동한다는 점이다.

## 주요 발견

### High
- **`/admin/import`가 백엔드 허용 경로를 과도하게 차단함**
  - 위치: `app/src/pages/AdminImportPage.jsx`
  - 내용: `canParse=false` 후보는 선택 자체가 불가능하다.
  - 영향: 백엔드에서는 `NO_PDF` 경로도 공식 데이터 저장이 가능하지만, 프론트가 막아서 운영자가 import를 끝까지 진행하지 못할 수 있다.

### Medium
- **“오늘 처리” 카드가 실제 today 필터 없이 전체 목록으로 이동함**
  - 위치: `app/src/pages/AdminDashboardPage.jsx`, `app/src/pages/AdminReviewListPage.jsx`
  - 내용: `processedToday`를 보여주지만 클릭 시 `/admin/review`로만 이동한다.
  - 영향: 운영자가 오늘 처리 건만 보는 것으로 오해할 수 있다.

### Low
- **`ttait_Front`는 `app/`과 혼동될 여지가 남아 있음**
  - 위치: `ttait_Front/README.md`, `ttait_Front/SKILL.md`
  - 내용: 디자인 자산/목업 저장소라는 설명은 있으나, 실제 앱은 `app/`이라는 경계가 더 강하게 드러나면 좋다.

## 검증 결과
- `cd subscrition && ./gradlew test` → 성공
- `cd app && npm run build` → 성공

## 남은 리스크
- `NO_PDF` 후보를 UI가 막는 회귀를 직접 잡는 자동화는 없다.
- “오늘 처리” 카드의 의미를 검증하는 UI 테스트도 없다.

## 팀 공유용 한줄 요약
테스트/빌드는 통과했지만, LH import 경로와 관리자 대시보드 의미가 일부 어긋나 있어 운영 회귀 가능성은 남아 있다.
