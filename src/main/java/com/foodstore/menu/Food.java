package com.foodstore.menu;

import java.math.BigDecimal;

public class Food extends Product {

    public Food() {
        super();
    }

    public Food(Long id, Long categoryId, String name,
                BigDecimal price, String description, boolean active) {
        super(id, categoryId, name, price, description, active);
    }

    @Override
    public String getType() {
        return "FOOD";
    }
}