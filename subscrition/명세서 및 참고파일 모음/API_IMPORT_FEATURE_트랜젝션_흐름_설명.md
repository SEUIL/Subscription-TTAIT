# LH 공고 수집 — 트랜잭션 흐름 설명

> `API_IMPORT_FEATURE.md`의 처리 흐름 섹션에 대한 심화 설명.
> "왜 이렇게 만들었는가"를 이해하기 위한 문서.

---

## 전체 흐름 요약

공고 1건 처리 = **TX1 → 외부 작업 → TX2** 순서로 자동 실행됨.
"트랜잭션 밖"이라는 뜻은 수동 실행이 아니라, `@Transactional` 경계 밖에서 순서대로 실행된다는 의미.

```
processLhItem(item)                  ← NoticeImportOrchestrator
  │
  ├─ [TX1] persistenceService.upsertLh(item)
  │         → Announcement upsert
  │         → AnnouncementParseRaw "LH_ITEM_JSON" 저장
  │         TX1 커밋 ✅
  │
  ├─ (트랜잭션 없음) lhApiClient.fetchNoticeDetail(...)   ← LH 상세 API 호출
  ├─ (트랜잭션 없음) extractPdfUrl(detailResponse)        ← PDF URL 추출
  ├─ (트랜잭션 없음) pdfParsingService.parse(pdfUrl)      ← PDF 다운로드 + OpenAI 파싱
  │
  └─ [TX2] persistenceService.upsertLhDetail(...)
            → AnnouncementDetail 저장
            → AnnouncementEligibility 저장
            → AnnouncementParseRaw "API_JSON", "PDF_AI_JSON" 저장
            → AnnouncementCategory 저장
            TX2 커밋 ✅
```

---

## LH 상세 API가 반환하는 내용

목록 API와 상세 API는 역할이 다름.

| API | 반환 내용 |
|-----|---------|
| 목록 API (`lhLeaseNoticeInfo1`) | 공고명, 지역, 공고 기간 등 기본 정보 |
| 상세 API (`lhLeaseNoticeDtlInfo1`) | 세부 공고 정보 + **`dsAhflInfo` (첨부파일 목록)** |

**핵심은 `dsAhflInfo` 배열** — 여기서 `.pdf` 확장자를 가진 항목을 필터링해 PDF URL을 얻음.
상세 API를 부르지 않으면 PDF를 어디서 다운받아야 할지 알 수 없음.

```
목록 API → "이 공고가 있다"
상세 API → "이 공고의 PDF는 이 URL에 있다"
```

상세 API 호출 파라미터: `PAN_ID`, `CCR_CNNT_SYS_DS_CD`, `SPL_INF_TP_CD` (목록 API 응답에서 추출)

---

## 왜 외부 API 호출을 트랜잭션 밖에 두었나

### 이유 1 — DB 커넥션 점유 문제

트랜잭션을 열어두면 DB 커넥션이 그 시간 동안 계속 묶임.

```
[트랜잭션 안에서 외부 호출 시]

커넥션 획득
  ↓
LH 상세 API 응답 대기  (수백ms ~ 수초)
  ↓
PDF 다운로드            (수십 MB, 수 ~ 수십초)
  ↓
OpenAI GPT 응답 대기   (10 ~ 30초)
  ↓
커넥션 반환
```

공고 100건을 처리하면 커넥션 100개가 각각 30초씩 묶임.
커넥션 풀 기본값(HikariCP 기본 10개)이면 즉시 고갈 → 전체 서비스 장애.

트랜잭션 밖에 두면 외부 작업 중에는 커넥션을 들고 있지 않음.

### 이유 2 — 외부 API 호출은 트랜잭션으로 롤백 불가

DB 작업과 달리 외부 HTTP 호출은 트랜잭션 롤백으로 되돌릴 수 없음.

```
[문제 시나리오]
트랜잭션 안에서 OpenAI 호출 → 비용 발생
→ 이후 DB 저장 실패 → 트랜잭션 롤백
→ DB는 롤백됐지만 OpenAI 비용은 이미 청구됨
```

트랜잭션 밖에 두면 "이 코드는 롤백 대상이 아님"을 코드 레벨에서 명확히 표현.

### 이유 3 — 에러 격리 (의도적 설계)

TX1에서 공고 기본 정보를 먼저 저장해두고, 이후 외부 작업이 실패해도 공고는 남아있게 함.

| 상황 | 결과 |
|------|------|
| TX1 성공 + 상세 API 실패 | 공고는 저장됨, 상세 정보만 없음 |
| TX1 성공 + PDF 다운로드 실패 | 공고는 저장됨, PDF 파싱 결과만 없음 |
| TX1 성공 + OpenAI 실패 | 공고는 저장됨, raw 필드 null |
| TX1 성공 + TX2 실패 | 공고는 저장됨, 상세/자격조건/카테고리만 미저장 |
| TX1 자체 실패 | 해당 공고 `failed` 카운트, 나머지 공고 계속 처리 |

이 구조 덕분에 **REIMPORT 기능**도 가능함.
TX1에서 `LH_ITEM_JSON`(원본 JSON)을 저장해두기 때문에, 나중에 이를 복원해 OpenAI만 재실행할 수 있음.

---

## 관련 코드 위치

| 역할 | 파일 |
|------|------|
| TX1 + TX2 경계 정의 | `external/service/NoticeImportPersistenceService.java` |
| 외부 작업 + 흐름 조율 | `external/service/NoticeImportOrchestrator.java` |
| LH API 클라이언트 | `external/lh/LhApiClient.java` |
| PDF 다운로드 + 텍스트 추출 | `external/pdf/PdfTextExtractor.java` |
| OpenAI 파싱 | `external/pdf/PdfParsingService.java`, `external/ai/OpenAiClient.java` |
