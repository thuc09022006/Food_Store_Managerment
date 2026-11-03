package com.foodstore.menu;

import java.util.List;
import java.util.Optional;

public interface CategoryDao {

    List<Category> findAll();

    Optional<Category> findById(long id);

    /** @return bản ghi đã có id sinh ra */
    Category save(Category category);

    void update(Category category);

    /** Ném DataIntegrityViolationException nếu còn sản phẩm thuộc danh mục này. */
    void delete(long id);
}
