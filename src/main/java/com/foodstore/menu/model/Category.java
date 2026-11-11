package com.foodstore.menu.model;

public class Category {
    private Long id;
    private String name;

    public Category(){}

    public Category(Long id, String name) {
        this.id = id;
        setName(name);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if(name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên không được trống");
        }
        this.name = name.trim();
    }
}


