package com.foodstore.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class StaffTest {

    @Test
    void adminCanAccessEveryScreen() {
        Staff admin = new Admin(1, "admin", "Quản Lý");
        assertEquals(EnumSet.allOf(Screen.class), admin.allowedScreens());
        assertEquals(StaffRole.ADMIN, admin.getRole());
    }

    @Test
    void cashierSeesOrdersPaymentsCustomersOnly() {
        Staff cashier = new Cashier(2, "cashier", "Thu Ngân");
        assertEquals(EnumSet.of(Screen.ORDERS, Screen.PAYMENTS, Screen.CUSTOMERS), cashier.allowedScreens());
        assertFalse(cashier.canAccess(Screen.STAFF));
        assertFalse(cashier.canAccess(Screen.REPORTS));
    }

    @Test
    void waiterSeesMenuTablesOrdersOnly() {
        Staff waiter = new Waiter(3, "waiter", "Phục Vụ");
        assertEquals(EnumSet.of(Screen.MENU, Screen.TABLES, Screen.ORDERS), waiter.allowedScreens());
        assertFalse(waiter.canAccess(Screen.PAYMENTS));
    }

    @Test
    void chefSeesKitchenAndInventoryOnly() {
        Staff chef = new Chef(4, "chef", "Đầu Bếp");
        assertEquals(EnumSet.of(Screen.KITCHEN, Screen.INVENTORY), chef.allowedScreens());
        assertTrue(chef.canAccess(Screen.KITCHEN));
        assertFalse(chef.canAccess(Screen.ORDERS));
    }

    @Test
    void allowedScreensIsNotModifiable() {
        Staff waiter = new Waiter(3, "waiter", "Phục Vụ");
        assertThrows(UnsupportedOperationException.class, () -> waiter.allowedScreens().add(Screen.REPORTS));
    }

    @Test
    void blankUsernameOrNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Admin(1, " ", "Quản Lý"));
        assertThrows(IllegalArgumentException.class, () -> new Admin(1, "admin", ""));
    }
}
