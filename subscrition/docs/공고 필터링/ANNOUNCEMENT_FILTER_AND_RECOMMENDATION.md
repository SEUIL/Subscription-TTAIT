# 공고 필터링 & 맞춤 추천 구현 분석

> 작성 기준: `dbd2fcdd` 커밋 (2026-04-28, dlgusgh0810)  
> 분석 대상: 전체 공고 리스트업/검색, 맞춤형 공고 추천 리스트

---

## 1. 구현 개요

### 커밋 내역

| 커밋 | 날짜 | 내용 |
|------|------|------|
| `dbd2fcdd` | 2026-04-28 | 유저 profileCompleted 추가, 공고 필터링 + 추천 구현 |
| `6171bf60` | 2026-05-03 | 맞춤공고 API 테스트 가이드 추가 |

### 추가/변경된 파일

| 파일 | 구분 |
|------|------|
| `AnnouncementController.java` | 필터 파라미터 확장 |
| `FilterController.java` | 필터 옵션 API (신규) |
| `RecommendationController.java` | 맞춤 추천 API (신규) |
| `AnnouncementQueryService.java` | 다중 필터 + 페이징 구현 |
| `RecommendationService.java` | 추천 스코어링 로직 (신규) |
| `RecommendationItemResponse.java` | 추천 결과 DTO (신규) |
| `AnnouncementRepository.java` | 필터 옵션용 DISTINCT 쿼리 추가 |
| `AnnouncementCategoryRepository.java` | `findByAnnouncementIdIn` 추가 |
| `AnnouncementEligibilityRepository.java` | `findByAnnouncementIdIn` 추가 |
| `UserProfile.java` | 추천 연산용 필드 확장 (8개 추가) |
| `UserProfileService.java` | `markProfileCompleted()` 호출 추가 |

---

## 2. 공고 필터링 (AnnouncementQueryService)

### 2.1 API

```
GET /api/announcements
  ?regionLevel1=서울특별시
  &regionLevel2=강남구
  &supplyType=공공임대
  &houseType=아파트
  &provider=LH
  &status=OPEN
  &keyword=청년
  &minDeposit=0
  &maxDeposit=5000
  &minMonthlyRent=0
  &maxMonthlyRent=30
  &categories=YOUTH,NEWLYWED
  &page=0
  &size=20
```

모든 파라미터 선택적(required = false). `page=0`, `size=20` 기본값.

### 2.2 처리 흐름

```
AnnouncementController.getAnnouncements()
  → announcementRepository.findByDeletedFalseAndMergedFalse(Pageable.unpaged())
      ↳ 삭제 안 된, 병합 안 된 공고 전체 로드
  → announcementCategoryRepository.findByAnnouncementIdIn(ids)
      ↳ 카테고리 맵 빌드: Map<Long, Set<CategoryCode>>
  → Stream 필터링 (아래 조건들 AND 결합)
  → 정렬: applicationEndDate 오름차순 → announcementId 내림차순
  → PageImpl(subList, pageable, total)
```

### 2.3 필터 조건별 구현

| 파라미터 | 구현 메서드 | 동작 방식 |
|----------|------------|-----------|
| `regionLevel1` | `matchesRegionLevel1` | `equalsIgnoreCase` |
| `regionLevel2` | `matchesRegionLevel2` | `equalsIgnoreCase` |
| `supplyType` | `matchesSupplyType` | `equalsIgnoreCase` |
| `houseType` | `matchesHouseType` | `equalsIgnoreCase` |
| `provider` | `matchesProvider` | `equalsIgnoreCase` |
| `status` | `matchesStatus` | `AnnouncementStatus.valueOf()` |
| `keyword` | `matchesKeyword` | noticeName, providerName, complexName, fullAddress 4개 필드 `contains` |
| `minDeposit`/`maxDeposit` | `matchesDeposit` | null-safe 범위 비교 |
| `minMonthlyRent`/`maxMonthlyRent` | `matchesMonthlyRent` | null-safe 범위 비교 |
| `categories` | `matchesCategories` | categoryMap 우선, 없으면 키워드 폴백 |

#### 카테고리 필터 — 2단계 처리

```java
private boolean matchesCategories(Announcement a, List<CategoryCode> categories,
                                   Map<Long, Set<CategoryCode>> categoryMap) {
    // 1단계: announcement_category 테이블에 데이터 있으면 그걸로 판단
    Set<CategoryCode> stored = categoryMap.get(a.getId());
    if (stored != null && !stored.isEmpty()) {
        return categories.stream().anyMatch(stored::contains);
    }
    // 2단계: 카테고리 데이터 없으면 공고명/주소 텍스트에서 키워드로 추론 (폴백)
    return categories.stream().anyMatch(c -> matchesCategoryByKeyword(a, c));
}
```

키워드 폴백 기준:

| CategoryCode | 키워드 |
|---|---|
| YOUTH | 청년, 대학생, 청년매입 |
| NEWLYWED | 신혼, 예비신혼, 혼인, 부부 |
| HOMELESS | 무주택 |
| ELDERLY | 고령자, 만 65세, 노인 |
| LOW_INCOME | 저소득, 기초급여, 차상위 |
| MULTI_CHILD | 다자녀 |

### 2.4 필터 옵션 API (FilterController)

```
GET /api/filters/regions          → 시/도 목록 (DISTINCT)
GET /api/filters/regions/level2   → 시/군/구 목록 (DISTINCT)
GET /api/filters/supply-types     → 공급유형 목록 (DISTINCT)
GET /api/filters/house-types      → 주택유형 목록 (DISTINCT)
GET /api/filters/providers        → 공급기관 목록 (DISTINCT)
GET /api/filters/categories       → 6개 카테고리 코드+한글명 목록 (하드코딩)
```

각 옵션 쿼리는 `deleted=false AND merged=false AND 필드 IS NOT NULL AND 필드 <> ''` 조건으로 JPQL DISTINCT를 사용합니다.

---

## 3. 맞춤 추천 (RecommendationService)

### 3.1 API

```
GET /api/recommendations?page=0&size=20
  Authorization: Bearer {token}   ← 로그인 필수
```

### 3.2 처리 흐름

```
RecommendationController.getRecommendations()
  → CurrentUser.id()  (SecurityContext에서 userId 추출)
  → userProfileRepository.findByUserIdAndDeletedFalse(userId)
      ↳ 없으면 400: "profile setup required"
  → userCategoryRepository.findByUserId(userId)
  → deriveUserCategories(profile, selectedCategories)
      ↳ DB 저장 카테고리 + 프로필 값에서 자동 추론
  → announcementRepository.findByDeletedFalseAndMergedFalse(Pageable.unpaged())
  → announcementCategoryRepository.findByAnnouncementIdIn(ids)
  → announcementEligibilityRepository.findByAnnouncementIdIn(ids)
  → 공고마다 scoreAnnouncement() 실행
      ↳ passesEligibility() 탈락 → null 반환 (제외)
      ↳ matchesBudget() 탈락 → null 반환 (제외)
      ↳ 통과 시 스코어 계산 + 이유 목록 생성
      ↳ score == 0 이면 null 반환 (제외)
  → 점수 내림차순 → applicationEndDate 오름차순 → announcementId 내림차순
  → PageImpl(subList, pageable, total)
```

### 3.3 유저 카테고리 자동 추론 (deriveUserCategories)

DB에 저장된 `user_category` 레코드 외에, 프로필 값에서 카테고리를 자동으로 추가합니다.

| 조건 | 추가되는 카테고리 |
|------|-----------------|
| 나이 19~39세 && 미혼 | YOUTH |
| 기혼 && 혼인 7년 이내 | NEWLYWED |
| homeless = true | HOMELESS |
| elderly = true || 나이 65세 이상 | ELDERLY |
| lowIncome = true || recipient = true || nearPoverty = true | LOW_INCOME |
| childrenCount >= 3 | MULTI_CHILD |

### 3.4 스코어링 구조

#### 사전 필터 (0점 처리 전 제거)

```
passesEligibility → false  →  공고 제외 (null 반환)
matchesBudget     → false  →  공고 제외 (null 반환)
```

**자격조건 필터 (passesEligibility)**

| 조건 필드 | 판단 방식 |
|----------|----------|
| `ageMin` | `profile.age < ageMin` → 탈락 |
| `ageMax` | `profile.age > ageMax` → 탈락 |
| `homelessRequired` | `true && !profile.homeless` → 탈락 |
| `lowIncomeRequired` | `true && !(lowIncome || recipient || nearPoverty)` → 탈락 |
| `elderlyRequired` | `true && age < elderlyAgeMin(기본 65)` → 탈락 |
| `childrenMinCount` | `profile.childrenCount < childrenMinCount` → 탈락 |
| `maritalTargetType` | 아래 혼인 조건 참고 |

**혼인 조건 판단 (passesMaritalCondition)**

| MaritalTargetType | 통과 조건 |
|-------------------|----------|
| ANY / null | 항상 통과 |
| SINGLE | maritalStatus == SINGLE |
| MARRIED | maritalStatus == MARRIED |
| ENGAGED | maritalStatus != MARRIED |
| NEWLYWED | MARRIED && (marriageYears <= marriageYearLimit) |

**예산 필터 (matchesBudget)**

- `maxDeposit` 설정 && 공고 depositAmount가 maxDeposit 초과 → 탈락
- `maxMonthlyRent` 설정 && 공고 monthlyRentAmount가 maxMonthlyRent 초과 → 탈락
- 공고 금액이 null이면 예산 필터 통과 (미확인 공고는 일단 포함)

#### 점수 계산표

| 항목 | 점수 | 조건 |
|------|------|------|
| 카테고리 매칭 | +35 | userCategories ∩ announcementCategories ≠ ∅ |
| 선호 광역시/도 | +20 | preferredRegionLevel1 == 공고 regionLevel1 (ignoreCase) |
| 선호 시/군/구 | +12 | preferredRegionLevel2 == 공고 regionLevel2 (ignoreCase) |
| 선호 주택유형 | +10 | preferredHouseType == 공고 houseTypeNormalized (ignoreCase) |
| 선호 공급유형 | +8 | preferredSupplyType == 공고 supplyTypeNormalized (ignoreCase) |
| 보증금 충족 | +8 | depositAmount <= maxDeposit |
| 월세 충족 | +8 | monthlyRentAmount <= maxMonthlyRent |
| 무주택 조건 충족 | +8 | eligibility.homelessRequired && profile.homeless |
| 저소득 조건 충족 | +10 | eligibility.lowIncomeRequired && (lowIncome\|recipient\|nearPoverty) |
| 자녀 수 조건 충족 | +8 | childrenCount >= eligibility.childrenMinCount |
| 고령자 조건 충족 | +8 | eligibility.elderlyRequired && age >= elderlyAgeMin |
| **최대 합계** | **135** | |

> score == 0 이고 reasons가 비어 있으면 null 반환 → 추천 목록에서 제외

### 3.5 응답 DTO (RecommendationItemResponse)

```java
record RecommendationItemResponse(
    Long announcementId,
    String noticeName,
    String providerName,
    String supplyType,
    String houseType,
    String regionLevel1,
    String regionLevel2,
    String fullAddress,
    String complexName,
    Long depositAmount,
    Long monthlyRentAmount,
    LocalDate applicationStartDate,
    LocalDate applicationEndDate,
    String noticeStatus,
    int matchScore,       // 0 ~ 135
    List<String> matchReasons  // 예: ["선택한 신분 유형과 일치", "희망 지역과 일치"]
)
```

---

## 4. UserProfile 확장 내역

추천 연산을 위해 4월 28일 커밋에서 UserProfile에 아래 필드가 추가됐습니다.

| 추가 필드 | 컬럼명 | 설명 |
|----------|--------|------|
| `marriageYears` | `marriage_years` | 혼인 연차 (NEWLYWED 조건 판단용) |
| `householdSize` | `household_size` | 가구원 수 |
| `recipient` | `is_recipient` | 기초수급자 여부 |
| `nearPoverty` | `is_near_poverty` | 차상위계층 여부 |
| `singleParentFamily` | `is_single_parent_family` | 한부모 가족 여부 |
| `monthlyAverageIncome` | `monthly_average_income` | 월평균 소득 (만원) |
| `totalAssets` | `total_assets` | 총 자산 (만원) |
| `vehicleAssetAmount` | `vehicle_asset_amount` | 차량 자산 (만원) |

`UserProfileService.saveOrUpdate()` 마지막에 `user.markProfileCompleted()` 호출이 추가됐습니다. 프로필 저장 완료 시 유저 상태를 변경합니다.

---

## 5. 코드 리뷰 — 발견된 이슈

### 5.1 [HIGH] 전체 공고 메모리 로드 — 성능 병목

두 서비스 모두 공고 전체를 먼저 메모리에 올린 뒤 Java Stream으로 필터링합니다.

```java
// AnnouncementQueryService.java:62 / RecommendationService.java:63
List<Announcement> announcements = announcementRepository
    .findByDeletedFalseAndMergedFalse(Pageable.unpaged())  // ← 전체 로드
    .getContent();
```

공고가 많아질수록 GC 압박 증가, 응답 지연 발생. 현재 개발 단계에서는 문제없지만 데이터가 수백 건을 넘기 전에 DB 레벨 필터링으로 전환 필요.

**권장 방향**: `AnnouncementQueryService`는 `JpaSpecificationExecutor` 또는 QueryDSL로 전환. `RecommendationService`는 카테고리 JOIN 1차 필터링 후 Java 레이어에서 스코어링.

### 5.2 [HIGH] Integer 언박싱 NPE 위험 (RecommendationService)

`UserProfile.age`와 `childrenCount`는 DB `NOT NULL`이지만 Java 타입이 `Integer`(Wrapper)입니다. 아래 두 곳에서 null 체크 없이 비교합니다.

```java
// RecommendationService.java:203 — null 체크 없음
if (profile.isElderly() || profile.getAge() >= 65)  // NPE 가능

// RecommendationService.java:260 — null 체크 없음  
if (eligibility.getChildrenMinCount() != null && profile.getChildrenCount() < eligibility.getChildrenMinCount())  // NPE 가능

// 수정 방법
if (profile.isElderly() || (profile.getAge() != null && profile.getAge() >= 65))
if (eligibility.getChildrenMinCount() != null && profile.getChildrenCount() != null
        && profile.getChildrenCount() < eligibility.getChildrenMinCount())
```

### 5.3 [HIGH] page size 상한 미설정

```java
// AnnouncementController.java / RecommendationController.java
@RequestParam(defaultValue = "20") int size
```

`?size=100000` 요청 시 직렬화 비용 폭발. 상한 추가 필요.

```java
int safeSize = Math.min(size, 100);
return announcementQueryService.getAnnouncements(..., PageRequest.of(page, safeSize));
```

### 5.4 [HIGH] `(int) pageable.getOffset()` 타입 캐스팅

```java
// AnnouncementQueryService.java:84 / RecommendationService.java:84
int start = (int) pageable.getOffset();  // long → int 강제 캐스팅, 오버플로우 시 음수
```

`getOffset()`은 `long` 반환. `page * size`가 `Integer.MAX_VALUE` 초과 시 `subList(음수, end)` → `IndexOutOfBoundsException`.

```java
long offsetLong = pageable.getOffset();
if (offsetLong >= filtered.size()) {
    return new PageImpl<>(List.of(), pageable, filtered.size());
}
int start = (int) offsetLong;
```

### 5.5 [MEDIUM] Stream 내부에서 예외 throw (status 유효성 검사)

```java
// AnnouncementQueryService.java:223 — Stream filter 람다 내에서 예외
private boolean matchesStatus(Announcement a, String status) {
    try {
        return a.getNoticeStatus() == AnnouncementStatus.valueOf(status.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "invalid status: " + status);  // Stream 중단
    }
}
```

유효성 검사는 스트림 진입 전에 처리해야 합니다.

### 5.6 [MEDIUM] null 금액 공고가 예산 필터를 통과

```java
// AnnouncementQueryService.java:246
return maxDeposit == null || depositAmount == null || depositAmount <= maxDeposit;
//                           ↑ depositAmount가 null이면 maxDeposit 있어도 통과
```

비즈니스 정책 결정 필요: "금액 미확인 공고는 포함 vs 제외". 현재는 포함.

### 5.7 [MEDIUM] PENDING 자격조건 공고가 추천 필터에 그대로 적용

AI 파싱 결과가 검수 전(PENDING) 상태여도 자격조건 필터링에 사용됩니다. 파싱 오류로 잘못된 나이 범위가 저장된 경우 실제 대상자가 제외될 수 있습니다.

```java
// 권장 처리
private boolean passesEligibility(UserProfile profile, AnnouncementEligibility eligibility) {
    if (eligibility == null) return true;
    if (eligibility.getReviewStatus() == ParseReviewStatus.PENDING) return true;  // PENDING은 필터 스킵
    ...
}
```

### 5.8 [MEDIUM] ENGAGED 혼인 조건 의미 불명확

```java
// RecommendationService.java:275
case ENGAGED -> profile.getMaritalStatus() != MaritalStatus.MARRIED;
```

"약혼"을 "미혼 또는 약혼 상태"(MARRIED 제외 전체)로 정의한 것인지, 실제 비즈니스 정책 확인이 필요합니다.

### 5.9 [MEDIUM] regionLevel2 옵션이 level1과 독립적으로 반환

`GET /api/filters/regions/level2`는 전체 시/군/구 목록을 반환합니다. "서울특별시"를 선택해도 "해운대구" 같은 다른 지역 구가 함께 표시됩니다. level1 파라미터를 받는 필터링 버전이 필요합니다.

### 5.10 [MEDIUM] 테스트 코드 없음

`src/test/java` 디렉토리에 테스트 파일이 없습니다. 스코어링 로직과 자격조건 필터는 비즈니스 핵심이므로 단위 테스트가 필요합니다.

```java
// 최소 필요 테스트 예시
@Test
void 청년_프로필_청년_카테고리_공고_35점() { ... }

@Test
void 나이_조건_미달_공고_추천_제외() { ... }

@Test
void 보증금_초과_공고_예산_필터_탈락() { ... }

@Test
void keyword_필터_공고명_매칭() { ... }
```

---

## 6. 개선 우선순위 요약

| 우선순위 | 이슈 | 영향 |
|----------|------|------|
| 1 | `Pageable.unpaged()` → DB 레벨 필터링 전환 | 스케일링 가능성 |
| 2 | Integer 언박싱 NPE 방어 코드 | 런타임 오류 |
| 3 | page size 상한 설정 | 잠재적 DoS |
| 4 | `(int) getOffset()` 캐스팅 방어 | 엣지 케이스 오류 |
| 5 | PENDING 자격조건 처리 정책 명시 | 추천 정확도 |
| 6 | 테스트 코드 작성 | 리그레션 방지 |
| 7 | regionLevel2 필터 옵션을 level1 파라미터 기반으로 | UX 개선 |
