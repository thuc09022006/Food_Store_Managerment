package com.foodstore.menu;

import java.math.BigDecimal;

public class Drink extends Product {

    public Drink() {
        super();
    }

    public Drink(Long id, Long categoryId, String name,
                 BigDecimal price, String description, boolean active) {
        super(id, categoryId, name, price, description, active);
    }

    @Override
    public String getType() {
        return "DRINK";
    }
}