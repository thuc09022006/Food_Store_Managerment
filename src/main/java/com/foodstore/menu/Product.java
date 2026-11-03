package com.foodstore.menu;

import java.math.BigDecimal;

/**
 * Lớp cha trừu tượng của mọi thứ có thể bán. Đặt tên Product (không phải MenuItem)
 * vì java.awt.MenuItem sẽ gây lỗi "reference is ambiguous" trong các file Swing.
 * Giá là BigDecimal (VND, không có phần thập phân), không dùng double.
 */
public abstract class Product {

    private long id;
    private long categoryId;
    private String name;
    private BigDecimal price;
    private String description;
    private boolean active;

    protected Product(long id, long categoryId, String name, BigDecimal price, String description, boolean active) {
        this.id = id;
        this.categoryId = categoryId;
        setName(name);
        setPrice(price);
        this.description = description;
        this.active = active;
    }

    public abstract ProductType getType();

    public long getId() {
        return id;
    }

    /** DAO gọi sau khi INSERT để gán id sinh ra. */
    void setId(long id) {
        this.id = id;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public final void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên món không được để trống");
        }
        this.name = name.trim();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public final void setPrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Giá phải >= 0");
        }
        this.price = price;
    }

    /** Có thể null (cột description cho phép NULL). */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
