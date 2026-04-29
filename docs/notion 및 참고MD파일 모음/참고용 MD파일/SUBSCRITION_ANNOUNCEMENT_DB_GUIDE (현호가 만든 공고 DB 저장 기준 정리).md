# `subscrition` 공고 DB 저장 기준 정리

## 목적

PDF AI 추출값과 API 값을 후처리해서 공고를 DB에 저장하고, `subscrition` 프로젝트의 기존 유저 정보를 바꾸지 않은 상태에서 추천 공고 기능에 사용할 수 있도록 공고 저장 기준을 정의한다.

핵심 원칙은 아래와 같다.

- 유저는 `subscrition`의 기존 `user`, `user_profile`, `user_category`를 그대로 사용한다.
- 공고도 유저와 동일한 6개 카테고리를 가져야 한다.
- 공고 원문은 반드시 남기고, 추천에 필요한 값은 정규화해서 따로 저장한다.
- 사용자가 보낸 공고 중 상위 6개 공고는 핵심 필드에 `null`이 있으면 안 된다.
- `supplyHouseholdCount`는 `"56호"`, `"총 22호"`처럼 들어와도 숫자만 추출해서 저장한다.

---

## 유저 기준

현재 `subscrition`의 유저 기준 카테고리는 아래 6개다.

- `YOUTH`
- `NEWLYWED`
- `HOMELESS`
- `ELDERLY`
- `LOW_INCOME`
- `MULTI_CHILD`

추천 공고 판단은 `user_category.category_code`와 `announcement_category.category_code`를 매칭하는 방식으로 처리한다.

즉, 추천 기준의 1차 조건은 아래와 같다.

- 유저가 가진 카테고리와 공고가 가진 카테고리가 하나 이상 일치해야 함

그 다음 보조 조건은 기존 `user_profile` 값을 사용한다.

- `preferredRegionLevel1`
- `preferredRegionLevel2`
- `preferredHouseType`
- `preferredSupplyType`
- `maxDeposit`
- `maxMonthlyRent`

---

## 사용자 구분 요소 종합 기준

공고를 저장할 때는 단순 공고 정보만 넣으면 안 되고, 기존 사용자 정보와 바로 비교할 수 있도록 "사용자 구분 요소"와 맞닿는 필드를 같이 저장해야 한다.

현재 `subscrition` 유저 쪽에서 바로 활용 가능한 구분 요소는 아래와 같다.

| 사용자 기준 | 유저 저장 위치 | 공고 저장 시 필요한 대응 필드 |
|---|---|---|
| 나이 | `user_profile.age` | `age_min`, `age_max`, `age_raw_text` |
| 혼인 상태 | `user_profile.marital_status` | `marital_target_type`, `marital_raw_text` |
| 자녀 수 | `user_profile.children_count` | `children_min_count`, `children_raw_text` |
| 무주택 여부 | `user_profile.homeless` | `homeless_required`, `homeless_raw_text` |
| 저소득 여부 | `user_profile.lowIncome` | `low_income_required`, `income_asset_criteria_raw` |
| 고령자 여부 | `user_profile.elderly` | `elderly_required`, `elderly_age_min`, `elderly_raw_text` |
| 선호 지역 | `preferredRegionLevel1`, `preferredRegionLevel2` | `region_level1`, `region_level2`, `full_address` |
| 선호 주택 유형 | `preferredHouseType` | `house_type_normalized`, `house_type_raw` |
| 선호 공급 유형 | `preferredSupplyType` | `supply_type_normalized`, `supply_type_raw` |
| 최대 보증금 | `maxDeposit` | `deposit_amount`, `deposit_monthly_rent_raw` |
| 최대 월세 | `maxMonthlyRent` | `monthly_rent_amount`, `deposit_monthly_rent_raw` |

즉, 공고 저장 시에는 아래 두 층을 같이 가져가야 한다.

- 추천용 정규화 필드
- 원문 검증용 raw 필드

---

## 공고 자격조건 저장 필드

사용자 구분 요소와 직접 비교할 수 있도록 공고 자격조건 전용 필드를 별도로 두는 것을 권장한다.

테이블명 예시:

- `announcement_eligibility`

필드 구성:

| 필드명 | 설명 | null 허용 |
|---|---|---|
| `id` | PK | N |
| `announcement_id` | 공고 FK | N |
| `age_min` | 최소 나이 | Y |
| `age_max` | 최대 나이 | Y |
| `age_raw_text` | 나이 조건 원문 | N |
| `marital_target_type` | `SINGLE`, `MARRIED`, `NEWLYWED`, `ENGAGED`, `ANY` | Y |
| `marriage_year_limit` | 혼인 n년 이내 | Y |
| `marital_raw_text` | 혼인 조건 원문 | N |
| `children_min_count` | 최소 자녀 수 | Y |
| `children_raw_text` | 자녀 조건 원문 | N |
| `homeless_required` | 무주택 필요 여부 | Y |
| `homeless_raw_text` | 무주택 조건 원문 | N |
| `low_income_required` | 저소득 여부 | Y |
| `income_asset_criteria_raw` | 소득/자산 조건 원문 | N |
| `elderly_required` | 고령자 대상 여부 | Y |
| `elderly_age_min` | 고령자 최소 나이 | Y |
| `elderly_raw_text` | 고령자 조건 원문 | N |
| `eligibility_raw` | 전체 자격조건 원문 | N |
| `special_supply_raw` | 특별공급/우선공급 원문 | N |

이 테이블의 목적은 "공고가 어떤 사용자에게 해당하는지"를 DB 차원에서 판별 가능하게 만드는 것이다.

예를 들면:

- `만 19~39세` -> `age_min = 19`, `age_max = 39`, `age_raw_text` 저장
- `혼인 7년 이내` -> `marital_target_type = NEWLYWED`, `marriage_year_limit = 7`
- `미성년 자녀 3인 이상` -> `children_min_count = 3`
- `무주택세대구성원` -> `homeless_required = true`
- `만 65세 이상` -> `elderly_required = true`, `elderly_age_min = 65`
- `기초생활수급자, 차상위` -> `low_income_required = true`

---

## API JSON + PDF 파싱값 최종 저장 기준

공고 저장 필드는 "어느 소스에서 왔는지"보다 "추천과 비교에 쓸 수 있느냐" 기준으로 정리해야 한다.

그래서 필드는 아래 4종류로 나눈다.

### 1. 공고 기본 정보

- 공고명
- 공급기관
- 공고 URL
- 공고일
- 접수 시작일
- 접수 종료일
- 지역
- 공급유형
- 주택유형

### 2. 금액/물량 정보

- 보증금
- 월세
- 실제 공급호수
- 공급호수 원문
- 공급호수 판단 근거

### 3. 사용자 비교용 자격조건 정보

- 최소/최대 나이
- 혼인 대상 구분
- 혼인 연차 제한
- 최소 자녀 수
- 무주택 필요 여부
- 저소득 필요 여부
- 고령자 여부
- 고령 최소 나이

### 4. 원문 보존 정보

- 접수기간 원문
- 보증금/월세 원문
- 소득/자산 기준 원문
- 문의처 원문
- 신청자격 원문
- 카테고리 판별 근거 원문

정리하면, API JSON과 PDF 파싱값은 아래처럼 합쳐서 저장하는 구조가 된다.

| 저장 목적 | 대표 필드 |
|---|---|
| 공고 기본 조회 | `notice_name`, `provider_name`, `region_level1`, `region_level2` |
| 추천 필터링 | `category_code`, `age_min`, `age_max`, `marital_target_type`, `children_min_count`, `homeless_required`, `low_income_required`, `elderly_required` |
| 비용 비교 | `deposit_amount`, `monthly_rent_amount` |
| 공급 물량 비교 | `supply_household_count`, `supply_household_count_basis` |
| 검수/재처리 | `*_raw` 계열 필드 전체 |

---

## 공고 저장 구조

기존 테이블은 유지한다.

- `announcement`
- `announcement_detail`

추가 테이블 2개를 권장한다.

- `announcement_category`
- `announcement_parse_raw`

---

## 1. `announcement` 저장 필드

공고의 기본 메타 정보와 추천용 핵심 값을 저장한다.

| 필드명 | 설명 | null 허용 |
|---|---|---|
| `source_primary` | 데이터 출처 | N |
| `source_notice_id` | 출처 공고 ID | N |
| `notice_name` | 공고명 | N |
| `provider_name` | 공급기관명 | N |
| `source_notice_url` | 공고 URL | N |
| `source_pc_url` | PC URL | Y |
| `source_mobile_url` | 모바일 URL | Y |
| `notice_status_raw` | 원본 상태값 | Y |
| `notice_status` | 정규화 상태값 | N |
| `announcement_date` | 공고일 | Y |
| `application_start_date` | 접수 시작일 | Y |
| `application_end_date` | 접수 종료일 | Y |
| `winner_announcement_date` | 당첨 발표일 | Y |
| `region_level1` | 시/도 | N |
| `region_level2` | 시/군/구 | Y |
| `full_address` | 전체 주소 | Y |
| `legal_code` | 법정동 코드 등 | Y |
| `complex_name` | 단지명 | Y |
| `provider_complex_household_count` | 공급기관 기준 단지 세대수 | Y |
| `supply_type_raw` | 원본 공급유형 | Y |
| `supply_type_normalized` | 정규화 공급유형 | Y |
| `house_type_raw` | 원본 주택유형 | Y |
| `house_type_normalized` | 정규화 주택유형 | Y |
| `deposit_amount` | 보증금 | Y |
| `monthly_rent_amount` | 월임대료 | Y |
| `supply_household_count` | 실제 모집/공급 호수 숫자값 | Y |
| `match_key` | 중복/병합 판별 키 | N |
| `merged_group_key` | 병합 그룹 키 | Y |
| `collected_at` | 수집 시각 | N |

---

## 2. `announcement_detail` 저장 필드

PDF와 상세 API에서 나오는 세부 정보와 원문형 텍스트를 저장한다.

기존 필드는 유지하고, 원문 보존 필드를 추가하는 방식을 권장한다.

| 필드명 | 설명 | null 허용 |
|---|---|---|
| `announcement_id` | 공고 FK | N |
| `application_datetime_text` | 접수 기간 원문 | Y |
| `document_submit_start_date` | 서류 제출 시작일 | Y |
| `document_submit_end_date` | 서류 제출 종료일 | Y |
| `contract_start_date` | 계약 시작일 | Y |
| `contract_end_date` | 계약 종료일 | Y |
| `complex_name` | 단지명 상세 | Y |
| `complex_address` | 단지 주소 | Y |
| `complex_detail_address` | 단지 상세 주소 | Y |
| `household_count` | 상세 세대수 | Y |
| `heating_type` | 난방 방식 | Y |
| `exclusive_area_text` | 전용면적 원문 | Y |
| `exclusive_area_value` | 전용면적 숫자 | Y |
| `move_in_expected_ym` | 입주 예정 년월 | Y |
| `guide_text` | 안내문 | Y |
| `contact_phone` | 문의 전화 | Y |
| `contact_address` | 문의처 주소 | Y |
| `contact_guide_text` | 문의 안내문 | Y |
| `supply_household_count_raw` | 공급호수 원문 | N |
| `supply_household_count_basis` | 공급호수 판단 근거 문장 | N |
| `supply_household_count_confidence` | 공급호수 판단 신뢰도 (`HIGH`, `MEDIUM`, `LOW`) | N |
| `deposit_monthly_rent_raw` | 보증금/월세 원문 | N |
| `income_asset_criteria_raw` | 소득/자산 기준 원문 | N |
| `contact_raw` | 문의처 원문 | N |
| `eligibility_raw` | 신청자격 원문 | N |

설명:

- 정규화 숫자 필드와 원문 필드는 같이 저장한다.
- 숫자 파싱이 불완전해도 원문은 반드시 남긴다.
- 추천과 검색은 정규화 필드를 사용하고, 상세 화면과 검수는 원문 필드를 사용한다.

---

## 3. `announcement_category` 저장 필드

공고가 어떤 유저 카테고리에 해당하는지 저장한다.

한 공고는 여러 카테고리를 가질 수 있다.

| 필드명 | 설명 | null 허용 |
|---|---|---|
| `id` | PK | N |
| `announcement_id` | 공고 FK | N |
| `category_code` | 유저 카테고리와 동일한 코드 | N |
| `match_source` | 판별 출처 (`RULE`, `AI`, `MANUAL`) | N |
| `match_reason` | 판별 근거 문장 또는 키워드 | N |
| `score` | 매칭 점수 | N |

제약 조건 권장:

- `UNIQUE (announcement_id, category_code)`

이 테이블이 있어야 추천공고 클릭 시 유저 카테고리와 공고 카테고리를 직접 비교할 수 있다.

---

## 4. `announcement_parse_raw` 저장 필드

원본 추출 결과를 별도로 보관한다.

| 필드명 | 설명 | null 허용 |
|---|---|---|
| `id` | PK | N |
| `announcement_id` | 공고 FK | N |
| `raw_type` | `API_JSON`, `PDF_AI_MD`, `PDF_AI_JSON` 등 | N |
| `raw_text` | 원본 문자열 | N |
| `collected_at` | 수집 시각 | N |

용도:

- PDF AI 추출 실패나 파싱 버그가 생겼을 때 원문 재검증
- 파싱 규칙 개선 시 재처리
- 디버깅 및 관리자 검수

---

## 추천공고 매칭에 필요한 최소 저장 필드

공고가 추천공고 화면에서 제대로 작동하려면 아래 필드는 사실상 필수다.

| 분류 | 필수 필드 |
|---|---|
| 공고 식별 | `source_primary`, `source_notice_id`, `match_key` |
| 기본 정보 | `notice_name`, `provider_name`, `source_notice_url`, `region_level1` |
| 일정 | `application_datetime_text` |
| 비용 | `deposit_monthly_rent_raw` |
| 물량 | `supply_household_count_raw`, `supply_household_count_basis`, `supply_household_count` |
| 자격 | `eligibility_raw`, `income_asset_criteria_raw` |
| 문의 | `contact_raw` |
| 카테고리 | `announcement_category` 최소 1건 |
| 유저 비교용 자격필드 | `age_raw_text`, `marital_raw_text`, `children_raw_text`, `homeless_raw_text`, `elderly_raw_text` |

---

## 공고 카테고리 기준

공고도 유저와 동일한 아래 6개 카테고리를 가져야 한다.     

| 카테고리 코드 | 의미 | 대표 판별 기준 |
|---|---|---|
| `YOUTH` | 청년 | `청년`, `만 19~39세`, `대학생`, `사회초년생` |
| `NEWLYWED` | 신혼부부 | `신혼부부`, `예비신혼부부`, `혼인 7년 이내` |
| `HOMELESS` | 무주택자 | `무주택`, `무주택세대구성원` |
| `ELDERLY` | 고령자 | `고령자`, `만 65세 이상` |
| `LOW_INCOME` | 저소득자 | `기초생활수급자`, `차상위`, `저소득` |
| `MULTI_CHILD` | 다자녀 | `다자녀`, `미성년 자녀 3인 이상` |

판별 결과는 `announcement_category`에 저장한다.

한 공고 예시:

- 청년 전용 공고면 `YOUTH`
- 신혼부부와 다자녀가 같이 들어가면 `NEWLYWED`, `MULTI_CHILD` 둘 다 저장
- 무주택, 저소득, 고령자 조건이 함께 있으면 3개 모두 저장

---

## PDF 파싱 입력값 매핑 기준

PDF AI 결과에서 아래 컬럼을 기준으로 후처리한다.

| PDF 추출 항목 | 저장 위치 | 처리 방식 |
|---|---|---|
| `applicationPeriod` | `announcement_detail.application_datetime_text` | 원문 그대로 저장 후 날짜 추출 시 시작/종료일 반영 |
| `supplyHouseholdCount` | `announcement_detail.supply_household_count_raw`, `announcement_detail.supply_household_count_basis`, `announcement.supply_household_count` | 원문 저장 + 의미 판별 후 실제 공급호수만 저장 |
| `depositMonthlyRent` | `announcement_detail.deposit_monthly_rent_raw`, `announcement.deposit_amount`, `announcement.monthly_rent_amount` | 원문 저장 + 금액 숫자 추출 |
| `incomeAssetCriteria` | `announcement_detail.income_asset_criteria_raw` | 원문 그대로 저장 |
| `contact` | `announcement_detail.contact_raw`, `announcement_detail.contact_phone`, `announcement_detail.contact_address` | 원문 저장 + 전화번호/주소 분리 시도 |
| 공고 본문 자격 문장 | `announcement_detail.eligibility_raw`, `announcement_detail.guide_text` | 원문 저장 후 카테고리 판별에 사용 |

---

## `supplyHouseholdCount` 처리 규칙

이 값은 단순 숫자 추출로 처리하면 안 된다.

원칙:

- `supply_household_count`는 "실제 모집/공급 수량"만 저장한다.
- `공급호수 원문`과 `판단 근거`는 반드시 같이 남긴다.
- `"호"`, `"세대"`, `"가구"`, `"실"` 같은 단위 제거는 보조 처리일 뿐, 최종 판정 기준이 아니다.
- 문장 안에 숫자가 여러 개 있으면 의미를 판별한 뒤 저장한다.
- 제일 큰 숫자 하나를 뽑는 방식으로 처리하면 안 된다.
- 원문은 `supply_household_count_raw`에 그대로 저장한다.
- 어떤 표현을 보고 공급호수로 판단했는지는 `supply_household_count_basis`에 남긴다.
- 확신이 낮으면 `supply_household_count_confidence`를 `LOW`로 저장해서 검수 대상으로 남긴다.

예시:

| 원문 | 저장 기준 |
|---|---|
| `56호` | `supply_household_count = 56` |
| `총 22호` | `supply_household_count = 22` |
| `행복주택 52호` | `supply_household_count = 52` |
| `422세대` | 공급물량이 아니라 단지 총세대수일 가능성이 높음. 이 경우 `provider_complex_household_count = 422`, `supply_household_count`는 별도 근거가 있을 때만 저장 |
| `1개(104호)` | `104호`는 호실번호일 수 있으므로 `supply_household_count = 1`이 맞고, `104`를 공급호수로 저장하면 안 됨 |
| `2호(총17호 중 15호 공급)` | 실제 공급 문구가 명시되어 있으면 `supply_household_count = 15` 우선, `17`은 참고값으로만 봄 |

권장 기준:

- `"총 N호"`, `"총 N세대 모집"`, `"N호 공급"`, `"공급호수 N"` 같이 모집/공급이 명시된 수량을 우선 사용
- `"N세대"`는 단지 전체 규모인지 공급 물량인지 문맥 확인 후 저장
- `"104호"`, `"101동"`, `"36A"` 같은 호실/동/평형 숫자는 공급호수로 저장하면 안 됨
- 여러 숫자가 섞인 경우 `실제 공급 수량`이 드러나는 표현만 `supply_household_count`에 저장
- 애매한 경우 원문과 판단 근거를 저장하고 검수 대상으로 분류

절대 금지 규칙:

- 숫자 하나만 보인다고 무조건 `supply_household_count`에 넣기
- 문장 안에서 가장 큰 숫자를 공급호수라고 가정하기
- `호`가 붙었다는 이유만으로 모두 공급호수로 처리하기

상위 6개 공고는 이 필드가 무조건 채워져야 한다.

단, 이 말은 "숫자만 어떻게든 넣는다"는 뜻이 아니다.

상위 6개 공고는 아래 기준으로 저장해야 한다.

- `supply_household_count_raw`는 반드시 저장
- `supply_household_count_basis`는 반드시 저장
- `supply_household_count`는 실제 공급호수로 검수 후 저장
- 애매한 문장은 수동 확인을 거쳐 값 확정

---

## 상위 6개 공고 null 금지 규칙

사용자가 보낸 공고 중 위쪽 6개 공고는 아래 필드에 `null`이 있으면 안 된다.

### `announcement`

- `notice_name`
- `provider_name`
- `source_notice_url`
- `notice_status`
- `region_level1`
- `supply_household_count`
- `match_key`
- `collected_at`

### `announcement_detail`

- `application_datetime_text`
- `supply_household_count_raw`
- `supply_household_count_basis`
- `supply_household_count_confidence`
- `deposit_monthly_rent_raw`
- `income_asset_criteria_raw`
- `contact_raw`
- `eligibility_raw`

### `announcement_category`

- 카테고리 최소 1건 이상

즉, 상위 6개 공고는 아래 상태로 저장되면 안 된다.

- 카테고리 없는 공고
- 공급호수 숫자 없는 공고
- 접수기간 원문 없는 공고
- 임대조건 원문 없는 공고
- 소득/자산 기준 원문 없는 공고
- 문의처 원문 없는 공고

문자열은 값이 애매해도 원문 그대로 저장해서 `null`을 피한다.

---

## 추천공고 판단 방식

추천공고에서는 유저의 카테고리와 공고의 카테고리를 직접 비교한다.

판단 기준:

1. `user_category.category_code`와 `announcement_category.category_code`가 하나 이상 일치
2. 같은 카테고리 내에서 아래 값으로 정렬 또는 가중치 부여

- `preferredRegionLevel1`
- `preferredRegionLevel2`
- `preferredSupplyType`
- `preferredHouseType`
- `maxDeposit`
- `maxMonthlyRent`

즉, 추천의 핵심은 아래 두 가지다.

- 유저도 카테고리를 가짐
- 공고도 같은 카테고리를 가짐

그래서 공고 카테고리 저장은 선택이 아니라 필수다.

---

## 최종 정리

이번 작업의 핵심 설계는 아래 한 줄로 정리된다.

`subscrition`의 기존 유저는 그대로 두고, 공고를 `기본정보 + 상세원문 + 카테고리 + 원본파싱결과` 구조로 저장한다.

필수 포인트:

- 공고는 유저와 같은 6개 카테고리를 가져야 한다.
- 카테고리는 별도 테이블로 다대다 구조처럼 저장한다.
- PDF에서 나온 원문은 무조건 남긴다.
- 추천은 `유저 카테고리 = 공고 카테고리` 매칭으로 시작한다.
- 상위 6개 공고는 핵심 필드 `null` 없이 저장해야 한다.
- `supplyHouseholdCount`는 숫자 추출이 아니라 실제 공급물량 의미를 판별해서 저장해야 한다.
