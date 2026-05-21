# [현행] LH 저장 데이터 기준 주변 시세 비교 Backend 분석

<!-- markdownlint-disable MD013 MD060 -->

## 1. 문서 목적

이 문서는 `LH API + PDF AI 파싱`으로 현재 DB에 저장되는 데이터를 기준으로, `주변 시세 비교` 기능을 만들 때 어떤 데이터가 이미 있고 어떤 데이터가 부족한지 정리한다.

이번 분석 범위에서는 프론트, DTO, Controller API 수정은 제외한다. 해당 영역은 별도 작업 중인 것으로 보고, `subscrition` backend의 DB 저장 구조와 시세 비교용 데이터 모델만 다룬다.

## 2. 결론 요약

주변 시세 비교의 기준은 `Announcement`가 아니라 `AnnouncementUnit`이어야 한다.

이유는 `Announcement`가 공고 parent 요약값이기 때문이다. 여러 공급 row가 있는 공고에서는 주소, 면적, 보증금, 월세, 분양가가 서로 다를 수 있는데, parent는 이 값을 첫 값, 최소값, 합산값 등으로 접어서 저장한다. 반면 `AnnouncementUnit`은 신청 가능한 공급 row 단위로 주소, 면적, 가격 정보를 들고 있다.

현재 DB 데이터만으로 가능한 것과 불가능한 것은 다음과 같다.

| 구분 | 가능 여부 | 이유 |
|-|-|-|
| 공고별 주소/가격 표시 | 가능 | `Announcement`, `AnnouncementDetail`, `AnnouncementUnit`에 주소와 가격 일부가 저장됨 |
| unit별 면적/가격 기준 비교 | 부분 가능 | `AnnouncementUnit`에 면적, 보증금, 월세, 분양가가 있음 |
| 법정동 기준 RTMS 조회 | 현재 불가 | `legalCode` 필드는 있으나 LH import에서 채워지지 않음 |
| 지도/반경 기반 주변 비교 | 부분 가능 | `AnnouncementUnit` 좌표 저장은 1차 MVP 완료. 다만 RTMS/market snapshot과 주소 정밀도 보강이 필요 |
| 주변 실거래가 평균/중앙값 비교 | 현재 불가 | 정식 backend에 RTMS 수집/market snapshot 테이블이 없음 |

따라서 최종 구조는 다음 흐름이 되어야 한다.

```text
LH 목록/상세 수집
→ PDF AI 파싱
→ AnnouncementUnit 저장
→ Naver Geocoding으로 unit 좌표 보강(1차 MVP 완료)
→ unit 주소 정규화와 법정동 코드 보강
→ LAWD_CD + DEAL_YMD 기준 RTMS 수집
→ market raw/snapshot 저장
→ unit별 market comparison 계산
```

## 3. 현재 LH import 저장 흐름

### 3.1 전체 흐름

현재 LH 수집은 아래 순서로 동작한다.

1. `LhApiClient.fetchNoticeList(...)`가 LH 목록 API를 호출한다.
2. 목록 row를 `NoticeImportPersistenceService.upsertLh(...)`가 `Announcement`에 저장한다.
3. `LhApiClient.fetchNoticeDetail(...)`가 LH 상세 API를 호출한다.
4. 상세 응답에서 PDF URL을 찾고, 필요하면 Gemini 기반 PDF 파싱을 수행한다.
5. `NoticeImportPersistenceService.upsertLhDetail(...)`가 상세 정보와 PDF 파싱 결과를 저장한다.
6. `replaceUnits(...)`가 기존 `AnnouncementUnit`을 삭제하고 새 unit row를 재생성한다.
7. `AnnouncementUnitSummaryService.applySummary(...)`가 unit 정보를 parent `Announcement`에 요약 반영한다.

주요 파일:

- `subscrition/src/main/java/com/ttait/subscription/external/service/NoticeImportOrchestrator.java`
- `subscrition/src/main/java/com/ttait/subscription/external/service/NoticeImportPersistenceService.java`
- `subscrition/src/main/java/com/ttait/subscription/external/service/LhUnitCandidateExtractor.java`
- `subscrition/src/main/java/com/ttait/subscription/external/service/AnnouncementUnitSummaryService.java`

### 3.2 LH 목록 API에서 저장되는 값

LH 목록 API는 `Announcement` parent를 만든다.

| LH 필드 | 저장 위치 | 의미 |
|-|-|-|
| `PAN_ID` | `announcement.source_notice_id` | LH 공고 ID |
| `PAN_NM` | `announcement.notice_name` | 공고명 |
| `DTL_URL` | `announcement.source_notice_url` | 원문 URL |
| `DTL_URL_MOB` | `announcement.source_mobile_url` | 모바일 URL |
| `PAN_SS` | `announcement.notice_status_raw`, `notice_status` | 원본/정규화 상태 |
| `PAN_NT_ST_DT` | `announcement.announcement_date` | 공고일 |
| `CLSG_DT` | `announcement.application_end_date` | 접수 마감일 |
| `CNP_CD_NM` | `announcement.region_level1` | 시도 |
| `AIS_TP_CD_NM` | `announcement.supply_type_raw`, `supply_type_normalized` | 공급유형 |

주의할 점:

- LH 목록 단계에서는 `full_address`, `region_level2`, `legal_code`, `house_type`, `deposit_amount`, `monthly_rent_amount`가 채워지지 않는다.
- 이 값들은 상세 API 또는 PDF AI 파싱 결과로 보완된다.

### 3.3 LH 상세 API에서 저장되는 값

LH 상세 API의 주요 배열은 `dsSplScdl`, `dsSbd`, `dsEtcInfo`, `dsCtrtPlc`다.

| LH 상세 필드 | 저장 위치 | 의미 |
|-|-|-|
| `dsSplScdl[0].SBSC_ACP_ST_DT` | `announcement.application_start_date` | 접수 시작일 |
| `dsSplScdl[0].ACP_DTTM` | `announcement_detail.application_datetime_text` | 신청 일시 원문 |
| `dsSplScdl[0].PPR_ACP_ST_DT` | `announcement_detail.document_submit_start_date` | 서류 제출 시작일 |
| `dsSplScdl[0].PPR_ACP_CLSG_DT` | `announcement_detail.document_submit_end_date` | 서류 제출 마감일 |
| `dsSplScdl[0].CTRT_ST_DT` | `announcement_detail.contract_start_date` | 계약 시작일 |
| `dsSplScdl[0].CTRT_ED_DT` | `announcement_detail.contract_end_date` | 계약 마감일 |
| `dsSbd[0].LCC_NT_NM` | `announcement.complex_name`, `announcement_detail.complex_name` | 단지명 |
| `dsSbd[0].SBD_NM` | `announcement.complex_name` fallback | 상가 등에서 사용 |
| `dsSbd[0].LGDN_ADR` | `announcement.full_address`, `announcement_detail.complex_address` | 단지 주소 |
| `dsSbd[0].LGDN_DTL_ADR` | `announcement_detail.complex_detail_address` | 상세 주소 |
| `dsSbd[0].HSH_CNT` | `announcement_detail.household_count` | 세대수 |
| `dsSbd[0].HTN_FMLA_DESC` | `announcement_detail.heating_type` | 난방 방식 |
| `dsSbd[0].DDO_AR` | `announcement_detail.exclusive_area_text`, `exclusive_area_value` | 전용면적 |
| `dsSbd[0].MVIN_XPC_YM` | `announcement_detail.move_in_expected_ym` | 입주 예정월 |
| `dsEtcInfo[0].ETC_CTS` | `announcement_detail.guide_text` | 안내사항 |
| `dsCtrtPlc[0].SIL_OFC_TLNO` | `announcement_detail.contact_phone` | 문의 전화 |
| `dsCtrtPlc[0].CTRT_PLC_ADR` | `announcement_detail.contact_address` | 문의처 주소 |
| `dsCtrtPlc[0].SIL_OFC_GUD_FCTS` | `announcement_detail.contact_guide_text` | 문의처 안내 |

주의할 점:

- 상세 API의 `dsSbd`는 첫 row만 `AnnouncementDetail`에 들어간다.
- 여러 공급 row를 정확히 보려면 `AnnouncementDetail`이 아니라 `AnnouncementUnit`을 봐야 한다.

### 3.4 LH 상세 `dsSbd[]`에서 unit row로 저장되는 값

`LhUnitCandidateExtractor`는 LH 상세 응답의 `dsSbd` 배열을 순회하면서 unit 후보를 만든다.

| LH 상세 `dsSbd` 필드 | 저장 위치 | 의미 |
|-|-|-|
| `LCC_NT_NM` 또는 `SBD_NM` | `announcement_unit.complex_name` | 단지명 |
| `LGDN_ADR` | `announcement_unit.full_address` | 주소 |
| `DDO_AR` | `announcement_unit.exclusive_area_text`, `exclusive_area_value` | 전용면적 |
| `HSH_CNT` | `announcement_unit.supply_household_count` | 공급 세대수 |
| parent `AIS_TP_CD_NM` | `announcement_unit.supply_type_raw`, `supply_type_normalized` | 공급유형 |
| parent/PDF `houseType` | `announcement_unit.house_type_raw`, `house_type_normalized` | 주택유형 |

LH-only unit의 한계:

- 보증금, 월세, 분양가는 LH 상세 `dsSbd`에서 안정적으로 들어오지 않는다.
- 그래서 LH-only unit은 `deposit_amount`, `monthly_rent_amount`, `sale_price_min`, `sale_price_max`가 비어 있을 수 있다.
- 가격 비교에는 PDF AI unit과 merge된 `MERGED` 또는 `PDF_AI` unit이 더 중요하다.

### 3.5 PDF AI 파싱 결과에서 저장되는 값

PDF AI 파싱 결과는 `PdfParseResult`로 들어오고, top-level 값과 `units[]` 값이 나뉘어 저장된다.

Top-level PDF 결과:

| PDF 필드 | 저장 위치 | 의미 |
|-|-|-|
| `noticeType` | `announcement_detail.notice_type` | 공고 유형 |
| `supplyHouseholdCount.value` | `announcement_detail.supply_household_count_raw` | 공급 세대수 원문 |
| `depositMonthlyRent.value` | `announcement_detail.deposit_monthly_rent_raw` | 보증금/월세 원문 |
| `incomeAssetCriteria.value` | `announcement_detail.income_asset_criteria_raw` | 소득/자산 기준 원문 |
| `contact.value` | `announcement_detail.contact_raw` | 문의처 원문 |
| `salePriceRaw.value` | `announcement_detail.sale_price_raw` | 분양가 원문 |
| `scheduleDetails` | `announcement_detail.schedule_details_json` | 일정 JSON |
| `importantNotes.value` | `announcement_detail.important_notes_raw` | 유의사항 원문 |
| `houseType` | `announcement.house_type_raw` | 주택유형 |
| `address` | `announcement.full_address` fallback | API 주소가 없을 때 보완 |
| `depositAmountManwon` | `announcement.deposit_amount` | parent 보증금 요약 |
| `monthlyRentAmountManwon` | `announcement.monthly_rent_amount` | parent 월세 요약 |

PDF `units[]` 결과:

| PDF unit 필드 | 저장 위치 | 의미 |
|-|-|-|
| `complexName` | `announcement_unit.complex_name` | 단지명 |
| `address` | `announcement_unit.full_address` | 주소 |
| `regionLevel1` | `announcement_unit.region_level1` | 시도 |
| `regionLevel2` | `announcement_unit.region_level2` | 시군구 |
| `supplyType` | `announcement_unit.supply_type_raw`, `supply_type_normalized` | 공급유형 |
| `houseType` | `announcement_unit.house_type_raw`, `house_type_normalized` | 주택유형 |
| `exclusiveAreaText` | `announcement_unit.exclusive_area_text` | 면적 원문 |
| `exclusiveAreaValue` | `announcement_unit.exclusive_area_value` | 면적 숫자 |
| `depositAmountManwon` | `announcement_unit.deposit_amount` | 보증금 |
| `monthlyRentAmountManwon` | `announcement_unit.monthly_rent_amount` | 월세 |
| `salePriceMinManwon` | `announcement_unit.sale_price_min` | 최소 분양가 |
| `salePriceMaxManwon` | `announcement_unit.sale_price_max` | 최대 분양가 |
| `salePriceRaw` | `announcement_unit.sale_price_raw` | 분양가 원문 |
| `supplyHouseholdCount` | `announcement_unit.supply_household_count` | 공급 세대수 |
| `rawText` | `announcement_unit.raw_text` | 판단 근거 원문 |
| `confidence` | `announcement_unit.confidence_level` | 신뢰도 |

## 4. 주변 시세 비교에 쓸 수 있는 현재 데이터

주변 시세 비교 입력값으로 가장 적합한 데이터는 `announcement_unit`이다.

| 현재 컬럼 | 사용처 | 신뢰도 |
|-|-|-|
| `full_address` | 주소 정규화, geocoding 입력 | 중간 |
| `region_level1` | 시도 보조 필터 | 중간 |
| `region_level2` | 시군구 보조 필터 | 중간 |
| `complex_name` | 단지명/매칭 보조 | 중간 |
| `house_type_normalized` | RTMS source type 선택 | 중간 |
| `exclusive_area_value` | 유사 면적 필터 | 높음, 단 null 가능 |
| `deposit_amount` | 공고 보증금 비교 | PDF merge 여부에 따라 다름 |
| `monthly_rent_amount` | 공고 월세 비교 | PDF merge 여부에 따라 다름 |
| `sale_price_min`, `sale_price_max` | 분양가 비교 | 분양/분양전환 공고에서만 사용 |
| `unit_source` | 데이터 출처 판단 | 높음 |
| `confidence_level` | AI 추출 신뢰도 gate | 높음 |

권장 gate:

- `unit_source = MERGED` 또는 `PDF_AI`인 unit을 우선 사용한다.
- `confidence_level = LOW`이면 자동 비교 결과에 `LOW_CONFIDENCE`를 붙인다.
- `exclusive_area_value`, 가격, 주소 중 하나라도 없으면 `INSUFFICIENT_UNIT_DATA`로 처리한다.

## 5. 현재 부족한 데이터

### 5.1 위치 데이터 부족

정식 backend에는 다음 필드나 테이블이 없다.

```text
latitude
longitude
normalized_address
legal_dong_code
lawd_cd
geocode_status
geocoded_at
```

따라서 현재 상태에서는 지도 표시나 반경 기반 주변 비교가 불가능하다.

### 5.2 법정동 코드 부족

`Announcement.legalCode` 필드는 존재하지만 LH import에서 채우는 경로가 없다.

RTMS 실거래가 API는 주소 문자열이 아니라 `LAWD_CD`와 `DEAL_YMD`를 기준으로 조회한다. `LAWD_CD`는 법정동 코드 앞 5자리다. 따라서 주소에서 법정동 코드를 구하는 단계가 반드시 필요하다.

### 5.3 시장 데이터 저장소 부족

정식 backend에는 아직 아래 구조가 없다.

```text
market_transaction_raw
market_price_snapshot
unit_market_comparison
lawd_code_mapping
address_geocode_cache
```

즉, 현재 DB에는 비교 대상인 민간 실거래가 데이터가 없다.

### 5.4 unit 재수집 구조로 인한 캐시 문제

현재 `replaceUnits(...)`는 reimport 때 기존 unit row를 삭제하고 새로 만든다.

따라서 좌표나 비교 결과를 `announcement_unit.id`에만 묶으면 재수집 후 연결이 깨질 수 있다. `unit_fingerprint` 또는 주소 기반 cache key가 필요하다.

## 6. 추가해야 할 backend 데이터 구조

### 6.1 주소/좌표 캐시

추천 테이블명:

```text
address_geocode_cache
```

추천 컬럼:

| 컬럼 | 설명 |
|-|-|
| `id` | PK |
| `raw_address` | LH/PDF 원본 주소 |
| `normalized_address` | 정규화 주소 |
| `provider` | KAKAO, NAVER, VWORLD 등 |
| `latitude` | 위도 |
| `longitude` | 경도 |
| `legal_dong_code` | 법정동 코드 10자리 |
| `lawd_cd` | RTMS 조회용 앞 5자리 |
| `region_level1` | 시도 |
| `region_level2` | 시군구 |
| `region_level3` | 읍면동 |
| `geocode_status` | PENDING, SUCCESS, FAILED, AMBIGUOUS |
| `failure_reason` | 실패 사유 |
| `geocoded_at` | 좌표 변환 시각 |

캐시를 별도 테이블로 두는 이유:

- unit row는 재수집 때 삭제/재생성된다.
- 같은 주소를 여러 공고에서 반복 geocoding하지 않아도 된다.
- geocoding 실패/모호함 상태를 별도로 관리할 수 있다.

### 6.2 unit fingerprint

추천 컬럼 또는 계산값:

```text
unit_fingerprint = announcement_id
                 + normalized_address
                 + house_type_normalized
                 + exclusive_area_value
                 + deposit_amount/monthly_rent_amount/sale_price
```

용도:

- reimport 후 unit id가 바뀌어도 같은 공급 row인지 추적한다.
- `unit_market_comparison` 재계산/재사용 기준으로 쓴다.
- 단, 가격이 바뀌면 fingerprint가 바뀔 수 있으므로 주소+면적 중심 fingerprint와 가격 포함 fingerprint를 분리할 수도 있다.

### 6.3 RTMS 원천 거래 테이블

추천 테이블명:

```text
market_transaction_raw
```

추천 컬럼:

| 컬럼 | 설명 |
|-|-|
| `id` | PK |
| `source_type` | APT_RENT, ROW_HOUSE_RENT, OFFICETEL_RENT, APT_TRADE 등 |
| `lawd_cd` | RTMS 조회 지역 코드 |
| `deal_ym` | 계약년월 |
| `legal_dong_name` | 법정동명 |
| `building_name` | 단지/건물명 |
| `jibun` | 지번 |
| `road_name` | 도로명 |
| `build_year` | 건축년도 |
| `exclusive_area` | 전용면적 |
| `floor` | 층 |
| `deposit_amount` | 보증금 |
| `monthly_rent_amount` | 월세 |
| `trade_amount` | 매매가/분양 비교용 |
| `contract_type` | 전세/월세/갱신 등 가능한 경우 |
| `raw_payload_hash` | 중복 방지용 hash |
| `collected_at` | 수집 시각 |

### 6.4 시세 집계 테이블

추천 테이블명:

```text
market_price_snapshot
```

추천 컬럼:

| 컬럼 | 설명 |
|-|-|
| `id` | PK |
| `source_type` | 거래 데이터 유형 |
| `lawd_cd` | 지역 코드 |
| `deal_ym_from` | 집계 시작월 |
| `deal_ym_to` | 집계 종료월 |
| `area_min` | 면적 하한 |
| `area_max` | 면적 상한 |
| `avg_deposit_amount` | 평균 보증금 |
| `median_deposit_amount` | 중앙 보증금 |
| `avg_monthly_rent_amount` | 평균 월세 |
| `median_monthly_rent_amount` | 중앙 월세 |
| `avg_trade_amount` | 평균 매매/분양가 |
| `median_trade_amount` | 중앙 매매/분양가 |
| `sample_count` | 표본 수 |
| `snapshot_status` | OK, LOW_SAMPLE, EMPTY |
| `calculated_at` | 계산 시각 |

평균만 쓰기보다 중앙값을 같이 저장하는 것을 권장한다. 실거래가는 이상치가 섞일 수 있기 때문이다.

### 6.5 unit별 비교 결과 테이블

추천 테이블명:

```text
unit_market_comparison
```

추천 컬럼:

| 컬럼 | 설명 |
|-|-|
| `id` | PK |
| `announcement_unit_id` | 비교 대상 unit |
| `unit_fingerprint` | reimport 안정성 보조 |
| `snapshot_id` | 비교에 사용한 snapshot |
| `comparison_status` | OK, INSUFFICIENT_UNIT_DATA, NO_GEOCODE, NO_MARKET_DATA, LOW_CONFIDENCE |
| `public_deposit_amount` | 공고 보증금 |
| `public_monthly_rent_amount` | 공고 월세 |
| `public_sale_price_min` | 공고 최소 분양가 |
| `public_sale_price_max` | 공고 최대 분양가 |
| `market_median_deposit_amount` | 주변 중앙 보증금 |
| `market_median_monthly_rent_amount` | 주변 중앙 월세 |
| `market_median_trade_amount` | 주변 중앙 매매가 |
| `deposit_diff_rate` | 보증금 차이율 |
| `rent_diff_rate` | 월세 차이율 |
| `trade_price_diff_rate` | 분양/매매가 차이율 |
| `sample_count` | 표본 수 |
| `summary` | 비교 요약 문구 |
| `compared_at` | 비교 계산 시각 |

## 7. 외부 API 요구사항

### 7.1 Geocoding

한국 주소를 좌표로 바꾸는 API는 있다.

| 후보 | 역할 | 비고 |
|-|-|-|
| Kakao Local API | 주소 → 좌표, 좌표 → 행정/법정 코드 | 도로명/지번 주소 지원 |
| Naver Maps Geocoding | 주소 → 좌표 | 도로명/지번 주소와 좌표 반환 |
| Juso API | 주소 검색/정규화/보조 코드 | 표준 주소검색은 좌표보다 주소 정규화에 적합 |
| VWorld Geocoder | 주소 → 좌표 | DB 저장 금지 문구가 있어 캐시 구조와 충돌 가능 |

권장:

- 1순위: Kakao Local 또는 Naver Geocoding
- 보조: Juso로 주소 정규화
- VWorld는 저장 정책 문제 때문에 보조 후보로만 검토

### 7.2 RTMS 실거래가 API

RTMS 계열 실거래가 API는 보통 아래 키가 필요하다.

```text
LAWD_CD   // 법정동코드 앞 5자리
DEAL_YMD  // 계약년월 6자리, 예: 202604
serviceKey
```

사용 후보:

- 국토교통부 아파트 전월세 실거래가
- 국토교통부 연립다세대 전월세 실거래가
- 국토교통부 오피스텔 전월세 실거래가
- 분양/매매 비교까지 확장할 경우 아파트/연립다세대/오피스텔 매매 실거래가

주의:

- RTMS는 좌표 기반 API가 아니다.
- 먼저 주소를 법정동 코드로 바꿔야 한다.
- `주변`을 법정동 기준으로 볼지, 좌표 반경 기준으로 볼지 정책을 정해야 한다.

## 8. 비교 정책 제안

### 8.1 MVP 기준

처음에는 좌표 반경 검색보다 법정동 기준 비교가 현실적이다.

```text
unit.fullAddress
→ geocoding/주소정규화
→ lawd_cd 확보
→ 최근 6개월 RTMS 조회
→ 같은 주택유형 + 유사 면적대 snapshot 생성
→ unit 가격과 비교
```

면적 범위 예시:

```text
exclusive_area_value * 0.8 <= market.exclusive_area <= exclusive_area_value * 1.2
```

이 방식은 데모의 `MarketComparisonService` 구조와 가장 가깝다.

### 8.2 확장 기준

지도 기반 주변 비교까지 하려면 좌표가 필요하다.

```text
unit 좌표
→ 같은 법정동 또는 인접 법정동 market transaction 후보 조회
→ 거래 주소도 geocoding
→ 반경 n m 이내 필터
→ 평균/중앙값 계산
```

확장 시에는 `market_transaction_raw`에도 거래 주소 좌표를 붙이는 구조가 필요하다.

## 9. 기존 demo 구조에서 참고할 부분

데모에는 다음 구조가 있다.

| 데모 파일 | 참고할 점 |
|-|-|
| `MarketRentSnapshot` | 실거래가 snapshot 필드 구조 |
| `LawdCodeMapping` | 시도/시군구 → 법정동 코드 매핑 구조 |
| `MarketComparisonService` | 주소 → 코드 → snapshot 조회 → 평균 비교 흐름 |
| `MarketRentSnapshotRepository` | 주택유형 + 법정동코드 + 면적 범위 조회 |

단, 데모 구조는 실제 좌표 반경 검색이 아니다. 행정구역과 면적 기준 평균 비교에 가깝다.

정식 backend에서는 데모 구조를 그대로 복사하기보다 아래처럼 분리하는 것이 좋다.

```text
market/geocode
market/rtms
market/transaction
market/snapshot
market/comparison
```

## 10. 최종 예상 구조

### 10.1 패키지 구조

```text
com.ttait.subscription.market
 ├─ geocode
 │   ├─ AddressGeocodeCache
 │   ├─ GeocodingClient
 │   └─ AddressGeocodingService
 ├─ rtms
 │   ├─ RtmsClient
 │   ├─ RtmsRentResponseParser
 │   └─ RtmsTradeResponseParser
 ├─ transaction
 │   ├─ MarketTransactionRaw
 │   └─ MarketTransactionRepository
 ├─ snapshot
 │   ├─ MarketPriceSnapshot
 │   └─ MarketSnapshotService
 └─ comparison
     ├─ UnitMarketComparison
     └─ MarketComparisonService
```

### 10.2 처리 흐름

```text
1. LH import 완료
2. AnnouncementUnit 생성
3. unit.fullAddress 기준 주소 정규화/geocoding
4. address_geocode_cache 저장
5. lawd_cd 기준 RTMS 최근 n개월 수집
6. market_transaction_raw 저장
7. market_price_snapshot 계산
8. unit_market_comparison 계산
9. 공고 단위로 비교 가능 unit 수와 최저/중앙/최고 차이율 집계
```

### 10.3 상태값 예시

`geocode_status`:

```text
PENDING
SUCCESS
FAILED
AMBIGUOUS
```

`comparison_status`:

```text
OK
INSUFFICIENT_UNIT_DATA
NO_GEOCODE
NO_LAWD_CODE
NO_MARKET_DATA
LOW_SAMPLE
LOW_CONFIDENCE
```

## 11. 구현 우선순위

### 1단계: DB 보강

1. `address_geocode_cache` 추가
2. `market_transaction_raw` 추가
3. `market_price_snapshot` 추가
4. `unit_market_comparison` 추가
5. 필요 시 `AnnouncementUnit`에 `unit_fingerprint` 추가

### 2단계: 주소 보강

1. `AnnouncementUnit.fullAddress` 기준 주소 정규화
2. Kakao 또는 Naver geocoding 연동
3. 좌표, 법정동코드, `lawd_cd` 저장
4. 실패/모호 주소는 상태값으로 관리

### 3단계: RTMS 수집

1. `lawd_cd + deal_ym` 기준 최근 6개월 수집
2. XML 파싱
3. raw transaction 저장
4. 중복 방지 hash 적용

### 4단계: 시세 집계와 비교

1. 주택유형 + 법정동 + 면적구간별 snapshot 생성
2. unit별 비교 결과 계산
3. 표본 부족/데이터 부족 상태 처리
4. 공고 단위 aggregate 결과 생성

## 12. 최종 판단

현재 LH/PDF 저장 구조는 주변 시세 비교의 출발점으로는 충분하다. 특히 `AnnouncementUnit`은 주소, 주택유형, 면적, 가격을 갖고 있어 비교 대상 unit을 정의하기 좋다.

하지만 아직 주변 시세 비교 기능 자체에 필요한 데이터는 없다. 부족한 핵심은 다음 세 가지다.

1. 주소를 좌표와 법정동코드로 바꾸는 위치 보강 데이터
2. RTMS 실거래가를 저장하고 집계하는 market 데이터
3. `AnnouncementUnit`과 market snapshot을 연결한 비교 결과 데이터

따라서 구현 방향은 `AnnouncementUnit` 중심으로 두고, parent `Announcement`는 비교 기준이 아니라 결과를 묶어 보여주는 집계 단위로 두는 것이 맞다.
