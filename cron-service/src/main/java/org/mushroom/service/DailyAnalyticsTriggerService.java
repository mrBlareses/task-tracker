package org.mushroom.service;

import org.mushroom.entity.enums.TriggerType;

public interface DailyAnalyticsTriggerService {
    void trigger(TriggerType triggerType) throws InterruptedException;
}
