package org.mushroom.util;

import lombok.RequiredArgsConstructor;
import org.mushroom.exception.BadRequestException;

@RequiredArgsConstructor
public class ValidationUtils {
    public static String trimOrBadRequestIfBlank(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return trimmed;
    }
}
