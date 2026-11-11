package com.foodstore.menu.service;

import java.util.List;

import com.foodstore.menu.model.Category;

public interface CategoryService{

    List<Category> findAll();

    long create(String name);

    void rename(long id, String name);

    void delete(long id);
    
}