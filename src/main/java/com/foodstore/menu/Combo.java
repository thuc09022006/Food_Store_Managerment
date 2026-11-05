package com.foodstore.menu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Combo extends Product {

    private List<ComboComponent> components = new ArrayList<>();


    public Combo(){}


    public Combo(long id, long categoryId, String name, BigDecimal price, String description, boolean active) {
        super(id, categoryId, name, price, description, active);
    }


    @Override 
    public String getType() {
        return "COMBO";
    }

    
    public List<ComboComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }


    public void setComponents(List<ComboComponent> components) {

        if(components == null) {
            throw new IllegalArgumentException("Danh sách thành phần không được trống!");
        }

        List<ComboComponent> copy = new ArrayList<>();

        for(ComboComponent component : components) {

            if(component == null) {
                throw new IllegalArgumentException("Thành phần Combo không được trống!");
            }
            copy.add(new ComboComponent(component.getProduct(), component.getQuantity()));
        }

        this.components = copy;
    }


    public void addComponent(ComboComponent component) {

        if(component == null) {
            throw new IllegalArgumentException("Thành phần không được trống!");
        }
        components.add(new ComboComponent(component.getProduct(), component.getQuantity()));
    }


    public void removeComponent(Product product) {
        if(product == null) {
            return;
        }

        components.removeIf(component -> 
            component.getProduct().getId() == product.getId()
        );
    }


    public BigDecimal getComponentsTotal() {
        return components.stream()
                .map(component -> component.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(component.getQuantity()))
                ).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getSavings() {
        if(getPrice() == null) {
            throw new IllegalArgumentException("Giá combo chưa được thiết lập!");
        }

        return getComponentsTotal().subtract(getPrice());
    }
}
