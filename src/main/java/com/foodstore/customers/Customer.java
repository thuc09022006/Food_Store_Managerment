package com.foodstore.customers;

public record Customer(long id, String fullName, String phone, int points, CustomerTier tier) {

    public Customer {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên khách không được để trống");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống");
        }
        if (points < 0) {
            throw new IllegalArgumentException("Điểm không được âm");
        }
    }
}
