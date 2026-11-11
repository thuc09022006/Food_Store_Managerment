package com.foodstore.menu;

import java.util.List;

public interface CategoryService{

    List<Category> findAll();

    long create(String name);

    void rename(long id, String name);

    void delete(long id);
    
}