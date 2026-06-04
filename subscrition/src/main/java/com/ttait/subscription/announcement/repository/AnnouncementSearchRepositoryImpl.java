package com.ttait.subscription.announcement.repository;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementCategory;
import com.ttait.subscription.announcement.domain.AnnouncementEligibility;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

public class AnnouncementSearchRepositoryImpl implements AnnouncementSearchRepository {

    private static final char LIKE_ESCAPE = '\\';

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Announcement> searchPublicVisible(AnnouncementSearchCondition condition, Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Announcement> contentQuery = criteriaBuilder.createQuery(Announcement.class);
        Root<Announcement> contentRoot = contentQuery.from(Announcement.class);

        contentQuery.select(contentRoot)
                .where(buildPredicates(criteriaBuilder, contentQuery, contentRoot, condition))
                .orderBy(resolveOrder(criteriaBuilder, contentRoot, pageable));

        TypedQuery<Announcement> query = entityManager.createQuery(contentQuery);
        if (pageable.isPaged()) {
            query.setFirstResult(Math.toIntExact(pageable.getOffset()));
            query.setMaxResults(pageable.getPageSize());
        }

        List<Announcement> content = query.getResultList();
        long total = countPublicVisible(condition);
        return new PageImpl<>(content, pageable, total);
    }

    private long countPublicVisible(AnnouncementSearchCondition condition) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<Announcement> countRoot = countQuery.from(Announcement.class);

        countQuery.select(criteriaBuilder.countDistinct(countRoot))
                .where(buildPredicates(criteriaBuilder, countQuery, countRoot, condition));

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private Predicate[] buildPredicates(CriteriaBuilder criteriaBuilder,
                                        CriteriaQuery<?> query,
                                        Root<Announcement> root,
                                        AnnouncementSearchCondition condition) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
        predicates.add(criteriaBuilder.isFalse(root.get("merged")));
        predicates.add(publicVisiblePredicate(criteriaBuilder, query, root, condition));

        if (StringUtils.hasText(condition.regionLevel1())) {
            predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("regionLevel1")),
                    condition.regionLevel1().toLowerCase(Locale.ROOT)));
        }
        if (StringUtils.hasText(condition.regionLevel2())) {
            String regionLevel2 = condition.regionLevel2().toLowerCase(Locale.ROOT);
            String escapedRegionLevel2 = escapeLike(regionLevel2);
            Expression<String> fullAddress = criteriaBuilder.lower(root.get("fullAddress"));
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.equal(criteriaBuilder.lower(root.get("regionLevel2")), regionLevel2),
                    like(criteriaBuilder, fullAddress, escapedRegionLevel2 + " %"),
                    like(criteriaBuilder, fullAddress, "% " + escapedRegionLevel2),
                    like(criteriaBuilder, fullAddress, "% " + escapedRegionLevel2 + " %")
            ));
        }
        if (StringUtils.hasText(condition.supplyType())) {
            predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("supplyTypeNormalized")),
                    condition.supplyType().toLowerCase(Locale.ROOT)));
        }
        if (StringUtils.hasText(condition.houseType())) {
            predicates.add(houseTypePredicate(criteriaBuilder, root, condition.houseType()));
        }
        if (StringUtils.hasText(condition.provider())) {
            predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("providerName")),
                    condition.provider().toLowerCase(Locale.ROOT)));
        }
        if (condition.status() != null) {
            predicates.add(criteriaBuilder.equal(root.get("noticeStatus"), condition.status()));
        }
        if (StringUtils.hasText(condition.keyword())) {
            String keyword = likePattern(condition.keyword());
            predicates.add(criteriaBuilder.or(
                    like(criteriaBuilder, criteriaBuilder.lower(root.get("noticeName")), keyword),
                    like(criteriaBuilder, criteriaBuilder.lower(root.get("providerName")), keyword),
                    like(criteriaBuilder, criteriaBuilder.lower(root.get("complexName")), keyword),
                    like(criteriaBuilder, criteriaBuilder.lower(root.get("fullAddress")), keyword)
            ));
        }
        if (condition.minDeposit() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("depositAmount"), condition.minDeposit()));
        }
        if (condition.maxDeposit() != null) {
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("depositAmount")),
                    criteriaBuilder.lessThanOrEqualTo(root.get("depositAmount"), condition.maxDeposit())
            ));
        }
        if (condition.minMonthlyRent() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("monthlyRentAmount"), condition.minMonthlyRent()));
        }
        if (condition.maxMonthlyRent() != null) {
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("monthlyRentAmount")),
                    criteriaBuilder.lessThanOrEqualTo(root.get("monthlyRentAmount"), condition.maxMonthlyRent())
            ));
        }
        if (condition.categories() != null && !condition.categories().isEmpty()) {
            predicates.add(categoryPredicate(criteriaBuilder, query, root, condition.categories()));
        }

        return predicates.toArray(Predicate[]::new);
    }

    private Predicate houseTypePredicate(CriteriaBuilder criteriaBuilder, Root<Announcement> root, String houseType) {
        String normalized = houseType.toLowerCase(Locale.ROOT);
        Expression<String> normalizedColumn = criteriaBuilder.lower(root.get("houseTypeNormalized"));
        Expression<String> rawColumn = criteriaBuilder.lower(root.get("houseTypeRaw"));

        return switch (houseType) {
            case "아파트" -> criteriaBuilder.or(
                    criteriaBuilder.equal(normalizedColumn, normalized),
                    like(criteriaBuilder, rawColumn, likePattern("아파트"))
            );
            case "다가구" -> criteriaBuilder.or(
                    criteriaBuilder.equal(normalizedColumn, normalized),
                    like(criteriaBuilder, rawColumn, likePattern("다가구"))
            );
            case "다세대/연립" -> criteriaBuilder.or(
                    criteriaBuilder.equal(normalizedColumn, normalized),
                    like(criteriaBuilder, rawColumn, likePattern("다세대")),
                    like(criteriaBuilder, rawColumn, likePattern("연립")),
                    like(criteriaBuilder, rawColumn, likePattern("빌라"))
            );
            case "오피스텔" -> criteriaBuilder.or(
                    criteriaBuilder.equal(normalizedColumn, normalized),
                    like(criteriaBuilder, rawColumn, likePattern("오피스텔"))
            );
            case "기타" -> criteriaBuilder.or(
                    criteriaBuilder.equal(normalizedColumn, normalized),
                    criteriaBuilder.and(
                            criteriaBuilder.isNotNull(rawColumn),
                            criteriaBuilder.notLike(rawColumn, likePattern("아파트"), LIKE_ESCAPE),
                            criteriaBuilder.notLike(rawColumn, likePattern("다가구"), LIKE_ESCAPE),
                            criteriaBuilder.notLike(rawColumn, likePattern("다세대"), LIKE_ESCAPE),
                            criteriaBuilder.notLike(rawColumn, likePattern("연립"), LIKE_ESCAPE),
                            criteriaBuilder.notLike(rawColumn, likePattern("빌라"), LIKE_ESCAPE),
                            criteriaBuilder.notLike(rawColumn, likePattern("오피스텔"), LIKE_ESCAPE)
                    )
            );
            default -> criteriaBuilder.or(
                    criteriaBuilder.equal(normalizedColumn, normalized),
                    criteriaBuilder.equal(rawColumn, normalized)
            );
        };
    }

    private Predicate publicVisiblePredicate(CriteriaBuilder criteriaBuilder,
                                             CriteriaQuery<?> query,
                                             Root<Announcement> root,
                                             AnnouncementSearchCondition condition) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<AnnouncementEligibility> eligibility = subquery.from(AnnouncementEligibility.class);
        subquery.select(eligibility.get("id"))
                .where(
                        criteriaBuilder.equal(eligibility.get("announcement"), root),
                        eligibility.get("reviewStatus").in(condition.reviewStatuses())
                );
        return criteriaBuilder.exists(subquery);
    }

    private Predicate categoryPredicate(CriteriaBuilder criteriaBuilder,
                                        CriteriaQuery<?> query,
                                        Root<Announcement> root,
                                        List<CategoryCode> categories) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<AnnouncementCategory> category = subquery.from(AnnouncementCategory.class);
        subquery.select(category.get("id"))
                .where(
                        criteriaBuilder.equal(category.get("announcement"), root),
                        category.get("categoryCode").in(categories)
                );

        List<Predicate> fallbackPredicates = new ArrayList<>();
        for (CategoryCode categoryCode : categories) {
            fallbackPredicates.add(categoryKeywordPredicate(criteriaBuilder, root, categoryCode));
        }

        Subquery<Long> anyCategorySubquery = query.subquery(Long.class);
        Root<AnnouncementCategory> anyCategory = anyCategorySubquery.from(AnnouncementCategory.class);
        anyCategorySubquery.select(anyCategory.get("id"))
                .where(criteriaBuilder.equal(anyCategory.get("announcement"), root));

        return criteriaBuilder.or(
                criteriaBuilder.exists(subquery),
                criteriaBuilder.and(
                        criteriaBuilder.not(criteriaBuilder.exists(anyCategorySubquery)),
                        criteriaBuilder.or(fallbackPredicates.toArray(Predicate[]::new))
                )
        );
    }

    private Predicate categoryKeywordPredicate(CriteriaBuilder criteriaBuilder,
                                               Root<Announcement> root,
                                               CategoryCode categoryCode) {
        Expression<String> searchText = searchableText(criteriaBuilder, root);
        return switch (categoryCode) {
            case YOUTH -> containsAny(criteriaBuilder, searchText, "청년", "대학생", "청년매입");
            case NEWLYWED -> containsAny(criteriaBuilder, searchText, "신혼", "예비신혼", "혼인", "부부");
            case HOMELESS -> containsAny(criteriaBuilder, searchText, "무주택");
            case ELDERLY -> containsAny(criteriaBuilder, searchText, "고령자", "만 65세", "노인");
            case LOW_INCOME -> containsAny(criteriaBuilder, searchText, "저소득", "기초급여", "차상위");
            case MULTI_CHILD -> containsAny(criteriaBuilder, searchText, "다자녀");
        };
    }

    private Expression<String> searchableText(CriteriaBuilder criteriaBuilder, Root<Announcement> root) {
        Expression<String> text = criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("noticeName"), ""));
        text = criteriaBuilder.concat(text, " ");
        text = criteriaBuilder.concat(text, criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("supplyTypeRaw"), "")));
        text = criteriaBuilder.concat(text, " ");
        text = criteriaBuilder.concat(text, criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("supplyTypeNormalized"), "")));
        text = criteriaBuilder.concat(text, " ");
        text = criteriaBuilder.concat(text, criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("houseTypeRaw"), "")));
        text = criteriaBuilder.concat(text, " ");
        text = criteriaBuilder.concat(text, criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("houseTypeNormalized"), "")));
        text = criteriaBuilder.concat(text, " ");
        text = criteriaBuilder.concat(text, criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("providerName"), "")));
        text = criteriaBuilder.concat(text, " ");
        return criteriaBuilder.concat(text, criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("fullAddress"), "")));
    }

    private Predicate containsAny(CriteriaBuilder criteriaBuilder, Expression<String> text, String... keywords) {
        List<Predicate> predicates = new ArrayList<>();
        for (String keyword : keywords) {
            predicates.add(like(criteriaBuilder, text, likePattern(keyword)));
        }
        return criteriaBuilder.or(predicates.toArray(Predicate[]::new));
    }

    private List<Order> resolveOrder(CriteriaBuilder criteriaBuilder, Root<Announcement> root, Pageable pageable) {
        Sort.Order primaryOrder = pageable.getSort().stream().findFirst().orElse(null);
        if (primaryOrder == null) {
            return orderByApplicationEndDateNullsLast(criteriaBuilder, root);
        }

        return switch (primaryOrder.getProperty()) {
            case "announcementDate" -> orderByAnnouncementDateDescNullsLast(criteriaBuilder, root);
            case "depositAmount" -> orderByDepositAmountNullsLast(criteriaBuilder, root, primaryOrder.getDirection());
            default -> orderByApplicationEndDateNullsLast(criteriaBuilder, root);
        };
    }

    private List<Order> orderByAnnouncementDateDescNullsLast(CriteriaBuilder criteriaBuilder, Root<Announcement> root) {
        return List.of(
                nullsLast(criteriaBuilder, root, "announcementDate"),
                criteriaBuilder.desc(root.get("announcementDate")),
                criteriaBuilder.desc(root.get("id"))
        );
    }

    private List<Order> orderByDepositAmountNullsLast(CriteriaBuilder criteriaBuilder,
                                                      Root<Announcement> root,
                                                      Sort.Direction direction) {
        Order amountOrder = direction.isAscending()
                ? criteriaBuilder.asc(root.get("depositAmount"))
                : criteriaBuilder.desc(root.get("depositAmount"));
        return List.of(
                nullsLast(criteriaBuilder, root, "depositAmount"),
                amountOrder,
                nullsLast(criteriaBuilder, root, "applicationEndDate"),
                criteriaBuilder.asc(root.get("applicationEndDate")),
                criteriaBuilder.desc(root.get("id"))
        );
    }

    private List<Order> orderByApplicationEndDateNullsLast(CriteriaBuilder criteriaBuilder, Root<Announcement> root) {
        return List.of(
                nullsLast(criteriaBuilder, root, "applicationEndDate"),
                criteriaBuilder.asc(root.get("applicationEndDate")),
                criteriaBuilder.desc(root.get("id"))
        );
    }

    private Order nullsLast(CriteriaBuilder criteriaBuilder, Root<Announcement> root, String property) {
        return criteriaBuilder.asc(criteriaBuilder.selectCase()
                .when(criteriaBuilder.isNull(root.get(property)), 1)
                .otherwise(0));
    }

    private String likePattern(String value) {
        return "%" + escapeLike(value.toLowerCase(Locale.ROOT)) + "%";
    }

    private Predicate like(CriteriaBuilder criteriaBuilder, Expression<String> expression, String pattern) {
        return criteriaBuilder.like(expression, pattern, LIKE_ESCAPE);
    }

    private String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
