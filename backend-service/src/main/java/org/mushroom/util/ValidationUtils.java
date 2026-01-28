package org.mushroom.util;

import org.mushroom.exception.BadRequestException;

public final class ValidationUtils {

    public static String validateAndSanitizeTitle(String title) {
        if (title == null) {
            return null;
        }
        if (title.isEmpty()) {
            throw new BadRequestException(" must not be blank");
        }
        return title.trim();
    }

    public static  String validateAndSanitizeDescription(String description){
        if (description == null) {
            return null;
        }
        if (description.isBlank()) {
            throw new BadRequestException("description must not be blank");
        }
        return description.trim();
    }
}
