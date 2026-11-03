package com.foodstore.auth;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Thu ngân: xem đơn, thanh toán, khách hàng/voucher. */
public class Cashier extends Staff {

    private static final Set<Screen> SCREENS = Collections.unmodifiableSet(EnumSet.of(Screen.ORDERS, Screen.PAYMENTS, Screen.CUSTOMERS));

    public Cashier(long id, String username, String fullName) {
        super(id, username, fullName);
    }

    @Override
    public StaffRole getRole() {
        return StaffRole.CASHIER;
    }

    @Override
    public Set<Screen> allowedScreens() {
        return SCREENS;
    }
}
