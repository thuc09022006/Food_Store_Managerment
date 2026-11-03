package com.foodstore.orders;

/** CONTRACT chốt ở Phase 0. M2 cài đặt trong TableServiceImpl (và thêm CRUD bàn); M4 gọi release. */
public interface TableService {

    /** Trả bàn về FREE sau khi thanh toán. */
    void release(long tableId);
}
