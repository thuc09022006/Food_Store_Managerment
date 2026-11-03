package com.foodstore.customers;

public enum CustomerTier {
    NORMAL, SILVER, GOLD;

    /** Hạng theo điểm; ngưỡng lấy từ cấu hình (store.loyalty.*), không hardcode. */
    public static CustomerTier forPoints(int points, int silverPoints, int goldPoints) {
        if (points >= goldPoints) {
            return GOLD;
        }
        if (points >= silverPoints) {
            return SILVER;
        }
        return NORMAL;
    }
}
