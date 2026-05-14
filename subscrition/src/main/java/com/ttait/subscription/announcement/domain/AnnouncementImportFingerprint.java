package com.ttait.subscription.announcement.domain;

import com.ttait.subscription.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "announcement_import_fingerprint",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_announcement_import_fingerprint_announcement",
                columnNames = "announcement_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementImportFingerprint extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false, unique = true)
    private Announcement announcement;

    @Column(name = "lh_item_hash", length = 128)
    private String lhItemHash;

    @Column(name = "lh_detail_hash", length = 128)
    private String lhDetailHash;

    @Column(name = "pdf_url", length = 1000)
    private String pdfUrl;

    @Column(name = "pdf_content_hash", length = 128)
    private String pdfContentHash;

    @Column(name = "pdf_ai_json_hash", length = 128)
    private String pdfAiJsonHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "parse_status", nullable = false, length = 20, columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'PENDING'")
    private AnnouncementImportParseStatus parseStatus;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "last_parsed_at")
    private LocalDateTime lastParsedAt;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Builder
    public AnnouncementImportFingerprint(Announcement announcement,
                                         String lhItemHash,
                                         String lhDetailHash,
                                         String pdfUrl,
                                         String pdfContentHash,
                                         String pdfAiJsonHash,
                                         AnnouncementImportParseStatus parseStatus,
                                         LocalDateTime lastCheckedAt,
                                         LocalDateTime lastParsedAt,
                                         String lastErrorMessage) {
        this.announcement = announcement;
        this.lhItemHash = lhItemHash;
        this.lhDetailHash = lhDetailHash;
        this.pdfUrl = pdfUrl;
        this.pdfContentHash = pdfContentHash;
        this.pdfAiJsonHash = pdfAiJsonHash;
        this.parseStatus = parseStatus == null ? AnnouncementImportParseStatus.PENDING : parseStatus;
        this.lastCheckedAt = lastCheckedAt;
        this.lastParsedAt = lastParsedAt;
        this.lastErrorMessage = lastErrorMessage;
    }

    public void updateImportFingerprint(String lhItemHash,
                                        String lhDetailHash,
                                        String pdfUrl,
                                        String pdfContentHash,
                                        String pdfAiJsonHash,
                                        AnnouncementImportParseStatus parseStatus,
                                        LocalDateTime lastCheckedAt,
                                        LocalDateTime lastParsedAt,
                                        String lastErrorMessage) {
        this.lhItemHash = lhItemHash;
        this.lhDetailHash = lhDetailHash;
        this.pdfUrl = pdfUrl;
        this.pdfContentHash = pdfContentHash;
        this.pdfAiJsonHash = pdfAiJsonHash;
        this.parseStatus = parseStatus == null ? AnnouncementImportParseStatus.PENDING : parseStatus;
        this.lastCheckedAt = lastCheckedAt;
        if (lastParsedAt != null) {
            this.lastParsedAt = lastParsedAt;
        }
        this.lastErrorMessage = lastErrorMessage;
    }

    public void markChecked(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }
}
