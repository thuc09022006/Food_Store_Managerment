package com.foodstore.menu;

/** Một thành phần của combo: sản phẩm + số lượng. */
public record ComboComponent(Product product, int quantity) {

    public ComboComponent {
        if (product == null) {
            throw new IllegalArgumentException("Thành phần combo không được null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng thành phần phải > 0");
        }
    }
}
