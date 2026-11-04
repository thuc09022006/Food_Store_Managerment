package com.foodstore.menu;

import java.math.BigDecimal;

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
        this.name = name;
        this.price = price;
        this.description = description;
        this.active =active;
    }

    protected Product(long categoryId, String name, BigDecimal price, String decription) {
        this(0, categoryId, name, price, decription, true);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    
}
