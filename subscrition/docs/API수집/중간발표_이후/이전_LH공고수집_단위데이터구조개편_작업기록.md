# [이전] LH 공고 수집 단위 데이터 구조 개편 작업 기록

> 작성일: 2026-05-12  
> 대상: `subscrition` 정식 백엔드  
> 목적: 이후 추천/필터링/표시 기능에서 사용할 수 있는 공고 단위 데이터를 안정적으로 확보한다.

> 현재 상태: 이 문서는 `announcement_unit` 단위 데이터 구조를 도입하던 시점의 작업 기록이다. 이후 LH import dedupe/admin 후보 API 작업으로 현행 수집 흐름이 바뀌었고, public detail에 `units[]`를 노출한다는 내용은 현재 public API 계약이 아니다. 현재 흐름은 `현행_LH공고수집_중복방지_관리자API_변경요약.md`와 `현행_LH공고수집_단위데이터구조개편_수동테스트가이드.md`를 기준으로 본다.

## 작업 배경

기존 구조는 공고 1건에 대표 보증금, 월세, 주택유형, 세대수만 저장하는 흐름이었다. LH 공고는 한 공고 안에 여러 단지, 여러 공급유형, 여러 면적, 여러 금액이 섞이는 경우가 많아 `dsSbd`의 첫 번째 row만 쓰면 데이터가 손실된다.

이번 작업은 공고를 parent로 유지하고, 실제 신청 단위에 가까운 row를 `announcement_unit`으로 정규화해서 저장하는 방향으로 진행했다.

## 완료한 작업

- `AnnouncementUnit` 도메인과 `AnnouncementUnitRepository`를 추가했다.
- unit source는 `LH_API`, `PDF_AI`, `MERGED`로 구분한다.
- LH 상세 API의 `dsSbd` 전체 row를 `LhUnitCandidateExtractor`로 추출한다.
- Gemini PDF 파싱 DTO인 `PdfParseResult`에 `units[]`를 추가했다.
- Gemini prompt가 여러 공급 row를 하나의 대표값으로 뭉개지 않고 `units[]`에 담도록 수정했다.
- import/reimport 시 기존 unit row를 삭제하고 다시 저장하는 delete-and-replace 정책을 적용했다.
- LH row와 PDF AI unit row를 주소, 단지명, 전용면적, row 순서 기준으로 병합한다.
- 기존 public list/detail 호환을 위해 `Announcement` summary 값을 unit row에서 다시 계산한다.
- public API에 `units[]`, `noticeType`, `salePriceRaw`, `scheduleDetails`, `importantNotesRaw`를 추가했다.
- admin review detail에 raw 검수용 unit 필드(`rawText`, `sourceUnitKey`, `unitSource`, `matchSource`, `confidenceLevel`)를 추가했다.
- public list의 unit 조회는 pagination 이후 현재 page 공고만 대상으로 수행하도록 조정했다.

## 구조 변경 요약

### DB 구조

이번 개편의 핵심은 공고 1건을 parent로 유지하고, 실제 신청 가능한 공급 단위를 `announcement_unit` row로 분리한 것이다.

새로 추가된 테이블은 `announcement_unit`이다.

| 컬럼 | 설명 |
|---|---|
| `announcement_id` | parent 공고 FK |
| `unit_source` | 단위 row 출처. `LH_API`, `PDF_AI`, `MERGED` |
| `source_unit_key` | 원본 row 식별키 |
| `unit_order` | 표시 순서 |
| `complex_name`, `full_address`, `region_level1`, `region_level2` | 단지/주소/지역 정보 |
| `supply_type_raw`, `supply_type_normalized` | 공급유형 원문/정규화 값 |
| `house_type_raw`, `house_type_normalized` | 주택유형 원문/정규화 값 |
| `exclusive_area_text`, `exclusive_area_value` | 전용면적 원문/숫자값 |
| `deposit_amount`, `monthly_rent_amount` | 임대 보증금/월세. 단위: 만원 |
| `sale_price_min`, `sale_price_max`, `sale_price_raw` | 분양가 최소/최대/원문. 단위: 만원 |
| `supply_household_count` | 해당 단위 공급 세대 수 |
| `raw_text` | 관리자 검수용 원문 row 또는 판단 근거 |
| `match_source` | 병합 근거. `RULE`, `AI`, `MANUAL` |
| `confidence_level` | 추출 신뢰도. `HIGH`, `MEDIUM`, `LOW` |

중복 방지를 위해 `announcement_id + unit_source + source_unit_key` 조합에 unique 제약을 둔다. import/reimport 시에는 기존 unit row를 삭제하고 다시 저장하는 delete-and-replace 방식으로 처리한다.

기존 `announcement` 테이블은 호환용 summary 필드를 계속 유지한다. 다만 보증금, 월세, 공급세대수, 주소, 단지명, 주택유형 대표값은 unit row 저장 후 다시 계산한다.

| `announcement` summary 필드 | 계산 기준 |
|---|---|
| `deposit_amount` | unit row 중 최소 보증금 |
| `monthly_rent_amount` | unit row 중 최소 월세 |
| `supply_household_count` | unit row 공급세대수 합계 |
| `full_address`, `complex_name` | unit row의 첫 번째 유효 값 |
| `house_type_raw`, `house_type_normalized` | unit row의 첫 번째 유효 주택유형 |

`announcement_detail`에는 공고 단위 부가 정보로 `notice_type`, `sale_price_raw`, `schedule_details_json`, `important_notes_raw`를 저장한다.

### 프론트/API 구조

프론트 일반 사용자 화면은 public API를 사용한다. public API는 검수 상태가 `APPROVED`, `CORRECTED`인 공고만 노출한다.

#### `GET /api/announcements`

공고 목록 API다. 기존 대표 필드는 유지하고, unit 기반 요약 필드를 추가했다.

| 필드 | 설명 |
|---|---|
| `unitCount` | 해당 공고의 unit row 개수 |
| `minDepositAmount`, `maxDepositAmount` | unit 기준 보증금 최소/최대 |
| `minMonthlyRentAmount`, `maxMonthlyRentAmount` | unit 기준 월세 최소/최대 |
| `minSalePriceAmount`, `maxSalePriceAmount` | unit 기준 분양가 최소/최대 |
| `minExclusiveArea`, `maxExclusiveArea` | unit 기준 전용면적 최소/최대 |

목록 API에서는 pagination 이후 현재 page에 포함된 공고만 대상으로 unit을 조회한다.

#### `GET /api/announcements/{announcementId}`

공고 상세 API다. 공고 단위 필드와 `units[]`를 함께 반환한다.

추가된 공고 단위 필드는 아래와 같다.

| 필드 | 설명 |
|---|---|
| `noticeType` | `임대`, `분양`, `분양전환`, `잔여세대`, `기타` |
| `salePriceRaw` | 분양가 원문 |
| `scheduleDetails` | 복수 일정 JSON 문자열 |
| `importantNotesRaw` | 유의사항 원문 |
| `units[]` | 신청 가능한 공급 단위 배열 |

public `units[]`는 아래 필드를 포함한다.

| 필드 | 설명 |
|---|---|
| `unitId`, `unitOrder` | unit 식별자/표시 순서 |
| `complexName`, `fullAddress`, `regionLevel1`, `regionLevel2` | 단지/주소/지역 정보 |
| `supplyType`, `houseType` | 정규화된 공급유형/주택유형 |
| `exclusiveAreaText`, `exclusiveAreaValue` | 전용면적 원문/숫자값 |
| `depositAmount`, `monthlyRentAmount` | 임대 보증금/월세 |
| `salePriceMin`, `salePriceMax`, `salePriceRaw` | 분양가 최소/최대/원문 |
| `supplyHouseholdCount` | 해당 단위 공급 세대 수 |
| `unitSource`, `confidenceLevel` | 출처/신뢰도 |

관리자 검수 화면은 `GET /api/admin/review/{announcementId}`를 사용한다. 이 API는 public `units[]`보다 더 많은 검수용 필드를 내려준다.

| admin unit 필드 | 설명 |
|---|---|
| `rawText` | Gemini 또는 LH row 원문 |
| `sourceUnitKey` | 원본 row 식별키 |
| `unitSource` | `LH_API`, `PDF_AI`, `MERGED` |
| `matchSource` | 병합 근거 |
| `confidenceLevel` | 추출 신뢰도 |
| `supplyTypeRaw`, `supplyTypeNormalized` | 공급유형 원문/정규화 값 |
| `houseTypeRaw`, `houseTypeNormalized` | 주택유형 원문/정규화 값 |

`GET /api/recommendations`와 즐겨찾기 응답은 아직 `units[]`를 직접 내려주지 않고 `announcement` summary 값을 사용한다. 따라서 unit row에서 재계산된 대표값이 간접 반영된다.

### Gemini 프롬프트 구조

Gemini PDF 파싱 프롬프트는 `GeminiClient.SYSTEM_PROMPT`에 정의되어 있다. PDF bytes 직접 전송과 텍스트 fallback 모두 같은 프롬프트를 사용하고, 응답은 JSON 형식으로 강제한다.

가장 큰 변경은 JSON 스키마에 `units[]`를 추가한 것이다.

```json
"units": [
  {
    "complexName": string|null,
    "address": string|null,
    "regionLevel1": string|null,
    "regionLevel2": string|null,
    "supplyType": string|null,
    "houseType": string|null,
    "exclusiveAreaText": string|null,
    "exclusiveAreaValue": number|null,
    "depositAmountManwon": number|null,
    "monthlyRentAmountManwon": number|null,
    "salePriceMinManwon": number|null,
    "salePriceMaxManwon": number|null,
    "salePriceRaw": string|null,
    "supplyHouseholdCount": number|null,
    "rawText": string|null,
    "confidence": number,
    "sourcePage": number|null
  }
]
```

프롬프트의 주요 추출 규칙은 아래와 같다.

- PDF 표/목록에서 신청 가능한 모든 row를 `units[]`에 각각 넣는다.
- 여러 단지, 공급유형, 주택유형, 전용면적, 가격, 세대수를 대표값 하나로 합치지 않는다.
- 하나의 unit은 `단지/사이트 + 공급유형 + 주택유형 + 전용면적 + 가격/세대수` 조합이다.
- 신뢰할 수 있는 단위 row가 없으면 `units: []`를 반환한다.
- `units[].rawText`에는 판단 근거가 된 표 row 또는 문장 원문을 넣는다.
- 추론이거나 일부 필드가 누락된 row는 `confidence`를 낮게 준다.
- `scheduleDetails[]`는 청약신청, 순번추첨, 사전개방, 계약체결, 상시계약 등을 각각 분리한다.
- `noticeType`은 `분양전환`, `잔여세대`, `임대`, `분양`, `기타` 중 하나로 분류한다.
- 임대 공고는 `depositAmountManwon`, `monthlyRentAmountManwon`을 사용한다.
- 분양/분양전환 공고는 `salePriceMinManwon`, `salePriceMaxManwon`, `salePriceRaw`를 사용한다.
- `1억8000만원` 같은 금액은 `18000`처럼 만원 단위 숫자로 변환한다.
- `houseType`, `address`는 LH API에서 부족한 주택유형/주소를 PDF로 보완하기 위한 top-level 호환 필드로 유지한다.

파싱 결과는 `PdfParseResult.units()`로 역직렬화된 뒤, LH `dsSbd` row와 주소, 단지명, 전용면적, row 순서를 기준으로 병합되어 `announcement_unit`에 저장된다.

## 확인한 검증

### 기본 빌드/기능 검증

- `./gradlew test` 성공
- `./gradlew build` 성공
- 로컬 앱 기동 성공
- admin 로그인 성공
- 기존 검증에서 `POST /api/admin/import/lh?page=1&size=5` 실행 성공
- 기존 검증에서 admin detail `unitsCount=5`, `rawText`, `sourceUnitKey`, `noticeType`, `scheduleDetailsJson` 확인
- 기존 검증에서 admin 승인 후 public detail `units[]` 5개, `noticeType`, `salePriceRaw` 확인
- 기존 검증에서 반복 import 후 같은 공고의 unit count가 `5 -> 5`로 유지되는 것 확인

### 2026-05-13 포그라운드 수동 검증

Gemini RPD 제한을 고려해 런타임 모델을 `gemini-2.5-flash-lite`로 맞춘 뒤 `size=3`으로 포그라운드 수동 검증을 진행했다.

| 항목 | 결과 |
|---|---|
| Gemini 모델 | `gemini-2.5-flash-lite` |
| 서버 실행 | `bootRun` 정상 기동, `8080` listen 확인 |
| admin 로그인 | `admin`, `role=ADMIN` 확인 |
| import 요청 | `POST /api/admin/import/lh?page=1&size=3` |
| import 결과 | `imported=1`, `failed=0`, `elapsedSec=35` |
| PDF/Gemini 로그 | `PDF found for panId=LN-0002821`, `Gemini PDF parse ... bytes=147399` |
| 대상 공고 | `announcementId=7` |
| admin detail | `units_count=1`, `unitId=43`, `unitSource=PDF_AI`, `matchSource=AI`, `confidenceLevel=HIGH` |
| 검수용 필드 | admin detail에서 `rawText`, `sourceUnitKey` 존재 |
| 승인 | `POST /api/admin/review/7`, `APPROVE` 성공 |
| public detail | `units_count=1`, `rawText/sourceUnitKey` 미노출 |
| DB 확인 | `announcement_unit` active row count `1` |
| 최종 판정 | `PASS` |

세부 실행 기록은 `현행_LH공고수집_단위데이터구조개편_테스트기록.md`에 정리했다. 직접 재현 절차는 Apidog 기준으로 `현행_LH공고수집_단위데이터구조개편_수동테스트가이드.md`에 분리했다.

## 추가로 확인하면 좋은 테스트

- 임대 주택, 분양/분양전환, 다중 단지 공고를 각각 1건씩 추가 확인한다.
- Gemini quota 여유가 있을 때 같은 공고를 반복 import해 `announcement_unit` row가 중복 생성되지 않고 delete-and-replace되는지 다시 확인한다.
- PowerShell/콘솔 인코딩을 UTF-8로 맞춘 뒤 evidence JSON을 다시 저장해 한글 가독성을 개선한다.

## 문서 상태 정리

- `현행_OpenAI_컨텍스트한도_테스트기록.md`는 현재 Gemini 기반 구현과 맞지 않아 `이전_OpenAI_컨텍스트한도_테스트기록.md`로 변경했다.
- `현행_LH공고수집_PDF_AI파싱_기능명세.md`와 `현행_LH공고수집_트랜잭션흐름_설명.md`의 현행 흐름 설명은 Gemini 기준으로 수정했다.
- 같은 기능명세 안의 2026-04-23 테스트 결과는 OpenAI 기반 과거 기록으로 남기되, 섹션명을 “이전 OpenAI 기반 테스트 결과”로 구분했다.

## 주의사항

- 기존 데이터 백업/백필은 이번 작업 범위가 아니었다.
- 테스트 과정에서 로컬 DB 데이터는 변경될 수 있다.
- `.env`, 로컬 DB dump, 임시 로그 파일은 커밋하지 않는다.
- 문서의 “현행” 표기는 실제 코드 기준과 맞아야 한다. OpenAI 기반 구현 기록은 “이전”으로 분리한다.
