package com.foodstore.menu.service;

import java.util.List;

import com.foodstore.menu.model.ComboComponent;
import com.foodstore.menu.model.Product;

public interface ProductAdminService {

    List<Product> search(String name, Long categoryId, Boolean active);

    long create(Product product, List<ComboComponent> components);

    void update(Product product, List<ComboComponent> components);

    void setActive(long id, boolean active);

    void delete(long id);
    
}
