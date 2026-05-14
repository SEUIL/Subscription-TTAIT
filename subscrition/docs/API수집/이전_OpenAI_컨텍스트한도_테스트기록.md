# [이전] OpenAI 컨텍스트 한도 처리 테스트 기록

> 현재 정식 수집/파싱 경로는 Gemini 기반으로 이동했으므로 이 문서는 이전 OpenAI 구현 기록으로 보관한다.

## 배경

GPT-4o-mini는 최대 128K 토큰 컨텍스트 한도를 가진다. PDF 텍스트가 길 경우 한도를 초과해 OpenAI API 오류가 발생했으며, 이를 처리하기 위해 `OpenAiClient`에 truncate 로직을 추가했다.

**수정 파일**: `external/ai/OpenAiClient.java`
- `MAX_TEXT_CHARS = 200_000` 상수 추가
- 전송 전 텍스트 길이 검사 → 초과 시 앞 200,000자로 잘라서 전송 + `warn` 로그
- catch 블록 분기: context 초과 오류 → `warn`, 그 외 → `error` (텍스트 길이 포함)

---

## 테스트 파일

**경로**: `src/test/java/com/ttait/subscription/external/ai/OpenAiClientTest.java`

**테스트 방식**: RestClient 전체 목(mock) 처리 (실제 OpenAI 호출 없음)
- `ArgumentCaptor`로 OpenAI에 실제 전달된 body의 user 메시지 content를 캡처해 글자 수 검증
- 오류 시나리오는 `responseSpec.body(JsonNode.class)`가 예외를 던지도록 설정

---

## 테스트 결과 (2026-05-06)

### 환경
- Java 17, Spring Boot 3.3.2
- JUnit 5 + Mockito (MockitoExtension)

### 테스트 케이스

#### 텍스트 길이 제한 처리

| 테스트 | 입력 | 기대 결과 | 결과 |
|--------|------|----------|------|
| 200,000자 초과 텍스트는 잘라서 전송한다 | `"가".repeat(250_000)` (250,000자) | OpenAI에 200,000자만 전달 | ✅ |
| 200,000자 이하 텍스트는 원본 그대로 전송된다 | `"나".repeat(100_000)` (100,000자) | OpenAI에 100,000자 그대로 전달 | ✅ |
| 정확히 200,000자는 잘리지 않는다 | `"다".repeat(200_000)` (200,000자) | OpenAI에 200,000자 그대로 전달 | ✅ |

250,000자 입력 시 로그 출력 확인:
```
WARN OpenAiClient -- PDF text truncated for OpenAI context limit: original=250000chars, truncated=200000chars
```

#### OpenAI API 오류 처리

| 테스트 | 모킹 예외 메시지 | 기대 결과 | 결과 |
|--------|----------------|----------|------|
| context_length_exceeded 오류 시 null 반환 | `"context_length_exceeded - maximum context length is 128000"` | null 반환, 예외 미전파 | ✅ |
| maximum context length 오류 시 null 반환 | `"This model's maximum context length is 128000 tokens"` | null 반환, 예외 미전파 | ✅ |
| 인증 오류 등 일반 오류 시 null 반환 | `"401 Unauthorized"` | null 반환, 예외 미전파 | ✅ |

### 전체 결과

| 그룹 | 테스트 수 | 성공 | 실패 |
|------|----------|------|------|
| 텍스트 길이 제한 처리 | 3 | 3 | 0 |
| OpenAI API 오류 처리 | 3 | 3 | 0 |
| **합계** | **6** | **6** | **0** |

---

## 검증 포인트

1. **truncate 후 원본 변경 없음**: 250,000자 입력 → 200,000자로 잘려서 전송, 잘린 부분은 `"가"` 연속이므로 정확히 `"가".repeat(200_000)` 과 일치하는지 확인
2. **경계값(200,000자)**: 초과가 아닌 경우 잘리지 않아야 함 (`> MAX_TEXT_CHARS` 조건)
3. **오류 격리**: API 오류가 발생해도 예외가 호출자에게 전파되지 않고 null 반환

---

## 관련 파일

| 파일 | 역할 |
|------|------|
| `external/ai/OpenAiClient.java` | truncate 및 오류 처리 로직 |
| `external/ai/OpenAiClientTest.java` | 단위 테스트 |
| `docs/API수집/현행_LH공고수집_PDF_AI파싱_기능명세.md` | 에러 처리 정책 전체 명세 |
