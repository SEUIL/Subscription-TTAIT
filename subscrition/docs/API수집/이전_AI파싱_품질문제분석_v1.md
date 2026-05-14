# [이전] AI 파싱 품질 문제 분석 v1

## 발생 현상

`announcement_detail` 테이블 id=7 (효천LH천년나무2단지 잔여세대 분양전환 공고, 2026-02-25)의
파싱 결과 주요 필드가 비어있음. 동일 PDF를 Gemini로 분석하면 아래 정보를 정확히 추출함:

| Gemini 추출 결과 | 우리 시스템 결과 |
|----------------|---------------|
| 분양가격: 1억8000만~2억1850만원 | null (필드 없음) |
| 청약신청: 2026.03.10~03.12 | 불명확 또는 null |
| 순번추첨: 2026.03.18 | 누락 |
| 사전개방: 2026.03.19~03.20 | 누락 |
| 계약체결: 2026.03.25~03.26 | 누락 |
| 상시계약: 2026.04.01~ | 누락 |
| 공급규모: 총 10호 | 불명확 |
| 계약금 10%, 잔금 60일 | 필드 없음 |
| 필수 유의사항 3개 항목 | 필드 없음 |

---

## 근본 원인 분석

### 원인 1: PDFBox 텍스트 추출 품질 문제 (핵심)

**파일**: `external/pdf/PdfTextExtractor.java`

`PDFTextStripper`는 PDF 텍스트를 **좌→우, 위→아래 선형**으로 읽음.
LH 공고 PDF는 아래 레이아웃을 빈번하게 사용하는데 PDFBox가 이를 처리 못함:

| PDF 구조 | PDFBox 결과 |
|---------|-----------|
| 일정표 (표 형태) | 행/열이 뒤섞인 의미없는 텍스트 |
| 가격표 (2~3컬럼) | 컬럼 내용이 합쳐져서 순서 파괴 |
| 절대 좌표 텍스트 | 렌더링 순서와 추출 순서 불일치 |

표 구조가 포함된 LH 공고 PDF에서 이 문제가 가장 자주 발생하며, 분양전환·잔여세대
공고처럼 가격표와 일정표가 복잡한 경우 특히 심각함.

---

### 원인 2: 시스템 프롬프트가 임대(賃貸) 전용으로 설계됨

현재 `PdfParseResult`에 존재하는 금액 필드:

```java
Long depositAmountManwon;      // 보증금 (임대 전용)
Long monthlyRentAmountManwon;  // 월세 (임대 전용)
```

id=7 공고는 **분양전환** 공고라 핵심 정보가 분양가격(1억8천~2억1천만원)인데,
이를 저장할 필드가 없음 → AI가 추출해도 어디에도 저장되지 않고 버려짐.

또한 일정이 복수인데 단일 string 필드 하나(`applicationPeriod`)에 다 욱여넣으려다 AI가 하나만 선택하거나 생략함.

---

### 원인 3: gpt-4o-mini 모델 한계

| 항목 | gpt-4o-mini | Gemini 2.0 Flash |
|------|------------|----------------|
| Vision 지원 | 없음 (텍스트 전용) | 있음 (PDF 직접 처리) |
| 깨진 표 텍스트 재조합 | 제한적 | 시각적으로 직접 인식해 불필요 |
| 무료 일일 한도 | - | 1,500 RPD |

---

## ✅ 해결 완료 (2026-05-06)

### Phase 1 — DTO + 프롬프트 + DB 확장

**`PdfParseResult.java` 추가 필드:**

```java
String noticeType;               // "임대"|"분양"|"분양전환"|"잔여세대"|"기타"
Long salePriceMinManwon;         // 최소 분양가 (만원)
Long salePriceMaxManwon;         // 최대 분양가 (만원)
Field salePriceRaw;              // 분양가 원문
List<ScheduleItem> scheduleDetails;  // 복수 일정 배열
Field importantNotes;            // 유의사항 원문
// ScheduleItem { String scheduleType, String startDate, String endDate }
```

**SYSTEM_PROMPT 수정:**
- `noticeType` 판별 규칙 추가 (키워드: "분양전환", "잔여세대", "임대", "분양")
- `salePriceRange` 추출 규칙 추가 (분양가격/분양금액 섹션)
- `scheduleDetails` 배열로 복수 일정 분리 추출
- `importantNotes` 추출 규칙 추가
- `depositAmountManwon`/`monthlyRentAmountManwon`은 임대 공고에만 유효하다고 명시

**DB 변경 (`announcement_detail` 컬럼 추가):**
- `notice_type` VARCHAR(20)
- `sale_price_raw` TEXT
- `schedule_details_json` TEXT  ← ScheduleItem 배열 JSON
- `important_notes_raw` TEXT

---

### Phase 2 — AI 모델 교체 + PDF 직접 처리

**OpenAI gpt-4o-mini → Gemini 2.0 Flash 전환 이유:**
- Gemini는 PDF를 직접 처리 가능 (`inline_data` + `application/pdf`) → 이미지 변환 불필요
- Vision 기본 내장 → 표/다단 레이아웃 정확 인식
- 무료 1,500 RPD (기존 gpt-4o-mini 유료 대비 압도적 유리)

**파이프라인 변경:**

```
[Before]
PDF URL → PDFBox 텍스트 추출 (표 구조 파괴) → gpt-4o-mini 파싱 → 저장

[After]
PDF URL → PDF bytes 다운로드 → Gemini 2.0 Flash (PDF 직접, Vision 인식) → 저장
                              ↓ 실패 시 fallback
                         PDFBox 텍스트 추출 → Gemini 텍스트 파싱 → 저장
```

**변경된 파일:**

| 파일 | 변경 내용 |
|------|---------|
| `external/ai/GeminiClient.java` | 신규 생성 (parsePdf + parseText) |
| `external/ai/GeminiProperties.java` | 신규 생성 (`gemini.*` 설정) |
| `external/pdf/PdfTextExtractor.java` | `downloadBytes()` 메서드 추가 |
| `external/pdf/PdfParsingService.java` | Gemini PDF 직접 전송 + 텍스트 fallback |
| `application.properties` | `openai.*` → `gemini.*` |
| `external/ai/OpenAiClient.java` | 삭제 |
| `external/ai/OpenAiProperties.java` | 삭제 |

**환경변수:** `.env`에 `GEMINI_API_KEY` 설정 필요

---

## 검증 방법

1. 앱 기동 → `announcement_detail`에 4개 컬럼 자동 생성 확인 (DDL auto-update)
2. `POST /api/admin/import/lh?page=1&size=3` → 로그에서 `Gemini PDF parse: url=...` 확인
3. `GET /api/admin/review/{id}` → `noticeType`, `salePriceRaw`, `scheduleDetailsJson` 필드 확인
4. 분양전환 공고에서 `notice_type=분양전환`, `sale_price_raw` 값 존재 확인
