package com.foodstore.menu;

import java.math.BigDecimal;

public abstract class Product {

    private Long id;
    private Long categoryId;
    private String name;
    private BigDecimal price;
    private String description;
    private boolean active;

    public Product() {
    }

    public Product(Long id, Long categoryId, String name,
                   BigDecimal price, String description, boolean active) {
        this.id = id;
        this.categoryId = categoryId;
        setName(name);
        setPrice(price);
        this.description = description;
        this.active = active;
    }

    public abstract String getType();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên món không được để trống");
        }
        this.name = name.trim();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Giá món không được để trống");
        }

        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá món không được âm");
        }

        BigDecimal normalized = price.stripTrailingZeros();

        if (normalized.scale() > 0) {
            throw new IllegalArgumentException("Giá món không được có phần thập phân");
        }

        if (normalized.precision() > 12) {
            throw new IllegalArgumentException("Giá món vượt quá giới hạn 12 chữ số");
        }

        this.price = normalized;
    }

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