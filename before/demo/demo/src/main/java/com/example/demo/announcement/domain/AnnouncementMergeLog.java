package com.example.demo.announcement.domain;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "announcement_merge_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementMergeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merged_group_key", nullable = false, length = 200)
    private String mergedGroupKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_announcement_id", nullable = false)
    private Announcement targetAnnouncement;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Column(name = "source_notice_id", nullable = false, length = 100)
    private String sourceNoticeId;

    @Column(name = "match_key", nullable = false, length = 200)
    private String matchKey;

    @Column(name = "merge_reason", nullable = false, length = 255)
    private String mergeReason;

    @Column(name = "merged_at", nullable = false)
    private LocalDateTime mergedAt;

    @Builder
    public AnnouncementMergeLog(String mergedGroupKey, Announcement targetAnnouncement, SourceType sourceType,
                                String sourceNoticeId, String matchKey, String mergeReason,
                                LocalDateTime mergedAt) {
        this.mergedGroupKey = mergedGroupKey;
        this.targetAnnouncement = targetAnnouncement;
        this.sourceType = sourceType;
        this.sourceNoticeId = sourceNoticeId;
        this.matchKey = matchKey;
        this.mergeReason = mergeReason;
        this.mergedAt = mergedAt;
    }
}
