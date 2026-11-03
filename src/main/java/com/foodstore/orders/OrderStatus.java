package com.foodstore.orders;

/** Vòng đời đơn: NEW -> PREPARING -> SERVED -> PAID; CANCELLED khi chưa PAID. */
public enum OrderStatus {
    NEW, PREPARING, SERVED, PAID, CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case NEW -> next == PREPARING || next == CANCELLED;
            case PREPARING -> next == SERVED || next == CANCELLED;
            case SERVED -> next == PAID || next == CANCELLED;
            case PAID, CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == PAID || this == CANCELLED;
    }
}
