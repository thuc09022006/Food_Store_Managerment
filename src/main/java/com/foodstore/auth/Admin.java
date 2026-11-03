package com.foodstore.auth;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Quản trị: được vào mọi màn hình. */
public class Admin extends Staff {

    private static final Set<Screen> SCREENS = Collections.unmodifiableSet(EnumSet.allOf(Screen.class));

    public Admin(long id, String username, String fullName) {
        super(id, username, fullName);
    }

    @Override
    public StaffRole getRole() {
        return StaffRole.ADMIN;
    }

    @Override
    public Set<Screen> allowedScreens() {
        return SCREENS;
    }
}
