package com.foodstore.menu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class ProductForm {
    private long id;

    @NotBlank(message = "Tên không được để trống")
    private String name;

    @NotNull(message = "Id danh mục không được để trống")
    private long categoryId;

    @NotNull(message = "Số tiền không được để trống")
    @PositiveOrZero(message = "Số tiền không được âm")
    private BigDecimal price;

    @NotBlank(message = "Mô tả không được để trống")
    private String type;

    private String description;

    private List<Long> componentIds = new ArrayList<>();

    private List<Integer> quantities = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Long> getComponentIds() {
        return componentIds;
    }

    public void setComponentIds(List<Long> componentIds) {
        this.componentIds = componentIds;
    }

    public List<Integer> getQuantities() {
        return quantities;
    }

    public void setQuantities(List<Integer> quantities) {
        this.quantities = quantities;
    }


}
