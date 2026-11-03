package com.foodstore.customers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CustomerTierTest {

    @Test
    void tierFollowsConfiguredThresholds() {
        assertEquals(CustomerTier.NORMAL, CustomerTier.forPoints(0, 500, 2000));
        assertEquals(CustomerTier.NORMAL, CustomerTier.forPoints(499, 500, 2000));
        assertEquals(CustomerTier.SILVER, CustomerTier.forPoints(500, 500, 2000));
        assertEquals(CustomerTier.SILVER, CustomerTier.forPoints(1999, 500, 2000));
        assertEquals(CustomerTier.GOLD, CustomerTier.forPoints(2000, 500, 2000));
    }
}
