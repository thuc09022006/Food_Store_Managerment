package com.foodstore.menu;

import java.util.List;

public interface ProductService {

    Product getById(long id);

    List<Product> findActive();

    List<ComboComponent> getComponents(long comboId);
}
