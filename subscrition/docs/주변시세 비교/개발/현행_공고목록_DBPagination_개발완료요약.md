# [현행] 공고 목록 DB Pagination 개발 완료 요약

<!-- markdownlint-disable MD013 -->

## 1. 문서 목적

이 문서는 `perf/public-list-pagination` branch에서 진행한 public 공고 목록 조회 성능 개선 작업의 개발 완료 내용을 정리한다.

대상은 `GET /api/announcements` main list path다. 기존에는 public visible 공고를 전체 조회한 뒤 Java Stream으로 필터링, 정렬, `subList` paging을 수행했으나, 이번 작업으로 DB query 기반 filtering/pagination으로 전환했다.

## 2. 완료 범위

| 영역 | 완료 내용 |
| - | - |
| 대상 API | `GET /api/announcements` public 목록 조회 |
| 조회 방식 | 전체 로드 후 메모리 필터링에서 DB filtering/pagination으로 전환 |
| public visible 조건 | `APPROVED`, `CORRECTED`, `deleted=false`, `merged=false` 유지 |
| 기본 필터 | 지역, 공급유형, 주택유형, 공급기관, 상태, 키워드, 보증금/월세 범위 DB 조건화 |
| category 필터 | 저장 category 우선, 저장 category 없는 공고만 키워드 fallback 적용 |
| 정렬 | `applicationEndDate` 오름차순, null last, `id` 내림차순 유지 |
| count | 별도 count query로 `Page.totalElements` 계산 |
| edge case | `houseTypeRaw` 정규화 의미, `regionLevel2` token 매칭, SQL LIKE wildcard escape 반영 |
| 문서 | 성능 리스크 문서와 기존 공고 필터링 분석 문서에 개선 반영 |

## 3. 주요 구현 파일

| 구분 | 파일 |
| - | - |
| service | `subscrition/src/main/java/com/ttait/subscription/announcement/service/AnnouncementQueryService.java` |
| repository interface | `subscrition/src/main/java/com/ttait/subscription/announcement/repository/AnnouncementRepository.java` |
| search condition | `subscrition/src/main/java/com/ttait/subscription/announcement/repository/AnnouncementSearchCondition.java` |
| custom repository | `subscrition/src/main/java/com/ttait/subscription/announcement/repository/AnnouncementSearchRepository.java` |
| custom repository impl | `subscrition/src/main/java/com/ttait/subscription/announcement/repository/AnnouncementSearchRepositoryImpl.java` |

## 4. 주요 테스트 파일

| 검증 대상 | 파일 |
| - | - |
| DB filtering/pagination | `subscrition/src/test/java/com/ttait/subscription/announcement/repository/AnnouncementSearchRepositoryTest.java` |
| category fallback | `subscrition/src/test/java/com/ttait/subscription/announcement/repository/AnnouncementSearchRepositoryTest.java` |
| region/houseType/LIKE edge case | `subscrition/src/test/java/com/ttait/subscription/announcement/repository/AnnouncementSearchRepositoryTest.java` |

## 5. 검증 결과

| 검증 | 결과 |
| - | - |
| `AnnouncementSearchRepositoryTest` | 통과 |
| `./gradlew test --no-daemon --max-workers=1` | 통과 |
| `./gradlew build --no-daemon --max-workers=1` | 통과 |
| 수정 문서 `markdownlint` | 통과 |
| 목표 검증 리뷰 | PASS |
| QA 리뷰 | PASS |
| 컨텍스트 재리뷰 | PASS |

LSP diagnostics는 현재 환경에서 반복적으로 중단되었으므로 최종 검증 기준에서는 제외했다. 대신 Gradle `compileJava`, `test`, `build`가 통과한 것을 기준으로 삼았다.

## 6. 리뷰 반영 사항

초기 구현 후 코드 품질 리뷰에서 다음 차이를 지적했고 모두 반영했다.

| 지적 | 반영 |
| - | - |
| `houseTypeRaw` 정규화 의미 차이 | `houseType` filter 입력을 service에서 정규화하고 DB query에서 raw 포함 조건 보정 |
| `regionLevel2` 부분 문자열 과매칭 | `남구`가 `강남구`를 매칭하지 않도록 token 단위 매칭으로 조정 |
| SQL LIKE wildcard 처리 | `%`, `_`, `\` escape 처리 추가 |
| offset int cast | `Math.toIntExact(pageable.getOffset())`로 변경 |

## 7. 제외 범위와 후속 작업

이번 작업은 public 공고 목록 API의 main list path만 대상으로 했다.

| 제외 범위 | 이유 |
| - | - |
| `RecommendationService.getRecommendations()` | 추천 scoring 특성상 별도 query/score 설계 필요 |
| 필터 옵션 일부의 `Pageable.unpaged()` 조회 | 목록 API pagination 병목과 별도 문제 |
| page size 상한 | API 정책 결정이 필요한 별도 hardening 작업 |
| 수동 HTTP 테스트 | 내부 조회 방식 변경이며 DB-backed repository test와 전체 build로 검증 충분 |

## 8. 최종 상태

C 작업은 구현, 테스트, build, 문서 정리까지 완료됐고 `perf/public-list-pagination` branch를 통해 main에 병합됐다.

병합 기준:

- PR: `#1`
- merge commit: `cba951e Merge pull request #1 from SEUIL/perf/public-list-pagination`
- 현재 main에서는 public 공고 목록 조회가 DB filtering/pagination 기반으로 동작한다.
