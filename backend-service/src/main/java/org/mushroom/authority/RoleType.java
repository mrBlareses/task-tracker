package org.mushroom.authority;


public enum RoleType {
    USER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
