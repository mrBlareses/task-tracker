package org.mushroom.model;

import lombok.Builder;

@Builder
public record UserDailyReportEvent(
        String userId,
        String email,
        int pendingCount,
        int completedCount
) {
}