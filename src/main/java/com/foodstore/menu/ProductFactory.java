package com.foodstore.menu;

import java.math.BigDecimal;
import java.util.Locale;

public final class ProductFactory {
    
    private ProductFactory(){}

    public static Product create(String type, long id, long categoryId, String name, BigDecimal price, String description, boolean active) {
        if(type == null || type.trim().isEmpty()){
            throw new IllegalArgumentException("Loại sản phẩm không được để trống!");
        }

        String normallizedType = type.trim().toUpperCase(Locale.ROOT);

        switch (normallizedType) {
            case "FOOD":
                return new Food(id, categoryId, name, price, description, active);
            
            case "DRINK" :
                return new Drink(id, categoryId, name, price, description, active);

            case "COMBO" : 
                return new Combo(id, categoryId, name, price, description, active);

            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type );
        }
    }

    
}
