package com.foodstore.Orders_Tables;


public enum OrderStatus {
    NEW, PREPARING, SERVED, PAID, CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        if(next == null) {
            return false;
        }

        switch(this) {
            case NEW:
                return next == PREPARING || next == CANCELLED;

            case PREPARING:
                return next == SERVED || next == CANCELLED;

            case SERVED:
                return next == PAID;

            case PAID:
                return false;

            case CANCELLED:
                return false;
        default:
            return false;

        }
    }
}

