# LH API 데이터 품질 분석

분석 일자: 2026-05-05  
분석 대상: DB에 수집된 LH 공고 5건 (announcement ID 1~5)

---

## 요약

| 문제 | 원인 | 수정 여부 |
|------|------|----------|
| 이메일 발신자 이름 깨짐 | Java Mail 인코딩 미지정 | ✅ 수정 완료 |
| 토지 공고가 수집됨 | 공급유형 필터링 누락 | ✅ 수정 완료 |
| application_start_date 항상 null | 상세 API 날짜 미반영 | ✅ 수정 완료 |
| region_level2 항상 null | LH API 자체 한계 | ⚠️ 구조적 한계 |
| house_type_normalized 일부 null | LH API에 주택유형 필드 없음 | ⚠️ 구조적 한계 |

---

## 1. 이메일 발신자 이름 인코딩 문제

### 증상
```
발신자: ì²­ì•½ë"°ìž‡ <together580428@gmail.com>
기대값: 청약따잇 <together580428@gmail.com>
```

### 원인
`MimeMessageHelper.setFrom(fromEmail, fromName)` 호출 시 내부적으로 `MimeUtility.encodeText()`를 사용하는데, 일부 Gmail 클라이언트가 이 인코딩 방식을 잘못 해석함. UTF-8 바이트가 Latin-1로 표시되는 전형적인 인코딩 불일치.

### 수정
`SmtpEmailSender.java`에서 `InternetAddress` 3인수 생성자 사용으로 변경:

```java
// Before
helper.setFrom(fromEmail, fromName);

// After
helper.setFrom(new InternetAddress(fromEmail, fromName, "UTF-8"));
```

`InternetAddress(address, personal, charset)` 생성자는 UTF-8 charset을 명시적으로 지정하여 RFC 2047 MIME encoded-word 형식으로 정확하게 인코딩.

---

## 2. LH 수집 데이터 null 필드 분석

### 수집된 공고 현황

| ID | 공고명 | 유형 | 실제 문제 여부 |
|----|--------|------|--------------|
| 1 | 과천지식정보타운 가스충전소용지 공급 재공고 | **토지** | 토지 공고 — 수집 대상 아님 |
| 2 | 과천지식정보타운 근린생활시설용지 공급공고 | **토지** | 토지 공고 — 수집 대상 아님 |
| 3 | 과천지식정보타운 단독주택(주거전용)용지 공급공고 | **토지** | 토지 공고 — 수집 대상 아님 |
| 4 | 양주옥정 A8BL,A16BL 영구임대주택 | **영구임대** | 대부분 정상, 시작일만 누락 |
| 5 | 효천LH천년나무2단지 잔여세대 일반매각 | **분양/매각** | 비표준 공고, AI 파싱 어려움 |

---

### 버그 1: 토지(土地) 공고 수집

**원인**: LH API에서 공급유형(`UPP_AIS_TP_CD`)이 `"01"` (토지)인 공고를 필터링하지 않아서 수집됨.

토지 공고(`UPP_AIS_TP_CD=01`)는 다음 이유로 수집 대상이 아님:
- 보증금/월세 개념 없음 → deposit_amount, monthly_rent_amount 항상 null
- 주택 유형 없음 → house_type_normalized null
- 청약 자격 조건 없음 → eligibility 전체 null
- PDF에서 confidence 0.0으로 AI가 "정보 없음" 정확히 인식

**수정**: `NoticeImportOrchestrator.importLhNotices()`에 필터 추가:
```java
if ("01".equals(uppAisTpCd)) { // 토지: 주택 서비스 대상 아님
    continue;
}
```

---

### 버그 2: application_start_date 항상 null

**원인**: `NoticeImportPersistenceService.upsertLh()` 메서드에서 LH 목록 API(`CLSG_DT`=마감일만 있음)를 기반으로 announcement를 생성할 때 `applicationStartDate`를 `null`로 하드코딩:

```java
announcement.updateFromImport(
    ...,
    announcementDate, null, endDate, null,  // ← applicationStartDate = null 고정
    ...
);
```

실제 시작일(`SBSC_ACP_ST_DT`)은 상세 API(`dsSplScdl`)에 있는데 `AnnouncementDetail`에만 저장되고 `Announcement` 엔티티에 반영 안 됨.

공고 4 확인:
- 상세 API: `SBSC_ACP_ST_DT = "2026.05.19"` 존재
- DB announcement.application_start_date = NULL ← 반영 안 됨

**수정**: `upsertLhDetail()`에서 announcement에도 업데이트 추가:
```java
LocalDate startDate = DateParsers.parseDate(text(schedule, "SBSC_ACP_ST_DT"));
announcement.updateApplicationStartDate(startDate);
announcementRepository.save(announcement);
```

`updateApplicationStartDate()`는 기존 값이 null일 때만 업데이트 (이미 설정된 값 보호).

---

### 구조적 한계 (LH API 자체 문제)

#### region_level2 항상 null

LH 목록 API는 `CNP_CD_NM` 필드로 시/도(예: "경기도", "서울특별시") 레벨만 제공. 시/군/구(강남구, 분당구 등)는 제공하지 않음.

**현재 상태**: 상세 API의 `LGDN_ADR` (단지 주소 예: "경기도 의왕시 포일동 1234")에서 파싱 가능하지만 구현되어 있지 않음.

**향후 개선 방향**: AnnouncementNormalizer에 주소 파싱 로직 추가 필요.

#### house_type_normalized 일부 null

LH API `AIS_TP_CD_NM`은 공급 유형(영구임대, 국민임대 등)이고 주택 유형(아파트, 빌라 등)은 별도 필드가 없음. 상세 API의 건물 정보나 공고명에서 추론해야 하는데 현재 미구현.

---

## 3. AI 파싱 품질 평가

### AI가 잘 동작한 케이스

**공고 4 (영구임대)** — PDF 파싱 성공:
```json
{
  "applicationPeriod": { "value": "2026.05.19(화) ~ 2026.05.22(금)", "confidence": 1.0 },
  "depositMonthlyRent": { "value": "보증금 2,469,000원, 월임대료 123,400원", "confidence": 0.8 },
  "incomeAssetCriteria": { "value": "도시근로자 가구원수별 가구당 월평균 소득 150% 이하", "confidence": 0.9 }
}
```
→ DB에 deposit_amount=2469, monthly_rent_amount=123 정상 저장됨

### AI가 정확히 "모름"을 반환한 케이스

**공고 1, 2, 3 (토지)** — confidence 0.0 전체:
```json
{
  "applicationPeriod": { "value": null, "confidence": 0.0 },
  "depositMonthlyRent": { "value": null, "confidence": 0.0 }
}
```
→ 토지 공고 PDF에는 보증금/청약 정보가 없으므로 올바른 반응. AI 오류 아님.

**공고 5 (잔여세대 매각)** — confidence 0.0 전체:
→ "잔여세대 일반매각" 비표준 포맷. 표준 청약 공고가 아니므로 AI가 파싱 어려운 것이 정상.

### 결론

AI 파싱 자체는 제대로 동작함. null의 주요 원인:
1. 토지/매각 같은 비주택 공고 수집 (필터링으로 해결)
2. application_start_date 코드 버그 (수정 완료)
3. LH API 자체 데이터 한계 (region_level2, house_type)

---

## 4. 수정 완료 파일 목록

| 파일 | 수정 내용 |
|------|----------|
| `notification/email/sender/SmtpEmailSender.java` | `InternetAddress(email, name, "UTF-8")` 사용으로 발신자 이름 인코딩 수정 |
| `announcement/domain/Announcement.java` | `updateApplicationStartDate()` 메서드 추가 |
| `external/service/NoticeImportPersistenceService.java` | `upsertLhDetail()`에서 SBSC_ACP_ST_DT로 announcement startDate 업데이트 |
| `external/service/NoticeImportOrchestrator.java` | `UPP_AIS_TP_CD="01"` (토지) 공고 수집 필터링 추가 |

---

## 5. 다음 수집 시 예상 개선 효과

- 토지 공고 미수집 → DB에 관련 없는 공고 축적 방지
- application_start_date 정상 파싱 → `calculateStatus()` 정확도 향상 (SCHEDULED/OPEN 구분)
- 이메일 발신자 이름 정상 표시
