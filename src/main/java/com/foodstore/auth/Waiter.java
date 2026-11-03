package com.foodstore.auth;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Phục vụ: bàn, đơn hàng, xem thực đơn. */
public class Waiter extends Staff {

    private static final Set<Screen> SCREENS = Collections.unmodifiableSet(EnumSet.of(Screen.MENU, Screen.TABLES, Screen.ORDERS));

    public Waiter(long id, String username, String fullName) {
        super(id, username, fullName);
    }

    @Override
    public StaffRole getRole() {
        return StaffRole.WAITER;
    }

    @Override
    public Set<Screen> allowedScreens() {
        return SCREENS;
    }
}
