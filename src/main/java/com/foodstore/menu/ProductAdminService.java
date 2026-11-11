package com.foodstore.menu;

import java.util.List;

public interface ProductAdminService {

    List<Product> search(String name, Long categoryId, Boolean active);

    long create(Product product, List<ComboComponent> components);

    void update(Product product, List<ComboComponent> components);

    void setActive(long id, boolean active);

    void delete(long id);
    
}
