package com.foodstore.auth;

/** Vai trò, khớp với ENUM cột staff.role trong schema.sql. */
public enum StaffRole {
    ADMIN("Quản trị viên"),
    CASHIER("Thu ngân"),
    WAITER("Phục vụ"),
    CHEF("Đầu bếp");

    private final String label;

    StaffRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
