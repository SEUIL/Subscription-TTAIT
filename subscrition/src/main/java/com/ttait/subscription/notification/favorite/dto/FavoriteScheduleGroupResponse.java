package com.ttait.subscription.notification.favorite.dto;

import java.util.List;

public record FavoriteScheduleGroupResponse(
    String key,
    String label,
    List<FavoriteScheduleItemResponse> items
) {
}
