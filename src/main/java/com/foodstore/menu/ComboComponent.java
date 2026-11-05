package com.foodstore.menu;

public class ComboComponent {
    private Product product;
    private  int quantity;

    public ComboComponent(){}

    public ComboComponent(Product product, int quantity) {
        setProduct(product);
        setQuantity(quantity);
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        if(product == null) {
            throw new IllegalArgumentException("Sản phẩm không được NULL!");
        }

        // if(product instanceof Combo) {
        //     throw new IllegalArgumentException("Không được thêm combo vào combo khác!");
        // }

        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if(quantity < 1) {
            throw new IllegalArgumentException("số lượng thành phần phải lớn hơn hoặc bằng 1");
        }
        this.quantity = quantity;
    }
}
