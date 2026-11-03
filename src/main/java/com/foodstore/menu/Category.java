package com.foodstore.menu;

public record Category(long id, String name) {

    public Category {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống");
        }
        name = name.trim();
    }
}
