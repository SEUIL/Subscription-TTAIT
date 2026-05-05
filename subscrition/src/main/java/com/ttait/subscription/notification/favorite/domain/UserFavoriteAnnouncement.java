package com.ttait.subscription.notification.favorite.domain;

import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(
    name = "user_favorite_announcement",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_ufa_user_announcement",
        columnNames = {"user_id", "announcement_id"}
    ),
    indexes = {
        @Index(name = "idx_ufa_user", columnList = "user_id"),
        @Index(name = "idx_ufa_announcement", columnList = "announcement_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFavoriteAnnouncement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Builder
    public UserFavoriteAnnouncement(Long userId, Announcement announcement) {
        this.userId = userId;
        this.announcement = announcement;
    }
}
