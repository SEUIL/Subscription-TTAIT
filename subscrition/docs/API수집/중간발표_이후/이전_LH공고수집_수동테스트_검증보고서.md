# [이전] LH 공고 수집 수동 테스트 검증 보고서

> 작성일: 2026-05-13  
> 대상: `subscrition` 정식 백엔드  
> 검증 대상 API: `POST /api/admin/import/lh?page=1&size=10`  
> 검증 공고: `PAN_ID=2015122300019940` / `announcementId=1`  
> 분석 방식: DB 조회, 저장 raw JSON 확인, LH 원본 PDF 다운로드 및 PDFBox 텍스트 추출, Oracle 검토

> 현재 상태: 이 문서는 dedupe/admin 후보 API 도입 전 legacy import 수동 검증 보고서다. 현재는 기존 legacy API를 유지하면서 후보 수집, 후보 목록, 선택 import, force reparse API가 추가되었으므로 현행 수집 흐름 설명은 `현행_LH공고수집_중복방지_관리자API_변경요약.md`와 `현행_LH공고수집_단위데이터구조개편_수동테스트가이드.md`를 기준으로 본다.

## 1. 결론 요약

| 항목 | 판정 | 근거 | 비고 |
|---|---|---|---|
| API 호출 성공 여부 | 통과 | `announcement`에 LH 공고 1건 저장, 관련 하위 테이블 row 생성 | 호출 자체는 정상 |
| LH 목록 API 값 저장 | 통과 | `LH_ITEM_JSON`의 `PAN_ID`, `PAN_NM`, `PAN_SS`, `PAN_NT_ST_DT`, `CLSG_DT`, `CNP_CD_NM`, `AIS_TP_CD_NM`이 `announcement` 주요 필드와 일치 | 원본 raw 보존됨 |
| LH 상세 API 값 저장 | 부분 통과 | 첨부파일, 접수 안내, 전화번호, 일정 raw가 `API_JSON`에 저장됨 | 일부 상세 필드는 DB 컬럼에 반영되지 않거나 PDF 값과 충돌 |
| PDF 원문 파싱 | 부분 통과 | 주소, 단지명, 일정, 자격/소득·자산 기준, 단위 row 대부분 추출 | 공급호수 합계가 원문과 불일치 |
| DB 최종 저장값 신뢰도 | 주의 필요 | `announcement.supply_household_count=22`, 실제 모집 단위는 11호로 판단됨 | 발표 전 수정 또는 주석 필요 |

요약하면, `/api/admin/import/lh`는 오류 없이 실행되고 핵심 raw 데이터도 정상 저장됐다. 다만 “DB에 들어왔다”만으로는 검증 완료라고 보기 어렵다. 특히 PDF AI 파싱 결과가 `announcement_unit`으로 저장되는 과정에서 공급호수 합계가 원문 대비 과대 집계된 것으로 보인다.

## 2. 저장 row 현황

| 테이블 | row 수 | 확인 내용 |
|---|---:|---|
| `announcement` | 1 | LH parent 공고 |
| `announcement_detail` | 1 | 상세/일정/PDF 파싱 요약 |
| `announcement_parse_raw` | 3 | `LH_ITEM_JSON`, `API_JSON`, `PDF_AI_JSON` |
| `announcement_unit` | 11 | PDF AI 기반 공급 단위 row |
| `announcement_eligibility` | 1 | AI 파싱 자격조건 및 검수 상태 |
| `announcement_category` | 1 | 카테고리 감지 결과 |

## 3. LH 목록 API raw와 DB 비교

| LH raw 필드 | raw 값 | DB 필드 | DB 값 | 판정 |
|---|---|---|---|---|
| `PAN_ID` | `2015122300019940` | `announcement.source_notice_id` | `2015122300019940` | 일치 |
| `PAN_NM` | `장애인 자립특화형 주택 여기가 입주자 모집공고(경기도 김포시)` | `announcement.notice_name` | 동일 | 일치 |
| `PAN_SS` | `접수중` | `announcement.notice_status_raw` | `접수중` | 일치 |
| `PAN_NT_ST_DT` | `2026.05.13` | `announcement.announcement_date` | `2026-05-13` | 일치 |
| `CLSG_DT` | `2026.05.22` | `announcement.application_end_date` | `2026-05-22` | 일치 |
| `CNP_CD_NM` | `전국` | `announcement.region_level1` | `전국` | 일치 |
| `AIS_TP_CD_NM` | `매입임대` | `announcement.supply_type_raw` | `매입임대` | 일치 |
| `DTL_URL` | LH apply 상세 URL | `announcement.source_notice_url` | 동일 계열 URL | 일치 |

목록 API에서 들어온 주요 공고 식별/상태/지역/유형 값은 DB에 정상 반영됐다. 또한 `announcement_parse_raw`에 `LH_ITEM_JSON`으로 원문이 저장되어 사후 검증 가능하다.

## 4. LH 상세 API raw와 DB 비교

| 상세 raw 영역 | raw 값 | DB 반영 | 판정 | 비고 |
|---|---|---|---|---|
| `dsAhflInfo` | HWP/PDF 첨부 4건 | `API_JSON` raw 저장 | 통과 | PDF URL `fileid=66831927` 사용 |
| `dsCtrtPlc.SIL_OFC_TLNO` | `02-2648-2252` | `announcement_detail.contact_phone=02-2648-2252` | 통과 | DB 저장 확인 |
| `dsCtrtPlc.SIL_OFC_GUD_FCTS` | 우편접수, 프리웰 주소, 2026.5.22 소인 유효 | `announcement_detail.contact_guide_text` 저장 | 통과 | PDF AI의 `contact_raw`는 홈페이지만 저장 |
| `dsSplScdl.SBSC_ACP_ST_DT` | `2026.05.13` | `announcement.application_start_date` | 부분 통과 | PDF 원문 신청접수는 `2026.5.20~2026.5.22` |
| `ACP_DTTM` | `~` | `announcement_detail.application_datetime_text` | 일치 | 값 자체는 의미가 약함 |

상세 API raw는 저장되어 있으나, 사용자에게 보여줄 “실제 신청접수 기간”은 PDF 원문/AI 파싱 결과가 더 정확해 보인다. 현재 `announcement.application_start_date=2026-05-13`은 LH 상세 API의 `SBSC_ACP_ST_DT`를 따른 값이고, PDF 원문 기준 신청접수 시작일 `2026.5.20`과 다르다.

## 5. 원본 PDF와 PDF AI 파싱 비교

검증한 원본 PDF: `여기가_입주자모집공고문_3차_최종_프리웰(260513_공고).pdf`  
다운로드 URL: `https://apply.lh.or.kr/lhapply/lhFile.do?fileid=66831927`

| 항목 | PDF 원문 | `PDF_AI_JSON` / DB 저장값 | 판정 |
|---|---|---|---|
| 공고 제목 | `장애인자립세대 및 아동양육세대 특화형 주택 『여기가』 입주자 모집공고(3차)` | DB 공고명은 LH 목록 API명 저장 | 정상 범위 |
| 단지/주소 | `공급주택 : 여기家 (김포시 양촌읍 양곡리 490, 488-3, 487)` | `complex_name=여기家`, `full_address=김포시 양촌읍 양곡리 490, 488-3, 487` | 일치 |
| 공고 유형 | 임대 공고 | `notice_type=임대` | 일치 |
| 신청접수 | `2026.5.20~2026.5.22` | `PDF_AI_JSON.applicationPeriod=2026.5.20.~2026.5.22.` | 일치 |
| 소득 기준 | 월평균 소득 100% 이하 | `income_asset_criteria_raw`에 저장 | 일치 |
| 자산 기준 | 총자산 33,700만원 이하, 자동차 4,563만원 이하 | `income_asset_criteria_raw`에 저장 | 일치 |
| 신청자격 | 장애인, 아동양육가구 조건 | `eligibility_raw`에 저장 | 일치 |
| 문의/접수 | 프리웰 홈페이지, 우편접수 안내 | `contact_raw=프리웰 홈페이지(thehome.freewel.org)` | 부분 일치 |

PDF의 주요 설명성 데이터는 전반적으로 잘 파싱됐다. 다만 접수 방법의 상세 주소/우편접수 조건은 `API_JSON`에는 있으나 `contact_raw`에는 홈페이지 중심으로 저장되어 있어, 화면 표시 목적이라면 보강 여지가 있다.

## 6. 공급 단위 데이터 검증

### 6.1 DB 저장 요약

| 구분 | DB 값 |
|---|---:|
| `announcement_unit` row 수 | 11 |
| `SUM(supply_household_count)` | 22 |
| 최소 보증금 | 7,930만원 |
| 최대 보증금 | 9,031만원 |
| 최소 월세 | 562만원 |
| 최대 월세 | 626만원 |

### 6.2 원본 PDF 표와 DB 비교

| 공급유형 | 면적 | PDF 원문 row 수 | PDF 원문 공급호수 해석 | DB row 수 | DB 공급호수 합계 | 판정 |
|---|---:|---:|---:|---:|---:|---|
| 장애인 | 72.7410㎡ | 5 | 5호 | 5 | 16 | 불일치 |
| 아동양육 | 72.7940㎡ | 4 | 4호 | 4 | 4 | 일치 |
| 장애인 | 82.5390㎡ | 1 | 1호 | 1 | 1 | 일치 |
| 장애인 | 82.0410㎡ | 1 | 1호 | 1 | 1 | 일치 |
| 합계 | - | 11 | 11호 | 11 | 22 | 불일치 |

가장 큰 문제는 첫 번째 장애인 72.7410㎡ 그룹이다. PDF 원문에는 5개 호실이 각 1호로 보이는데, DB에는 같은 그룹의 공급호수 합계가 16으로 저장되어 전체 합계가 22가 됐다. 원문 상단의 `장애인가구 [공급호수] 7호`, `아동양육가구 [공급호수] 4호`와도 맞지 않는다.

원인 후보는 다음과 같다.

| 후보 | 설명 | 가능성 |
|---|---|---|
| PDF AI가 첫 row의 `supplyHouseholdCount`를 `12`로 오인 | 주택개요의 `1동 12호` 또는 표 주변 숫자를 첫 공급 row에 잘못 연결했을 가능성 | 높음 |
| `unitSummaryService`가 AI 단위 공급호수를 그대로 합산 | `announcement.supply_household_count=22`로 반영됨 | 높음 |
| 공급호수와 모집인원 개념 혼동 | PDF에는 모집인원 3배수 문구가 있으나 실제 공급호수와 다름 | 중간 |

보증금/월세는 단위가 “만원” 기준으로 저장된 것으로 보이며, 원문 `7,930,000원`, `562,220원`이 각각 `7930`, `562`로 반영된 것은 기존 설계상 정상 범위다. 다만 월세는 원 단위 절삭 또는 반올림 정책을 문서화해야 한다.

## 7. 주요 이슈 및 조치 제안

| 우선순위 | 이슈 | 영향 | 권장 조치 |
|---|---|---|---|
| 높음 | 공급호수 합계가 PDF 원문 11호가 아니라 22호로 저장됨 | 추천/필터/상세 표시에서 공급 규모 오표시 가능 | `PDF_AI_JSON.units[].supplyHouseholdCount` 검수 로직 추가. 호실별 row는 기본 1호로 보정하거나 원문 표의 호실 개수와 교차검증 |
| 높음 | `application_start_date=2026-05-13`이 PDF 신청접수 `2026.5.20`과 다름 | 사용자에게 접수 시작일 오안내 가능 | LH 상세 API 날짜와 PDF 일정 중 어떤 값을 public 기준으로 쓸지 정책 결정. 신청접수 일정은 PDF `scheduleDetails` 우선 검토 |
| 중간 | `contact_raw`가 홈페이지만 저장되고 우편접수 주소/조건이 빠짐 | 신청 방법 안내가 부족할 수 있음 | `dsCtrtPlc.SIL_OFC_GUD_FCTS` 또는 PDF 접수 안내를 public/admin detail에 함께 노출 |
| 중간 | `complex_address`, `household_count`, `exclusive_area_text` 등 detail 대표 필드 일부 NULL | 대표 상세값만 보는 화면에서 빈 값 발생 | 단위 row 기반 summary 또는 PDF 값으로 보완 여부 검토 |
| 낮음 | `PDF_AI_JSON` top-level 공급/보증금/월세가 null이고 units에만 존재 | 구조상 가능하나 검수자가 헷갈릴 수 있음 | 문서에 “대표값은 unit summary에서 계산”이라고 명시 |

## 8. 최종 판단

이번 수동 테스트는 “API 호출이 성공했고 DB 저장 파이프라인이 동작한다”는 점에서는 성공이다. LH 목록 raw, 상세 raw, PDF AI raw가 모두 보존되어 재검증 가능한 상태인 점도 좋다.

하지만 “저장된 값이 원본과 정확히 일치한다”는 기준에서는 아직 통과로 보기 어렵다. 특히 공급호수 합계 22는 원본 PDF의 실제 모집 단위 11호와 다르므로, 발표나 시연 전에 이 부분은 반드시 “현재 발견된 검증 이슈”로 설명하거나 보정 로직을 추가하는 것이 안전하다.

## 9. 확인에 사용한 근거

| 근거 | 내용 |
|---|---|
| DB 조회 | `announcement`, `announcement_detail`, `announcement_parse_raw`, `announcement_unit`, `announcement_eligibility` |
| raw JSON | `LH_ITEM_JSON`, `API_JSON`, `PDF_AI_JSON` |
| 원본 PDF | LH 첨부 PDF `fileid=66831927` |
| PDF 텍스트 추출 | PDFBox 기반 텍스트 추출 |
| Oracle 검토 | API/DB/PDF 파싱 흐름의 불일치와 리스크 판단 |
