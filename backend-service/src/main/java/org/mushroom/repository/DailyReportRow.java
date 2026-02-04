package org.mushroom.repository;

import java.util.UUID;

public interface DailyReportRow {
    UUID getUserId();
    String getEmail();
    long getPendingCount();
    long getCompletedCount();
}
