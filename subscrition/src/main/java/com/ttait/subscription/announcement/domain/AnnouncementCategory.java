package com.ttait.subscription.announcement.domain;

import com.ttait.subscription.user.domain.enums.CategoryCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "announcement_category",
        uniqueConstraints = @UniqueConstraint(columnNames = {"announcement_id", "category_code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK (자동 증가)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement; // 연관 공고

    @Enumerated(EnumType.STRING)
    @Column(name = "category_code", nullable = false, length = 20)
    private CategoryCode categoryCode; // 수혜 대상 카테고리 (청년, 신혼부부 등)

    @Enumerated(EnumType.STRING)
    @Column(name = "match_source", nullable = false, length = 10)
    private MatchSource matchSource; // 카테고리 매칭 방식 (규칙/AI/수동)

    @Column(name = "match_reason", columnDefinition = "TEXT")
    private String matchReason; // 카테고리 매칭 사유

    @Column(name = "score")
    private Integer score; // 매칭 점수

    @Builder
    public AnnouncementCategory(Announcement announcement, CategoryCode categoryCode,
                                MatchSource matchSource, String matchReason, Integer score) {
        this.announcement = announcement;
        this.categoryCode = categoryCode;
        this.matchSource = matchSource;
        this.matchReason = matchReason;
        this.score = score;
    }
}
