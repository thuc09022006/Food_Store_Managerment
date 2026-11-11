package com.foodstore.menu.model;

public class ComboComponent {

    private Product product;
    private int quantity;

    public ComboComponent() {
    }

    public ComboComponent(Product product, int quantity) {
        setProduct(product);
        setQuantity(quantity);
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Món trong combo không được để trống");
        }

        if (product instanceof Combo) {
            throw new IllegalArgumentException("Không được đặt combo bên trong combo khác");
        }

        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Số lượng thành phần phải từ 1 trở lên");
        }

        this.quantity = quantity;
    }
}