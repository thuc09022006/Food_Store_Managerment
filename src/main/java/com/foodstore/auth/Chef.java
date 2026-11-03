package com.foodstore.auth;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Đầu bếp: màn hình bếp, xem tồn kho. */
public class Chef extends Staff {

    private static final Set<Screen> SCREENS = Collections.unmodifiableSet(EnumSet.of(Screen.KITCHEN, Screen.INVENTORY));

    public Chef(long id, String username, String fullName) {
        super(id, username, fullName);
    }

    @Override
    public StaffRole getRole() {
        return StaffRole.CHEF;
    }

    @Override
    public Set<Screen> allowedScreens() {
        return SCREENS;
    }
}
