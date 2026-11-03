package com.foodstore.inventory;

/** CONTRACT chốt ở Phase 0. M3 cài đặt trong InventoryServiceImpl; M4 (CheckoutService) gọi. */
public interface InventoryService {

    /**
     * Trừ kho theo công thức cho mọi dòng món của đơn (combo được mở rộng thành các thành phần).
     * Chính sách thiếu nguyên liệu: xem Open Question 3 trong SPEC.md.
     */
    void deductForOrder(long orderId);
}
