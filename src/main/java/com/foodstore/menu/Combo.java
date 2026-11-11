package com.foodstore.menu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Combo extends Product {

    private List<ComboComponent> components = new ArrayList<>();

    public Combo() {
        super();
    }

    public Combo(Long id, Long categoryId, String name,
                 BigDecimal price, String description, boolean active) {
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
        if (components == null) {
            throw new IllegalArgumentException("Danh sách thành phần combo không được null");
        }

        List<ComboComponent> newComponents = new ArrayList<>();

        for (ComboComponent component : components) {
            if (component == null || component.getProduct() == null) {
                throw new IllegalArgumentException("Thành phần combo không hợp lệ");
            }

            if (component.getProduct() instanceof Combo) {
                throw new IllegalArgumentException("Không được đặt combo bên trong combo khác");
            }

            boolean duplicated = newComponents.stream()
                    .anyMatch(existing ->
                            existing.getProduct().getId()
                                    == component.getProduct().getId());

            if (duplicated) {
                throw new IllegalArgumentException("Không được trùng món trong combo");
            }

            newComponents.add(component);
        }

        this.components = newComponents;
    }

    public void addComponent(ComboComponent component) {
        if (component == null || component.getProduct() == null) {
            throw new IllegalArgumentException("Thành phần combo không hợp lệ");
        }

        if (component.getProduct() instanceof Combo) {
            throw new IllegalArgumentException("Không được đặt combo bên trong combo khác");
        }

        boolean duplicated = components.stream()
                .anyMatch(existing ->
                        existing.getProduct().getId()
                                == component.getProduct().getId());

        if (duplicated) {
            throw new IllegalArgumentException("Món đã tồn tại trong combo");
        }

        components.add(component);
    }

    public BigDecimal getComponentsTotal() {
        BigDecimal total = BigDecimal.ZERO;

        for (ComboComponent component : components) {
            BigDecimal componentPrice = component.getProduct().getPrice();

            BigDecimal lineTotal = componentPrice.multiply(
                    BigDecimal.valueOf(component.getQuantity())
            );

            total = total.add(lineTotal);
        }

        return total;
    }

    public BigDecimal getSavings() {
        
        if(getPrice() == null) {
            throw new IllegalArgumentException("Giá combo chưa được thiết lập!");
        }

        return getComponentsTotal().subtract(getPrice());
    }
}