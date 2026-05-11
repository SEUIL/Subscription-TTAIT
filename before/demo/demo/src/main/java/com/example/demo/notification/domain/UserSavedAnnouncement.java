package com.example.demo.notification.domain;

import com.example.demo.announcement.domain.Announcement;
import com.example.demo.common.entity.BaseTimeEntity;
import com.example.demo.user.domain.User;
import jakarta.persistence.Entity;
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
@Table(name = "user_saved_announcement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSavedAnnouncement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Builder
    public UserSavedAnnouncement(User user, Announcement announcement) {
        this.user = user;
        this.announcement = announcement;
    }
}
