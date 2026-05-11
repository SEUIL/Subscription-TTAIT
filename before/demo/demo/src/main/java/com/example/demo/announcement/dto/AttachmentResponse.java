package com.example.demo.announcement.dto;

public record AttachmentResponse(
        String attachmentType,
        String attachmentName,
        String attachmentUrl
) {
}
