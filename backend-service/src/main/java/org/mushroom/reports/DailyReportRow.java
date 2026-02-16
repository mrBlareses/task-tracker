package org.mushroom.reports;

import java.util.UUID;

public interface DailyReportRow {
    UUID getUserId();
    String getEmail();
    int getPendingCount();
    int getCompletedCount();
}
