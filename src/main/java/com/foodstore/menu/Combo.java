package com.foodstore.menu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Combo (Composite): chứa nhiều Product, có giá riêng. Thành phần không được là combo khác
 * để tránh đệ quy khi mở rộng công thức trừ kho.
 */
public class Combo extends Product {

    private final List<ComboComponent> components = new ArrayList<>();

    public Combo(long id, long categoryId, String name, BigDecimal price, String description, boolean active) {
        super(id, categoryId, name, price, description, active);
    }

    @Override
    public ProductType getType() {
        return ProductType.COMBO;
    }

    public void addComponent(ComboComponent component) {
        if (component.product().getType() == ProductType.COMBO) {
            throw new IllegalArgumentException("Combo không được chứa combo khác");
        }
        components.add(component);
    }

    public List<ComboComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }

    /** Tổng giá nếu mua lẻ từng thành phần. */
    public BigDecimal getComponentsTotal() {
        return components.stream()
                .map(c -> c.product().getPrice().multiply(BigDecimal.valueOf(c.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Số tiền khách tiết kiệm được so với mua lẻ (có thể âm nếu combo đặt giá cao hơn). */
    public BigDecimal getSavings() {
        return getComponentsTotal().subtract(getPrice());
    }
}
