package com.example.demo.announcement.domain;

import com.example.demo.common.entity.BaseTimeEntity;
import com.example.demo.user.domain.CategoryCode;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "announcement_category_tag")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementCategoryTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_code", nullable = false, length = 30)
    private CategoryCode categoryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_source", nullable = false, length = 30)
    private TagSource tagSource;

    @Column(name = "tag_score", nullable = false)
    private Integer tagScore;

    @Builder
    public AnnouncementCategoryTag(Announcement announcement, CategoryCode categoryCode, TagSource tagSource,
                                   Integer tagScore) {
        this.announcement = announcement;
        this.categoryCode = categoryCode;
        this.tagSource = tagSource;
        this.tagScore = tagScore;
    }
}
