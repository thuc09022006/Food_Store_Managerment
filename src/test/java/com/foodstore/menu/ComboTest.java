package com.foodstore.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ComboTest {

    private Food food(long id, String name, long price) {
        return new Food(id, 1, name, BigDecimal.valueOf(price), null, true);
    }

    @Test
    void polymorphicTypeAndNullDescription() {
        Product p = food(1, "Phở bò", 55000);
        Product d = new Drink(2, 3, "Trà đá", BigDecimal.valueOf(5000), null, true);
        assertEquals(ProductType.FOOD, p.getType());
        assertEquals(ProductType.DRINK, d.getType());
    }

    @Test
    void comboComputesSavingsAgainstBuyingSeparately() {
        Combo combo = new Combo(8, 4, "Combo", BigDecimal.valueOf(65000), null, true);
        combo.addComponent(new ComboComponent(food(2, "Cơm gà", 50000), 1));
        combo.addComponent(new ComboComponent(new Drink(6, 3, "Cà phê sữa", BigDecimal.valueOf(25000), null, true), 1));

        assertEquals(0, BigDecimal.valueOf(75000).compareTo(combo.getComponentsTotal()));
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(combo.getSavings()));
        assertEquals(ProductType.COMBO, combo.getType());
    }

    @Test
    void comboCannotContainAnotherCombo() {
        Combo outer = new Combo(8, 4, "Outer", BigDecimal.valueOf(1), null, true);
        Combo inner = new Combo(9, 4, "Inner", BigDecimal.valueOf(1), null, true);
        assertThrows(IllegalArgumentException.class, () -> outer.addComponent(new ComboComponent(inner, 1)));
    }

    @Test
    void invalidProductDataIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> food(1, " ", 1000));
        assertThrows(IllegalArgumentException.class, () -> food(1, "Phở", -1));
        assertThrows(IllegalArgumentException.class, () -> new ComboComponent(food(1, "Phở", 1000), 0));
    }
}
