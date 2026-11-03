package com.foodstore.menu;

import java.util.List;

/**
 * CONTRACT chốt ở Phase 0 (chỉ thêm phương thức mới, không đổi/xóa phương thức cũ).
 * M1 cài đặt trong ProductServiceImpl; M2 và M3 gọi qua interface này.
 */
public interface ProductService {

    /** @throws java.util.NoSuchElementException nếu không có sản phẩm */
    Product getById(long id);

    /** Chỉ các sản phẩm đang bán (is_active = TRUE). */
    List<Product> findActive();

    /** Thành phần của một combo; danh sách rỗng nếu không phải combo. */
    List<ComboComponent> getComponents(long comboId);
}
