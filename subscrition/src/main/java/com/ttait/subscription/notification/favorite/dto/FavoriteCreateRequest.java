package com.ttait.subscription.notification.favorite.dto;

import jakarta.validation.constraints.NotNull;

public record FavoriteCreateRequest(
    @NotNull Long announcementId
) {}
