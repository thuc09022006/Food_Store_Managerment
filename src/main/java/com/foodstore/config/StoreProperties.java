package com.foodstore.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Các tham số nghiệp vụ đọc từ application.properties (tiền tố "store").
 * Không hardcode VAT, ngưỡng hạng hay số ngày cảnh báo trong code.
 */
@ConfigurationProperties(prefix = "store")
public record StoreProperties(
        BigDecimal vatRate,          // ví dụ 0.08 = 8%
        int expiryWarningDays,       // cảnh báo lô sắp hết hạn trong N ngày
        Loyalty loyalty) {

    public record Loyalty(
            BigDecimal amountPerPoint,        // bao nhiêu đồng được 1 điểm
            int silverPoints,                 // số điểm tối thiểu để lên hạng SILVER
            int goldPoints,                   // số điểm tối thiểu để lên hạng GOLD
            BigDecimal silverDiscountPercent, // % giảm cho hạng SILVER
            BigDecimal goldDiscountPercent) { // % giảm cho hạng GOLD
    }
}
