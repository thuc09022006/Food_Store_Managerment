package com.foodstore.menu.web;

import jakarta.validation.constraints.NotBlank;

public class CategoryForm {

    private long id;

    @NotBlank(message =  "Tên không được để trống")
    private String name;

    public long getId() {
        return id;
    }

    public void setId() {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
}
