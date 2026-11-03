package com.foodstore.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Đơn hàng: composition với {@link OrderItem}. Đổi trạng thái chỉ qua OrderService (setter package-private). */
public class Order {

    private long id;
    private final Long tableId;      // null nếu mang đi
    private final Long customerId;   // null nếu khách vãng lai
    private final long staffId;
    private final OrderType orderType;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private final String note;
    private final List<OrderItem> items = new ArrayList<>();

    public Order(long id, Long tableId, Long customerId, long staffId, OrderType orderType,
                 OrderStatus status, LocalDateTime createdAt, String note) {
        if (orderType == OrderType.DINE_IN && tableId == null) {
            throw new IllegalArgumentException("Đơn ăn tại chỗ phải gắn với một bàn");
        }
        this.id = id;
        this.tableId = tableId;
        this.customerId = customerId;
        this.staffId = staffId;
        this.orderType = orderType;
        this.status = status;
        this.createdAt = createdAt;
        this.note = note;
    }

    public long getId() { return id; }
    void setId(long id) { this.id = id; }

    public Long getTableId() { return tableId; }
    public Long getCustomerId() { return customerId; }
    public long getStaffId() { return staffId; }
    public OrderType getOrderType() { return orderType; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getNote() { return note; }

    /** Chỉ OrderService (cùng package) được đổi trạng thái, sau khi kiểm canTransitionTo. */
    void setStatus(OrderStatus status) { this.status = status; }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    void addItem(OrderItem item) {
        items.add(item);
    }

    public BigDecimal subtotal() {
        return items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
