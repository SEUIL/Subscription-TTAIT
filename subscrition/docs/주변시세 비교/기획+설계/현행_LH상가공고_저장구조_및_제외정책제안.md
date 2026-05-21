# [현행] LH 상가 공고 저장 구조 및 제외 정책 제안

<!-- markdownlint-disable MD013 -->

## 1. 문서 목적

이 문서는 LH 공고 수집 중 `임대상가(추첨)` 같은 상가 공고가 현재 backend에 어떤 구조로 저장되는지 정리하고, 주변시세 비교 및 청약 알림 서비스 관점에서 상가 공고를 토지 공고처럼 import skip 대상에 포함하는 것이 적절한지 판단하기 위한 기준을 정리한다.

분석 범위는 현재 코드 기준의 저장 흐름과 정책 제안이다. 이 문서 자체는 코드 변경 내역이 아니다.

## 2. 결론 요약

현재 코드에는 상가 공고를 명시적으로 제외하는 skip 로직이 없다.

명시적으로 제외되는 것은 토지 공고뿐이다. 현재 토지 공고는 `UPP_AIS_TP_CD = "01"`일 때 `LAND_SKIP`으로 처리된다. 반면 상가 공고는 별도 타입으로 분리되지 않고 일반 LH 공고와 같은 구조로 저장된다.

따라서 상가 공고에서 PDF 파싱이나 unit 저장이 되지 않는 현상은 "상가라서 막힌 것"이라기보다, LH 상세 OpenAPI 응답에 `dsAhflInfo` 또는 `dsSbd`가 없어서 발생하는 `NO_PDF` / unit 미생성 흐름으로 보는 것이 맞다.

서비스 정책상 상가 공고가 청년, 신혼부부, 무주택자, 고령자, 저소득자, 다자녀 대상 공공임대 알림 서비스의 대상이 아니라면 토지 공고처럼 skip 대상에 추가하는 것이 적절하다. 다만 `LAND_SKIP`에 합치지 말고 `COMMERCIAL_SKIP` 같은 별도 decision으로 분리하는 것이 좋다.

## 3. 현재 상가 공고 저장 구조

상가는 현재 별도 entity나 별도 subtype으로 저장되지 않는다. 일반 LH 공고와 동일하게 `announcement`, `announcement_detail`, `announcement_parse_raw`, `announcement_unit` 흐름을 탄다.

| 단계 | 저장 위치 | 상가 공고 처리 방식 |
| - | - | - |
| LH 목록 item 저장 | `announcement` | `PAN_NM`, `DTL_URL`, `CNP_CD_NM`, `AIS_TP_CD_NM` 등이 일반 공고와 동일하게 저장됨 |
| 공급유형 원문 | `announcement.supply_type_raw` | 예: `임대상가(추첨)` |
| 공급유형 정규화 | `announcement.supply_type_normalized` | 현재 normalizer 기준 대부분 `기타` |
| 상세 API 저장 | `announcement_detail` | `dsSbd`, `dsSplScdl`, `dsEtcInfo`, `dsCtrtPlc`가 있으면 일반 상세처럼 저장 |
| 원본 JSON 저장 | `announcement_parse_raw` | `LH_ITEM_JSON`, `API_JSON`, PDF 성공 시 `PDF_AI_JSON` 저장 |
| PDF 파싱 | Gemini | `dsAhflInfo`에서 PDF URL을 찾을 때만 실행 |
| 단위 row 생성 | `announcement_unit` | `dsSbd` 또는 PDF AI `units[]`가 있어야 생성 |

현재 코드에는 상가를 처리하려는 일부 fallback도 존재한다.

| 코드 기준 | 의미 |
| - | - |
| `NoticeImportPersistenceService`의 `SBD_NM` fallback | `LCC_NT_NM`이 없으면 `SBD_NM`을 단지명처럼 사용한다. 코드 주석에 상가용 fallback임이 명시되어 있다. |
| `GeminiClient` prompt | `houseType` 예시에 `상가`가 포함되어 있다. |
| `AnnouncementNormalizer.normalizeHouseType(...)` | 상가는 별도 주택유형으로 정규화되지 않고 `기타`로 떨어진다. |

즉 현재 구조는 "상가를 제외"하는 구조가 아니라 "상가도 들어올 수 있지만 서비스 모델에는 잘 맞지 않는 일반 공고"로 취급하는 구조다.

## 4. 최근 수동 확인 사례

DB reset 후 `POST /api/admin/import/lh?page=1&size=5`로 수집된 최근 LH 공고 중 id 1~4는 `임대상가(추첨)` 공고였다.

해당 공고들의 관찰 결과는 다음과 같다.

| 항목 | 관찰 결과 |
| - | - |
| `announcement` | 저장됨 |
| `announcement_detail` | 일부 저장 가능 |
| `announcement_parse_raw` | `LH_ITEM_JSON`, `API_JSON` 저장됨 |
| `API_JSON` 주요 key | `dsSch`, `resHeader` 수준 |
| `API_JSON.dsAhflInfo` | 없음 |
| `PDF_AI_JSON` | 없음 |
| `announcement_unit` | 0건 |
| fingerprint parse 상태 | PDF URL이 없어 `PENDING` 또는 `NO_PDF` 흐름 |

웹 원문 페이지에는 첨부/PDF 다운로드 흐름이 보였지만, 현재 backend는 웹 HTML을 직접 scrape하지 않는다. 현재 PDF URL 추출은 LH 상세 OpenAPI 응답의 `dsAhflInfo[].AHFL_URL`과 `CMN_AHFL_NM`만 기준으로 한다.

따라서 이 사례의 직접 원인은 상가 전용 차단 로직이 아니라 다음 흐름이다.

```text
LH 목록 공고 수집
→ 상세 OpenAPI 호출
→ detail API 응답에 dsAhflInfo 없음
→ extractPdfUrl(...) 결과 null
→ NO_PDF decision
→ Gemini PDF 파싱 안 함
→ PDF_AI_JSON 없음
→ dsSbd 또는 PDF units[]가 없어 announcement_unit 0건
```

## 5. 현재 skip 로직과의 차이

현재 명확한 skip 대상은 토지 공고다.

| 구분 | 현재 기준 | decision | 의미 |
| - | - | - | - |
| 토지 공고 | `UPP_AIS_TP_CD = "01"` | `LAND_SKIP` | 서비스 대상이 아니므로 import 전 skip |
| PDF 없는 공고 | PDF URL 없음 | `NO_PDF` | 공고 metadata는 저장하되 Gemini 파싱은 skip |
| 상가 공고 | 명시 기준 없음 | 없음 | 일반 LH 공고처럼 처리됨 |

상가를 서비스 대상에서 제외하려면 `NO_PDF`에 우연히 걸리게 두는 것이 아니라, 토지처럼 명시적인 skip 사유를 두는 것이 맞다.

## 6. 상가 공고를 제외하는 것이 나은 이유

주변시세 비교와 맞춤형 공고 알림 서비스 기준에서 상가 공고는 현재 핵심 사용자 흐름과 맞지 않는다.

| 이유 | 설명 |
| - | - |
| 사용자 대상 불일치 | 서비스 핵심 대상은 주거 목적 공공임대 수요자다. 상가는 상업시설 계약 대상이다. |
| 자격조건 모델 불일치 | 청년, 신혼부부, 무주택, 소득/자산 같은 주거 공고 자격조건과 구조가 다르다. |
| 가격 모델 불일치 | 보증금/월세가 있더라도 주거용 임대료 비교와 같은 의미로 보기 어렵다. |
| 주변시세 비교 기준 불일치 | RTMS 주택 실거래가, 전월세 비교와 직접 연결하기 어렵다. |
| 데이터 품질 노이즈 | `unit 0건`, `PDF_AI_JSON 없음`, `eligibility null` 같은 분석 노이즈가 반복된다. |
| 비용 낭비 가능성 | PDF URL이 있는 상가까지 Gemini 파싱하면 서비스 대상이 아닌 데이터에 비용을 사용한다. |

따라서 상가 공고는 현행 서비스 범위에서는 import 대상이 아니라 제외 대상으로 보는 것이 더 자연스럽다.

## 7. 권장 정책

상가 공고는 토지 공고처럼 import 초기에 skip하되, 토지와 구분되는 별도 decision을 둔다.

권장 decision:

```text
COMMERCIAL_SKIP
```

권장 판별 기준:

| 우선순위 | 기준 | 설명 |
| - | - | - |
| 1 | `AIS_TP_CD_NM`에 `상가` 포함 | 운영자가 이해하기 쉽고 실제 원문 의미와 직접 연결됨 |
| 2 | `UPP_AIS_TP_CD = "22"` and `AIS_TP_CD = "24"` | 현재 확인된 `임대상가(추첨)` 코드값 기준 보조 조건 |

권장 판단식은 다음 형태다.

```text
isCommercialNotice =
  AIS_TP_CD_NM contains "상가"
  OR (UPP_AIS_TP_CD == "22" AND AIS_TP_CD == "24")
```

`AIS_TP_CD_NM` 문자열 기준은 LH 코드값이 바뀌거나 일부 공고에서 코드 조합이 다를 때도 의미 기반으로 대응하기 쉽다. 코드값 기준은 오탐을 줄이기 위한 보조 조건으로 둔다.

## 8. 구현 시 수정 포인트

실제 코드 변경을 진행한다면 아래 영역을 함께 수정하는 것이 좋다.

| 영역 | 수정 방향 |
| - | - |
| `LhImportDecisionType` | `COMMERCIAL_SKIP` 추가 |
| `LhImportDedupeDecisionService` | `isCommercialNotice(...)` 추가 후 `LAND_SKIP`과 같은 early decision 처리 |
| `NoticeImportOrchestrator` | import loop에서 `COMMERCIAL_SKIP`이면 detail/PDF 처리 전에 continue |
| `AdminLhImportManagementService` | 후보 수집과 선택 import에서 상가 skip 상태 반영 |
| admin candidate DTO | 필요하면 `isCommercialNotice`, `skippedCommercial` 같은 필드 추가 검토 |
| 문서/API 가이드 | `LAND_SKIP`, `NO_PDF`, `COMMERCIAL_SKIP` 의미를 분리해서 설명 |
| 테스트 | 토지 skip, 상가 skip, PDF 없음 `NO_PDF`가 서로 다른 decision으로 분리되는지 검증 |

## 9. 주의할 점

상가 제외는 PDF 수집 실패를 해결하는 작업이 아니다.

상가를 제외하면 상가 공고의 `unit 0건` 노이즈는 줄어든다. 하지만 주거 공고 중에서도 LH 상세 OpenAPI의 `dsAhflInfo`가 비어 있거나, 실제 목록이 PDF가 아닌 XLSX에만 있는 경우는 계속 발생할 수 있다.

따라서 후속으로는 아래 이슈를 별도 backlog로 관리해야 한다.

| 이슈 | 설명 |
| - | - |
| 웹 HTML 첨부 fallback | OpenAPI `dsAhflInfo`가 없지만 원문 웹 페이지에는 첨부가 있는 경우 |
| XLSX 기반 unit 생성 | 주택 목록이 PDF가 아니라 `.xlsx` 첨부에만 있는 경우 |
| `units=[]` 정책 | PDF는 파싱했지만 Gemini가 unit row를 못 만든 경우 |
| 서비스 대상 공급유형 allowlist | 상가 외에도 서비스 범위 밖 공고를 명확히 제외할지 결정 필요 |

## 10. 최종 판단

현행 코드 기준에서 상가는 명시적으로 skip되지 않는다. 다만 현재 서비스 목적과 주변시세 비교 기능의 데이터 모델을 기준으로 보면 상가는 수집 대상에서 제외하는 것이 맞다.

권장 방향은 다음과 같다.

1. 상가 공고를 `COMMERCIAL_SKIP`으로 명시 제외한다.
2. `LAND_SKIP`과 합치지 않고 통계와 사유를 분리한다.
3. `NO_PDF`는 "서비스 대상이지만 PDF URL을 찾지 못한 공고"에만 남긴다.
4. 주거 공고의 PDF 누락, XLSX unit 목록, `units=[]` 문제는 별도 데이터 품질 backlog로 처리한다.
