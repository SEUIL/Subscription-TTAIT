package com.example.demo.announcement.domain;

import com.example.demo.common.entity.BaseTimeEntity;
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
@Table(name = "announcement_source_map")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementSourceMap extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Column(name = "source_notice_id", nullable = false, length = 100)
    private String sourceNoticeId;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_payload_id")
    private RawPayload rawPayload;

    @Builder
    public AnnouncementSourceMap(Announcement announcement, SourceType sourceType, String sourceNoticeId,
                                 String sourceUrl, RawPayload rawPayload) {
        this.announcement = announcement;
        this.sourceType = sourceType;
        this.sourceNoticeId = sourceNoticeId;
        this.sourceUrl = sourceUrl;
        this.rawPayload = rawPayload;
    }
}
