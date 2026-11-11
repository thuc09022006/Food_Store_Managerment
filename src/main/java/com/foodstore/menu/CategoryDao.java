package com.foodstore.menu;

import java.util.List;
import java.util.Optional;

public interface CategoryDao {

    List<Category> findAll();

    Optional<Category> findById(long id);

    long save(Category category);
    
    void update(Category category);

    void delete(long id);

    //Từ bảng danh mục tra bảng products có sản phẩm có category_id = ? hay không
    boolean existsProductIn(long categoryId);

    //xem danh mục đó đã tồn tại chưa
    boolean existsByName(String name);
}
