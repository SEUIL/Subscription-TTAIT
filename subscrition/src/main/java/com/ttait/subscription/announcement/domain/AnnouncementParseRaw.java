package com.ttait.subscription.announcement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "announcement_parse_raw")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementParseRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK (자동 증가)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement; // 연관 공고

    @Column(name = "raw_type", nullable = false, length = 30)
    private String rawType; // 원본 데이터 유형 (예: LH_ITEM_JSON, PDF_TEXT, AI_PARSE_RESULT)

    @Column(name = "raw_text", nullable = false, columnDefinition = "LONGTEXT")
    private String rawText; // 원본 데이터 전문 (JSON 또는 텍스트)

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt; // 데이터 수집 시각

    @Builder
    public AnnouncementParseRaw(Announcement announcement, String rawType, String rawText, LocalDateTime collectedAt) {
        this.announcement = announcement;
        this.rawType = rawType;
        this.rawText = rawText;
        this.collectedAt = collectedAt;
    }
}
