package com.example.demo.announcement.domain;

import com.example.demo.common.entity.SoftDeleteBaseEntity;
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
@Table(name = "announcement_attachment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementAttachment extends SoftDeleteBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 50)
    private AttachmentType attachmentType;

    @Column(name = "attachment_name", nullable = false, length = 255)
    private String attachmentName;

    @Column(name = "attachment_url", nullable = false, length = 500)
    private String attachmentUrl;

    @Builder
    public AnnouncementAttachment(Announcement announcement, AttachmentType attachmentType, String attachmentName,
                                  String attachmentUrl) {
        this.announcement = announcement;
        this.attachmentType = attachmentType;
        this.attachmentName = attachmentName;
        this.attachmentUrl = attachmentUrl;
    }
}
