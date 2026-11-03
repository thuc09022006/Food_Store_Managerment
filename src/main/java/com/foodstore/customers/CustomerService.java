package com.foodstore.customers;

import java.math.BigDecimal;
import java.util.Optional;

/** CONTRACT chốt ở Phase 0. M4 cài đặt trong CustomerServiceImpl. */
public interface CustomerService {

    Optional<Customer> findById(long id);

    /** Cộng điểm theo giá trị đơn (store.loyalty.amount-per-point) và cập nhật hạng. */
    void addPoints(long customerId, BigDecimal orderTotal);
}
