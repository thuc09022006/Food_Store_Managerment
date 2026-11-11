package com.foodstore.menu.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.foodstore.menu.model.ComboComponent;
import com.foodstore.menu.model.Product;

public interface ProductDao {

    List<Product> findAll();

    Optional<Product> findById(long id);

    List<Product> search(String name, Long categoryId, Boolean active);

    long save(Product product);

    void update(Product product);

    void setActive(long id, boolean active);

    void delete(long id);

    List<ComboComponent> findComponents(long comboId);

    Map<Long, List<ComboComponent>> findComponentsByComboIds(Collection<Long> comboIds);

    void replaceComponents(long comboId, List<ComboComponent> components);

    boolean existsInOrders(long productId);

    boolean existsAsComponent(long productId);

    boolean existsInRecipes(long productId);

    List<Product> findActiveCombosContaining(long productId);
    
}
