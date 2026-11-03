package com.foodstore.auth;

/** Các màn hình của ứng dụng. Thứ tự khai báo là thứ tự hiện trên menu. */
public enum Screen {
    MENU("Thực đơn"),
    TABLES("Bàn"),
    ORDERS("Đơn hàng"),
    KITCHEN("Bếp"),
    INVENTORY("Kho"),
    PAYMENTS("Thanh toán"),
    CUSTOMERS("Khách hàng & Voucher"),
    STAFF("Nhân viên"),
    REPORTS("Báo cáo");

    private final String label;

    Screen(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
