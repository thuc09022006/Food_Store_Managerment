package com.foodstore.orders;

import java.math.BigDecimal;

/** Một dòng món trong đơn. unitPrice là giá CHỤP tại thời điểm gọi món. */
public record OrderItem(long id, long orderId, long productId, int quantity, BigDecimal unitPrice, String note) {

    public OrderItem {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải > 0");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Đơn giá phải >= 0");
        }
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
