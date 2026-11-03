package com.foodstore.menu;

import java.math.BigDecimal;

/** Món ăn. */
public class Food extends Product {

    public Food(long id, long categoryId, String name, BigDecimal price, String description, boolean active) {
        super(id, categoryId, name, price, description, active);
    }

    @Override
    public ProductType getType() {
        return ProductType.FOOD;
    }
}
