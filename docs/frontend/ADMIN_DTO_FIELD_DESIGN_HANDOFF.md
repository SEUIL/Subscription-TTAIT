# 관리자 DTO 필드 디자인 핸드오프

> 대상: 디자인팀 / 프론트엔드 작업자  
> 기준: 백엔드 Swagger DTO와 현재 관리자 프론트 화면 비교  
> 목적: 프론트가 받거나 받을 수 있는 DTO 값 중 화면에 없거나 UX 판단이 필요한 필드를 디자인팀에 전달

## 1. 사용자 규칙

프론트엔드에 DTO 값이 있거나 API 응답으로 받을 수 있는데 화면에 반영되지 않았고 디자인 판단이 필요하다면, 프론트에서 임의로 UI를 설계하지 않는다.

이 경우 별도 Markdown 문서로 디자인팀에 핸드오프하고, 디자인 결정이 내려질 때까지 기다린다.

## 2. 이미 커버된 필드

아래 필드는 현재 관리자 프론트에서 렌더링되거나 요청 값으로 입력, 전송되는 것으로 확인된 항목이다. 별도 디자인 결정이 필요한 누락 항목으로 보지 않는다.

### AdminStatsResponse

현재 `AdminDashboardPage`가 렌더링한다.

`pending`, `approved`, `corrected`, `rejected`, `reImport`, `totalAnnouncements`

### AdminReviewListResponse

현재 `AdminReviewListPage`가 렌더링한다.

`announcementId`, `noticeName`, `providerName`, `regionLevel1`, `regionLevel2`, `applicationEndDate`, `supplyType`, `houseType`, `depositAmount`, `monthlyRentAmount`, `supplyHouseholdCount`, `unitCount`, `reviewStatus`, `reviewedAt`, `reviewedBy`

### AdminReviewDetailResponse

상세 화면은 핵심 요약, 출처, 상태, 일정, 주소, 유형, 세대 수 계열 필드, AI 파싱 조건, 원문 자격 조건, 원문 출처 텍스트, units, 보정 입력, 검수 이력을 렌더링한다.

검수 이력은 `reviewedBy`가 있을 때 표시된다.

### AdminAnnouncementUnitResponse

현재 `ReviewUnitsSection`이 렌더링한다.

`unitOrder`, `unitSource`, `confidenceLevel`, `matchSource`, `complexName`, `fullAddress`, `supplyTypeRaw`, `supplyTypeNormalized`, `houseTypeRaw`, `houseTypeNormalized`, `exclusiveAreaText`, `depositAmount`, `monthlyRentAmount`, `salePriceMin`, `salePriceMax`, `supplyHouseholdCount`, `rawText`, `sourceUnitKey`

### LH import DTO

`LhImportRunResult` import 결과는 아래 필드를 렌더링한다.

`fetched`, `scanned`, `skippedLand`, `unchanged`, `geminiSkipped`, `imported`, `reparsed`, `failed`

`LhCandidateCollectionResponse` 수집 결과는 아래 필드를 렌더링한다.

`fetched`, `scanned`, `skippedLand`

`LhImportCandidateResponse` 후보 테이블은 아래 필드를 렌더링한다.

`id`, `panId`, `title`, `region`, `status`, `dedupeStatus`, `canParse`, `isLandNotice`, `alreadyImported`, `pdfUrl`, `sourceNoticeUrl`

### ManualAnnouncementRequest

`AdminAnnouncementNewPage`는 요청 스키마의 모든 필드를 입력, 전송 대상으로 다룬다.

## 3. 디자인 결정이 필요해서 빠진 필드

아래 필드는 백엔드 Swagger DTO에는 있지만 현재 화면에서 눈에 띄게 렌더링되지 않는다. 프론트에서 임의 배치하지 말고 디자인 결정 후 반영한다.

### P0. 운영 판단에 바로 영향을 주는 필드

1. `AdminStatsResponse.processedToday`
   오늘 처리량을 대시보드 핵심 지표로 보여줄지 결정이 필요하다.
2. `AdminReviewListResponse.noticeStatus`
   검수 상태 `reviewStatus`와 함께 보여줄 때 관리자가 혼동하지 않도록 표시명과 위치를 정해야 한다.
3. `AdminReviewListResponse.applicationStartDate`
   마감일 `applicationEndDate`만 보여주는 현재 목록에서 시작일을 함께 보여줄지 결정이 필요하다.
4. `AdminReviewDetailResponse.scheduleDetailsJson`
   JSON 형태의 일정 세부값을 원문 그대로 보여줄지, 구조화된 일정 블록으로 풀지 결정이 필요하다.
5. `AdminReviewDetailResponse.importantNotesRaw`
   공고 중요 안내 원문을 관리자 검수 흐름에서 어느 정도 강조할지 결정이 필요하다.

### P1. 목록과 상세 이해도를 높이는 필드

1. `AdminReviewListResponse.fullAddress`
   목록에서 지역명만 보여줄지, 상세 주소까지 노출할지 결정이 필요하다.
2. `AdminReviewListResponse.complexName`
   공고명과 단지명이 중복될 수 있어 표시 우선순위와 열 구성이 필요하다.
3. `AdminReviewDetailResponse.supplyHouseholdCountBasis`
   세대 수 산정 기준을 보조 정보로 보여줄지, 검수 입력 근거로 강조할지 결정이 필요하다.
4. `AdminReviewDetailResponse.noticeType`
   공고 유형을 상태, 공급 유형, 주택 유형 중 어디와 묶어 보여줄지 결정이 필요하다.
5. `AdminReviewDetailResponse.salePriceRaw`
   매매가 원문을 가격 정보 영역에 보조값으로 둘지, 원문 검수 영역에 둘지 결정이 필요하다.

### P2. 단위 정보 정밀 검수용 필드

1. `AdminAnnouncementUnitResponse.unitId`
   관리자에게 내부 식별자를 노출할 필요가 있는지 결정이 필요하다.
2. `AdminAnnouncementUnitResponse.regionLevel1`, `AdminAnnouncementUnitResponse.regionLevel2`
   단위 행에서 주소와 지역을 중복 표시할지 결정이 필요하다.
3. `AdminAnnouncementUnitResponse.exclusiveAreaValue`
   `exclusiveAreaText`와 함께 숫자 값을 표시할지, 정렬과 필터 용도로만 둘지 결정이 필요하다.
4. `AdminAnnouncementUnitResponse.salePriceRaw`
   `salePriceMin`, `salePriceMax`와 원문 가격을 함께 보여줄 때 중복과 불일치 처리가 필요하다.

## 4. 기술적으로 연결되어 있지만 UX 리뷰가 필요한 필드

아래 항목은 단순 누락이라기보다 조건부 표시나 해석 방식 때문에 UX 리뷰가 필요하다.

1. `AdminReviewDetailResponse.reviewNote`, `AdminReviewDetailResponse.reviewedAt`
   상세 화면의 검수 이력은 `reviewedBy`가 없으면 숨겨진다. 따라서 `reviewNote`나 `reviewedAt`이 있어도 `reviewedBy`가 없으면 표시되지 않을 수 있다. 검수자 없음 상태에서 이력을 보여줄지 결정이 필요하다.
2. `AdminReviewDetailResponse.scheduleDetailsJson`
   기술적으로 값은 받을 수 있지만 JSON 원문 노출은 관리자에게 부담이 될 수 있다. 일정 요약, 원문 접기, 오류 확인 중 어떤 사용 목적을 우선할지 정해야 한다.
3. `AdminReviewDetailResponse.importantNotesRaw`
   원문 안내를 그대로 노출하면 길어질 수 있다. 검수 필수 정보인지, 참고 정보인지, 접힘 영역인지 결정이 필요하다.
4. `AdminAnnouncementUnitResponse.salePriceRaw`
   정규화된 가격 범위와 원문 가격이 같이 있을 때 서로 다르게 보일 수 있다. 불일치 표시 방식이 필요하다.
5. `AdminReviewDetailResponse.homelessRequired`, `AdminReviewDetailResponse.lowIncomeRequired`, `AdminReviewDetailResponse.elderlyRequired`
   기술적으로 렌더링되어 있지만 현재 truthy 표시 방식에서는 백엔드 `null`이 `아니오`처럼 보일 수 있다. 알 수 없음/null과 명시적 false를 어떻게 구분할지 디자인/UX 결정이 필요하다.

## 5. 디자인팀 질문

1. 대시보드의 `processedToday`는 기존 상태 카드와 같은 수준의 핵심 지표인가?
2. 검수 목록에서 `noticeStatus`와 `reviewStatus`를 동시에 보여줄 때 라벨을 어떻게 구분할까?
3. 목록의 주소 정보는 `regionLevel1`, `regionLevel2`만 유지할까, `fullAddress`와 `complexName`까지 확장할까?
4. 상세의 원문성 필드 `salePriceRaw`, `importantNotesRaw`, `scheduleDetailsJson`는 항상 펼칠까, 접힘 영역으로 둘까?
5. 단위 정보의 내부값 `unitId`, `exclusiveAreaValue`는 관리자에게 직접 보여줄 값인가, 검색과 디버깅 보조값인가?
6. `reviewedBy`가 없지만 `reviewNote`나 `reviewedAt`이 있는 검수 이력은 어떻게 보여줄까?

## 6. 백엔드와 DB 테스트 메모

디자인 확정 후 프론트 반영 여부를 확인할 때는 mock data가 아니라 실제 백엔드 응답이나 DB에 저장된 데이터로 테스트해야 한다.

특히 조건부 이력 표시, 원문 필드, 가격 원문과 정규화 가격의 차이는 실제 데이터에서만 확인 가능한 경우가 많다.

## 7. 다음 액션

1. 디자인팀이 P0 항목의 표시 여부와 위치를 먼저 결정한다.
2. P1 항목은 목록 밀도와 상세 정보 구조를 함께 보고 결정한다.
3. P2 항목은 운영자 디버깅 필요성이 확인된 뒤 노출 여부를 정한다.
4. 프론트엔드는 디자인 확정 전까지 위 필드를 임의로 화면에 추가하지 않는다.
