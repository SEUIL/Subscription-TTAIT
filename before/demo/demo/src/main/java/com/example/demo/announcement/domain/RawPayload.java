package com.example.demo.announcement.domain;

import com.example.demo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "raw_payload")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawPayload extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Column(name = "api_name", nullable = false, length = 100)
    private String apiName;

    @Column(name = "source_notice_id", length = 100)
    private String sourceNoticeId;

    @Column(name = "request_key", length = 200)
    private String requestKey;

    @Column(name = "payload_format", nullable = false, length = 20)
    private String payloadFormat;

    @Column(name = "payload_text", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadText;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Builder
    public RawPayload(SourceType sourceType, String apiName, String sourceNoticeId, String requestKey,
                      String payloadFormat, String payloadText, LocalDateTime collectedAt) {
        this.sourceType = sourceType;
        this.apiName = apiName;
        this.sourceNoticeId = sourceNoticeId;
        this.requestKey = requestKey;
        this.payloadFormat = payloadFormat;
        this.payloadText = payloadText;
        this.collectedAt = collectedAt;
    }
}
