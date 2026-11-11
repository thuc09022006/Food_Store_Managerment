package com.foodstore.menu.service;

import java.util.List;

import com.foodstore.menu.model.ComboComponent;
import com.foodstore.menu.model.Product;

public interface ProductService {

    Product getById(long id);

    List<Product> findActive();

    List<ComboComponent> getComponents(long comboId);
}
