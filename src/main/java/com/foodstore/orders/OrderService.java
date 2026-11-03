package com.foodstore.orders;

import java.math.BigDecimal;
import java.util.List;

/** CONTRACT chốt ở Phase 0. M2 cài đặt trong OrderServiceImpl; M3 và M4 gọi qua interface này. */
public interface OrderService {

    /** @throws java.util.NoSuchElementException nếu không có đơn */
    Order getById(long id);

    List<OrderItem> getItems(long orderId);

    /** Tổng tiền các dòng món, dùng unitPrice đã chụp. */
    BigDecimal calcSubtotal(long orderId);

    /** SERVED -> PAID. Ném IllegalStateException nếu đơn không ở trạng thái SERVED. */
    void markPaid(long orderId);
}
