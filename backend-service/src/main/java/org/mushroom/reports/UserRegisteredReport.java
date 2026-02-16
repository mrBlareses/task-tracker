package org.mushroom.reports;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserRegisteredReport(UUID userId, String email, String firstname, String lastname,
                                   OffsetDateTime registeredAt) {
}