package org.mushroom.reports;

import java.util.UUID;

public interface UserDailyReportProjection {
    UUID getUserId();
    String getEmail();
    int getPendingCount();
    int getCompletedCount();
}
