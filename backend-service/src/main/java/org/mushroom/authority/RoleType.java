package org.mushroom.authority;


public enum RoleType {
    USER,
    ADMIN,
    SERVICE;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
