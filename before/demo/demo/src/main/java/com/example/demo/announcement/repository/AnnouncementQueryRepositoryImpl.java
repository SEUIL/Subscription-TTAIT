package com.example.demo.announcement.repository;

import com.example.demo.announcement.domain.AnnouncementStatus;
import com.example.demo.common.exception.ApiException;
import com.example.demo.announcement.repository.query.AnnouncementDetailRow;
import com.example.demo.announcement.repository.query.AnnouncementListRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class AnnouncementQueryRepositoryImpl implements AnnouncementQueryRepository {

    private final EntityManager entityManager;

    public AnnouncementQueryRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<AnnouncementListRow> search(Long userId, String regionLevel1, String regionLevel2,
                                            String supplyType, String provider, String status,
                                            String keyword, String sort, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        String whereClause = buildWhereClause(regionLevel1, regionLevel2, supplyType, provider, status, keyword, params);
        String scoreExpr = buildRecommendScoreExpression(userId != null);
        String select = """
                select new com.example.demo.announcement.repository.query.AnnouncementListRow(
                    a.id,
                    a.noticeName,
                    a.providerName,
                    a.supplyTypeNormalized,
                    a.houseTypeNormalized,
                    a.regionLevel1,
                    a.regionLevel2,
                    a.complexName,
                    a.depositAmount,
                    a.monthlyRentAmount,
                    a.applicationStartDate,
                    a.applicationEndDate,
                    a.noticeStatus,
                    %s,
                    %s
                )
                from Announcement a
                where a.deleted = false and a.merged = false
                """.formatted(buildSavedExpression(userId != null), scoreExpr) + whereClause + buildOrderBy(sort, userId != null);

        TypedQuery<AnnouncementListRow> query = entityManager.createQuery(select, AnnouncementListRow.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(
                "select count(a.id) from Announcement a where a.deleted = false and a.merged = false " + whereClause,
                Long.class
        );

        if (userId != null) {
            query.setParameter("userId", userId);
        }
        applyParams(query, params);
        applyParams(countQuery, params);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return new PageImpl<>(query.getResultList(), pageable, countQuery.getSingleResult());
    }

    @Override
    public Optional<AnnouncementDetailRow> findDetail(Long announcementId) {
        TypedQuery<AnnouncementDetailRow> query = entityManager.createQuery("""
                select new com.example.demo.announcement.repository.query.AnnouncementDetailRow(
                    a.id,
                    a.noticeName,
                    a.providerName,
                    a.noticeStatus,
                    a.announcementDate,
                    a.applicationStartDate,
                    a.applicationEndDate,
                    a.winnerAnnouncementDate,
                    a.supplyTypeNormalized,
                    a.houseTypeNormalized,
                    a.complexName,
                    a.fullAddress,
                    a.depositAmount,
                    a.monthlyRentAmount,
                    d.householdCount,
                    a.supplyHouseholdCount,
                    d.heatingType,
                    d.exclusiveAreaText,
                    d.exclusiveAreaValue,
                    d.moveInExpectedYm,
                    d.applicationDatetimeText,
                    d.guideText,
                    d.contactPhone,
                    a.sourceNoticeUrl
                )
                from Announcement a
                left join AnnouncementDetail d on d.announcement.id = a.id and d.deleted = false
                where a.deleted = false and a.merged = false and a.id = :announcementId
                """, AnnouncementDetailRow.class);
        query.setParameter("announcementId", announcementId);
        return query.getResultStream().findFirst();
    }

    private String buildWhereClause(String regionLevel1, String regionLevel2, String supplyType,
                                    String provider, String status, String keyword, Map<String, Object> params) {
        StringBuilder where = new StringBuilder();
        if (StringUtils.hasText(regionLevel1)) {
            where.append(" and a.regionLevel1 = :regionLevel1");
            params.put("regionLevel1", regionLevel1);
        }
        if (StringUtils.hasText(regionLevel2)) {
            where.append(" and a.regionLevel2 = :regionLevel2");
            params.put("regionLevel2", regionLevel2);
        }
        if (StringUtils.hasText(supplyType)) {
            where.append(" and a.supplyTypeNormalized = :supplyType");
            params.put("supplyType", supplyType);
        }
        if (StringUtils.hasText(provider)) {
            where.append(" and a.providerName = :provider");
            params.put("provider", provider);
        }
        if (StringUtils.hasText(status)) {
            where.append(" and a.noticeStatus = :status");
            params.put("status", parseStatus(status));
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" and (lower(a.noticeName) like :keyword or lower(a.complexName) like :keyword)");
            params.put("keyword", "%" + keyword.toLowerCase() + "%");
        }
        return where.toString();
    }

    private String buildSavedExpression(boolean hasUser) {
        if (!hasUser) {
            return "false";
        }
        return "case when exists (select 1 from UserSavedAnnouncement usa where usa.user.id = :userId and usa.announcement.id = a.id) then true else false end";
    }

    private String buildRecommendScoreExpression(boolean hasUser) {
        if (!hasUser) {
            return "case when a.noticeStatus = 'OPEN' then 20 when a.noticeStatus = 'SCHEDULED' then 5 else 0 end";
        }
        return """
                (
                    coalesce((select max(tag.tagScore) from AnnouncementCategoryTag tag
                        where tag.announcement.id = a.id
                          and tag.categoryCode in (select uc.categoryCode from UserCategory uc where uc.user.id = :userId)
                    ), 0)
                    + case when exists (
                        select 1 from UserProfile up
                        where up.user.id = :userId and up.deleted = false and up.preferredRegionLevel1 = a.regionLevel1
                    ) then 15 else 0 end
                    + case when exists (
                        select 1 from UserProfile up
                        where up.user.id = :userId and up.deleted = false and up.preferredRegionLevel2 = a.regionLevel2
                    ) then 20 else 0 end
                    + case when exists (
                        select 1 from UserProfile up
                        where up.user.id = :userId and up.deleted = false and up.preferredSupplyType = a.supplyTypeNormalized
                    ) then 15 else 0 end
                    + case when exists (
                        select 1 from UserProfile up
                        where up.user.id = :userId and up.deleted = false and up.preferredHouseType = a.houseTypeNormalized
                    ) then 10 else 0 end
                    + case when exists (
                        select 1 from UserProfile up
                        where up.user.id = :userId and up.deleted = false
                          and up.maxDeposit is not null and a.depositAmount is not null and a.depositAmount <= up.maxDeposit
                    ) then 10 else 0 end
                    + case when exists (
                        select 1 from UserProfile up
                        where up.user.id = :userId and up.deleted = false
                          and up.maxMonthlyRent is not null and a.monthlyRentAmount is not null and a.monthlyRentAmount <= up.maxMonthlyRent
                    ) then 10 else 0 end
                    + case when a.noticeStatus = 'OPEN' then 20 when a.noticeStatus = 'SCHEDULED' then 5 else 0 end
                )
                """;
    }

    private String buildOrderBy(String sort, boolean hasUser) {
        if ("latest".equalsIgnoreCase(sort)) {
            return " order by a.announcementDate desc nulls last, a.id desc";
        }
        if ("deadline".equalsIgnoreCase(sort)) {
            return " order by a.applicationEndDate asc nulls last, a.id desc";
        }
        if (hasUser) {
            return " order by " + buildRecommendScoreExpression(true) + " desc, a.applicationEndDate asc nulls last, a.id desc";
        }
        return " order by case when a.noticeStatus = 'OPEN' then 0 else 1 end asc, a.applicationEndDate asc nulls last, a.id desc";
    }

    private void applyParams(TypedQuery<?> query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    private AnnouncementStatus parseStatus(String status) {
        try {
            return AnnouncementStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid status: " + status);
        }
    }
}
