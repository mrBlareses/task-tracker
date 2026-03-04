package org.mushroom.enums;

public enum RoleType {
    USER,
    ADMIN,
    SERVICE;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
