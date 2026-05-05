# 이메일 알리미 테스트 기록

테스트 일자: 2026-05-05

---

## 1. 컴파일 검증

```
./gradlew compileJava
BUILD SUCCESSFUL in 17s
```

신규 추가 클래스 14개, 수정 클래스 5개 모두 컴파일 오류 없음.

---

## 2. 단위 테스트 결과

실행 명령: `./gradlew test --tests "com.ttait.subscription.notification.*"`

**결과: 20개 테스트 전원 통과 (BUILD SUCCESSFUL)**

### FavoriteServiceTest (6개)

| 테스트명 | 결과 |
|---------|------|
| add — 이미 즐겨찾기한 경우 중복 저장하지 않는다 | PASS |
| add — 공고가 존재하지 않으면 404 예외를 던진다 | PASS |
| add — 소프트 삭제된 공고는 404 예외를 던진다 | PASS |
| add — 정상 공고를 즐겨찾기에 추가한다 | PASS |
| remove — 즐겨찾기가 없으면 404 예외를 던진다 | PASS |
| remove — 즐겨찾기를 정상적으로 삭제한다 | PASS |
| exists — 즐겨찾기 여부를 반환한다 | PASS |
| list — 즐겨찾기 목록을 FavoriteResponse로 변환해 반환한다 | PASS |

### RecommendationEmailServiceTest (5개)

| 테스트명 | 결과 |
|---------|------|
| sendFor — 추천 공고가 없으면 이메일을 발송하지 않는다 | PASS |
| sendFor — 모든 공고가 이미 발송된 경우 이메일을 발송하지 않는다 | PASS |
| sendFor — 신규 공고가 있으면 이메일을 발송하고 성공 로그를 저장한다 | PASS |
| sendFor — 이메일 발송 실패 시 실패 로그를 저장하고 예외를 삼킨다 | PASS |
| sendFor — 추천 서비스 예외 발생 시 이메일 발송 없이 조용히 종료된다 | PASS |

### DeadlineEmailServiceTest (7개)

| 테스트명 | 결과 |
|---------|------|
| sendFor — 즐겨찾기가 없으면 이메일을 발송하지 않는다 | PASS |
| sendFor — 마감 7일 전 공고에 DEADLINE_7 이메일을 발송한다 | PASS |
| sendFor — 마감 3일 전 공고에 DEADLINE_3 이메일을 발송한다 | PASS |
| sendFor — 마감 1일 전 공고에 DEADLINE_1 이메일을 발송한다 | PASS |
| sendFor — 이미 발송된 타입은 중복 발송하지 않는다 | PASS |
| sendFor — applicationEndDate가 null인 공고는 건너뛴다 | PASS |
| sendFor — 마감일과 관계없는 날짜에는 이메일을 발송하지 않는다 | PASS |

---

## 3. 빌드 검증

```
./gradlew build -x test
BUILD SUCCESSFUL in 1s
```

JAR 아티팩트 정상 생성 확인.

---

## 4. SMTP 연결 테스트

PowerShell로 Gmail SMTP 직접 연결 테스트 수행.

**계정**: together580428@gmail.com  
**방식**: Gmail 앱 비밀번호 (2단계 인증 활성화)  
**결과**: SUCCESS — 이메일 발송 성공

```
[테스트 결과] SUCCESS: 이메일 발송 성공
```

---

## 5. 테스트 범위 및 검증 항목

### 검증된 비즈니스 로직

| 항목 | 검증 방법 |
|------|----------|
| 즐겨찾기 멱등성 (중복 추가 방지) | FavoriteServiceTest.add_alreadyExists_skips |
| 소프트 삭제 공고 즐겨찾기 거부 | FavoriteServiceTest.add_deletedAnnouncement_throws |
| 맞춤 공고 중복 발송 방지 (success=true 기준) | RecommendationEmailServiceTest.sendFor_allAlreadySent_skips |
| 이메일 발송 실패 시 로그 저장 + 예외 swallow | RecommendationEmailServiceTest.sendFor_sendFailure_savesFailureLog |
| 프로필 미완성 사용자 skip | RecommendationEmailServiceTest.sendFor_recommendationServiceThrows_skips |
| D-7/3/1 날짜 매칭 정확도 | DeadlineEmailServiceTest.sendFor_deadlineIn7Days/3Days/1Day |
| 마감 리마인드 중복 발송 방지 | DeadlineEmailServiceTest.sendFor_alreadySentType_skips |
| applicationEndDate null 안전 처리 | DeadlineEmailServiceTest.sendFor_nullEndDate_skips |
| D-5 등 비대상 날짜 무시 | DeadlineEmailServiceTest.sendFor_nonTargetDate_skips |

### 미커버 항목 (수동 검증 필요)

| 항목 | 이유 |
|------|------|
| 스케줄러 cron 실행 타이밍 | H2 환경 없어 Spring 통합 테스트 미작성 |
| 실제 이메일 수신 확인 | SMTP 계정 인증 필요, 수동 확인 |
| Thymeleaf 템플릿 렌더링 결과물 | 브라우저에서 직접 확인 필요 |
| 100건 단위 사용자 페이지네이션 | DB 데이터 필요 |

---

## 6. 발견된 버그 및 수정 이력

| 버그 | 원인 | 수정 내용 |
|------|------|----------|
| 발송 실패 공고 영구 차단 | `findAlreadySentAnnouncementIds` 쿼리에 `success=true` 조건 누락 | `AND l.success = true` 추가 |
| 트랜잭션 경계 오류 | `sendFor`에 `@Transactional` 있으면 이메일 발송 후 롤백 시 로그 미저장 | `@Transactional` 제거, repository 자체 트랜잭션에 위임 |
| JOIN FETCH + Page 인메모리 페이지네이션 경고 | countQuery 미분리 | `countQuery` 별도 설정 |
| POST 즐겨찾기 200 반환 | `ResponseEntity.ok()` 사용 | `ResponseEntity.status(201).build()` 로 수정 |

---

## 7. 다음 단계 (수동 검증 체크리스트)

서버 기동 후 Swagger UI(`/swagger-ui/index.html`)에서 직접 확인:

- [ ] `POST /api/me/favorites` → 201 반환, DB `user_favorite_announcement` row 생성 확인
- [ ] 동일 공고 재추가 → 200 반환, DB row 중복 없음 확인
- [ ] `GET /api/me/favorites` → 즐겨찾기 목록 반환 확인
- [ ] `DELETE /api/me/favorites/{id}` → 204, DB row 삭제 확인
- [ ] 스케줄러 메서드 직접 호출 테스트 (`EmailNotificationScheduler.sendDailyRecommendations()`)
- [ ] 실제 이메일 수신 확인 (together580428@gmail.com → sone91961@gmail.com)
- [ ] `email_notification_log` 테이블 row 확인
- [ ] 동일 메서드 2회 호출 → 2회차 이메일 미발송, 로그 1개만 존재 확인
