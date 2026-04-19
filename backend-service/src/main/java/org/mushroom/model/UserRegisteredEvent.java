package org.mushroom.model;

import lombok.Builder;

@Builder
public record UserRegisteredEvent(
        String id,
        String email,
        String firstName,
        String lastName
) {
}