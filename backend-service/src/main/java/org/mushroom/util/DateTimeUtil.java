package org.mushroom.util;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class DateTimeUtil {
    private final Clock clock;

    public Instant now(){
        return clock.instant();
    }

    public OffsetDateTime nowOffsetDateTime() {
        return OffsetDateTime.now(clock);
    }
}
