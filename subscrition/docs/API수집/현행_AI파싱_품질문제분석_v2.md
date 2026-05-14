# [현행] AI 파싱 품질 문제 분석 v2

> v1 문서: `이전_AI파싱_품질문제분석_v1.md` 참고  
> v2 작성일: 2026-05-07

---

## 이번 세션에서 발견된 문제 및 해결 내용

### 문제 1: Gemini JSON 파싱 오류 로그 레벨 오류

**현상:**
Gemini가 `importantNotes` 등 한국어 텍스트 필드에 이스케이프 없이 따옴표를 포함한 JSON을 반환할 때
`JsonMappingException: Unexpected character ('이'): was expecting comma` 같은 예외가 발생하며
**ERROR** 레벨로 로깅됨.

**문제:**  
이 경우는 text fallback으로 즉시 복구되는 정상 흐름이므로 ERROR가 아닌 WARN이 맞음.
실제 치명적 오류(HTTP 연결 실패, 네트워크 오류)만 ERROR로 남겨야 함.

**해결 (`GeminiClient.java` — `callGemini()` catch 블록 분리):**

```java
// 수정 전
} catch (Exception e) {
    log.error("Gemini parsing failed: mode={}, size={}", mode, size, e);
    return null;
}

// 수정 후
} catch (JsonProcessingException e) {
    log.warn("Gemini response JSON invalid (falling back): mode={}, size={}, error={}", mode, size, e.getMessage());
    return null;
} catch (Exception e) {
    log.error("Gemini parsing failed: mode={}, size={}", mode, size, e);
    return null;
}
```

---

### 문제 2: `full_address` / `complex_name` null

**현상:**  
LH 공고 5건 테스트 결과 주소 필드가 비어있는 케이스 다수 발생.

**원인:**  
`upsertLhDetail()` 내에서 LH 상세 API `dsSbd` 배열의 `LGDN_ADR` / `LCC_NT_NM` 값을
`announcement` 엔티티에 반영하지 않고 있었음.

**해결 (`NoticeImportPersistenceService.upsertLhDetail()`):**

```java
String addr = text(site, "LGDN_ADR");
String lccNm = text(site, "LCC_NT_NM");
String complexNm = lccNm != null ? lccNm : text(site, "SBD_NM"); // 상가는 SBD_NM 사용
if (addr == null && "전국".equals(announcement.getRegionLevel1())) {
    addr = "전국공고(직접확인필요)";
}
announcement.updateAddress(addr, complexNm);
announcementRepository.save(announcement);
```

---

### 문제 3: 공고 유형별 주소/complexName 처리 케이스

LH 상세 API `dsSbd` 배열을 SQL 분석한 결과, 공고 유형에 따라 구조가 다름:

| 공고 유형 | `dsSbd` 배열 | 해결책 |
|----------|------------|-------|
| 일반 주거형 (국민임대 등) | `LGDN_ADR`, `LCC_NT_NM` 포함 | API 값 직접 사용 |
| 상가 (영구임대 단지 내) | `LGDN_ADR` 없음, `SBD_NM`에 상가명 | `SBD_NM`을 complexName fallback으로 사용 |
| 전국 매입임대 | `LGDN_ADR` null, `regionLevel1 = "전국"` | "전국공고(직접확인필요)" 고정 문자열 설정 |
| 집주인임대 | `dsSbd` 자체가 비거나 null | 개별 단독주택 → 단일 주소 없음. 해결 불가 (구조적 한계) |

---

### 문제 4: `house_type_raw` null — LH API 한계

**원인:**  
LH 목록 API(`dsList`)에도, 상세 API(`dsSbd`)에도 주택유형 필드(`HSH_TP_CD_NM` 등)가 없음.
LH 공고 수집 시 `houseTypeRaw`는 항상 null.

**결론:**  
LH API로는 주택유형 취득 불가. 해결책: **Gemini PDF 파싱에서 추출**.
PDF 공고문에는 "아파트", "국민임대아파트" 등 주택유형이 항상 명시됨.

**해결:**

1. **`PdfParseResult.java`**: `String houseType`, `String address` 필드 추가

2. **`GeminiClient.java` SYSTEM_PROMPT**:
   - JSON 스키마에 `"houseType": string|null`, `"address": string|null` 추가
   - 추출 규칙 추가:
     ```
     - houseType: extract the housing type (주택유형). Korean as-is (아파트/빌라/다가구주택 등). null if not found.
     - address: extract the full street address of the housing complex. null if multiple/nationwide.
     ```

3. **`Announcement.java`**: `updateHouseType(String houseTypeRaw)` 메서드 추가

4. **`NoticeImportPersistenceService.java`** — pdfResult 처리 블록에 추가:
   ```java
   // house_type_raw: LH API에 없으므로 PDF 파싱 결과로 채움
   if (pdfResult.houseType() != null) announcement.updateHouseType(pdfResult.houseType());
   // address 보완: API dsSbd에서 못 채운 경우(상가/집주인임대) PDF 주소로 보완
   if (pdfResult.address() != null && announcement.getFullAddress() == null) {
       announcement.updateAddress(pdfResult.address(), null);
   }
   ```

---

### 문제 5: Gemini Free Tier 제한 (10 RPM, 20 RPD) — 운영 전략

**현황:**  
Gemini 2.5 Flash Lite Free tier는 하루 20건만 처리 가능 (RPD=20).
LH 수집은 최대 10페이지×100건 = 1,000건.

**현재 대응:**
- 공고 간 6초 딜레이 (RPM 초과 방지)
- 429 응답 시 30초 대기 후 지수 백오프 재시도 (최대 3회)
- `GeminiRateLimitException`: 최대 재시도 초과 시 throw → Orchestrator에서 catch해 다음 공고로 진행

**향후 고려사항:**
- Gemini 유료 전환 시 RPD 제한 없어짐 → 6초 딜레이 완화 가능
- 웹크롤링 병행은 robots.txt 및 LH 이용약관 검토 필요

---

## 미해결 항목 (구조적 한계)

| 항목 | 원인 | 해결 가능 여부 |
|------|------|-------------|
| 집주인임대 주소 null | 개별 단독주택, 단일 주소 없음 | 불가 (UI에서 "개별 주소 공고" 안내 필요) |
| 상가 주소 null | `dsSbd.LGDN_ADR` 없음, PDF에도 상가 내 위치 표기가 주 | PDF `address` 파싱으로 부분 보완 가능 |
| `heatingType` null | LH API에 없음, PDF에서만 파싱 가능 | 향후 Gemini prompt 확장으로 추가 가능 |
| 분양가 `salePriceMin/Max` DB 미저장 | `PdfParseResult`에 필드는 있으나 `Announcement` 엔티티/응답에 없음 | DB 컬럼 + 응답 DTO 추가 필요 (미구현) |

---

## 검증 방법 (v2 수정사항)

1. `POST /api/admin/import/lh?page=1&size=5` 실행
2. 로그에서 `Gemini response JSON invalid (falling back)` 케이스 → **WARN** 레벨 확인 (ERROR 없어야 함)
3. DB에서 `announcement.full_address`, `complex_name` 값 채워짐 확인
4. `announcement.house_type_raw` — PDF에서 파싱된 주택유형 확인 (예: "아파트", "국민임대아파트")
5. 전국 공고(`region_level1 = '전국'`)의 `full_address = '전국공고(직접확인필요)'` 확인
