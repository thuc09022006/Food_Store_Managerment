-- FoodStore Management System — schema (MySQL 8.0.16+)
-- Chạy một lần trên database trống, tại thư mục gốc của repo. Dùng -e "source ..." (chạy được ở bash, cmd và
-- PowerShell; toán tử "<" KHÔNG chạy trong PowerShell):
--   mysql -u root -p -e "CREATE DATABASE foodstore_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
--   mysql -u root -p --default-character-set=utf8mb4 foodstore_db -e "source src/main/resources/schema.sql"
-- Muốn làm lại từ đầu: DROP DATABASE foodstore_db; rồi tạo lại và chạy lại file này.
-- Spring Boot KHÔNG tự chạy file này với MySQL (spring.sql.init.mode mặc định chỉ chạy với DB nhúng);
-- đừng đặt spring.sql.init.mode=always vì sẽ chạy lại mỗi lần khởi động.
-- Các bảng khai báo theo đúng thứ tự phụ thuộc khóa ngoại. SAU PHASE 0 FILE NÀY ĐƯỢC ĐÓNG BĂNG:
-- muốn sửa phải hỏi nhóm trước.
-- Lưu ý: các ràng buộc CHECK chỉ có hiệu lực từ MySQL 8.0.16; bản cũ hơn bỏ qua chúng một cách im lặng.

SET NAMES utf8mb4;

-- 1. Nhân viên đăng nhập (M5)
CREATE TABLE staff (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,                       -- BCrypt, không bao giờ plaintext
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    role          ENUM('ADMIN','CASHIER','WAITER','CHEF') NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. Thực đơn (M1)
CREATE TABLE categories (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE products (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id  BIGINT       NOT NULL,
    name         VARCHAR(100) NOT NULL,
    product_type ENUM('FOOD','DRINK','COMBO') NOT NULL,
    price        DECIMAL(12,0) NOT NULL CHECK (price >= 0),
    description  VARCHAR(255),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,                -- xóa mềm: món đã từng bán chỉ tắt, không DELETE
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE combo_components (
    combo_id     BIGINT NOT NULL,
    component_id BIGINT NOT NULL,
    quantity     INT    NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (combo_id, component_id),
    FOREIGN KEY (combo_id)     REFERENCES products(id),
    FOREIGN KEY (component_id) REFERENCES products(id),
    CHECK (combo_id <> component_id)                           -- combo không chứa chính nó (combo lồng combo do service chặn)
);

-- 3. Bàn, khách hàng, voucher (M2, M4)
CREATE TABLE dining_tables (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    code   VARCHAR(10) NOT NULL UNIQUE,
    seats  INT         NOT NULL CHECK (seats > 0),
    status ENUM('FREE','OCCUPIED','RESERVED') NOT NULL DEFAULT 'FREE'
);

CREATE TABLE customers (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone     VARCHAR(20)  NOT NULL UNIQUE,
    points    INT          NOT NULL DEFAULT 0 CHECK (points >= 0),
    tier      ENUM('NORMAL','SILVER','GOLD') NOT NULL DEFAULT 'NORMAL'
);

CREATE TABLE vouchers (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(30)  NOT NULL UNIQUE,
    discount_type  ENUM('PERCENT','FIXED') NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL CHECK (discount_value > 0),  -- PERCENT: 10.00 = 10%; FIXED: số đồng
    min_order      DECIMAL(12,0) NOT NULL DEFAULT 0 CHECK (min_order >= 0),
    valid_from     DATE NOT NULL,
    valid_to       DATE NOT NULL,
    usage_limit    INT  NOT NULL CHECK (usage_limit > 0),
    used_count     INT  NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    CHECK (valid_from <= valid_to),
    CHECK (used_count <= usage_limit),                         -- chốt chặn cuối: không bao giờ vượt số lượt dùng
    CHECK (discount_type <> 'PERCENT' OR discount_value <= 100)
);

-- 4. Đơn hàng (M2)
CREATE TABLE orders (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id    BIGINT NULL,                                   -- NULL nếu mang đi/giao hàng; service bắt buộc có khi DINE_IN
    customer_id BIGINT NULL,                                   -- NULL nếu khách vãng lai
    staff_id    BIGINT NOT NULL,                               -- nhân viên tạo đơn (lấy từ người đang đăng nhập)
    order_type  ENUM('DINE_IN','TAKEAWAY','DELIVERY') NOT NULL,   -- DELIVERY: thông tin giao nằm ở bảng deliveries
    status      ENUM('NEW','PREPARING','SERVED','PAID','CANCELLED') NOT NULL DEFAULT 'NEW',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note        VARCHAR(255),
    FOREIGN KEY (table_id)    REFERENCES dining_tables(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (staff_id)    REFERENCES staff(id),
    INDEX idx_orders_status (status),
    INDEX idx_orders_created_at (created_at)
);

CREATE TABLE order_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(12,0) NOT NULL CHECK (unit_price >= 0), -- giá CHỤP tại thời điểm gọi món
    note       VARCHAR(255),
    FOREIGN KEY (order_id)   REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 4b. Giao hàng (M6) — chỉ THÊM bảng mới, không đổi bảng cũ ngoài việc thêm 'DELIVERY' vào orders.order_type
-- Shipper không đăng nhập hệ thống (nhân viên điều phối cập nhật trạng thái), nên tách khỏi bảng staff.
CREATE TABLE shippers (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(20)  NOT NULL UNIQUE,
    vehicle_plate VARCHAR(20),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE                -- xóa mềm: shipper đã từng giao chỉ ngừng hoạt động
);

CREATE TABLE deliveries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT NOT NULL UNIQUE,                    -- mỗi đơn có tối đa một lần giao (giao lại = cùng dòng, attempt_count tăng)
    shipper_id      BIGINT NULL,                               -- NULL khi chưa phân công
    status          ENUM('PENDING','ASSIGNED','DELIVERING','DELIVERED','FAILED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    recipient_name  VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20)  NOT NULL,
    address         VARCHAR(255) NOT NULL,
    delivery_fee    DECIMAL(12,0) NOT NULL DEFAULT 0 CHECK (delivery_fee >= 0),
    attempt_count   INT NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),  -- số lần shipper đã lấy hàng đi giao
    fail_reason     VARCHAR(255) NULL,                         -- lý do thất bại gần nhất
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_at     DATETIME NULL,
    picked_up_at    DATETIME NULL,
    delivered_at    DATETIME NULL,
    FOREIGN KEY (order_id)   REFERENCES orders(id),
    FOREIGN KEY (shipper_id) REFERENCES shippers(id),
    INDEX idx_deliveries_status (status),
    INDEX idx_deliveries_shipper (shipper_id, status)
);

-- 5. Kho và nhà cung cấp (M3)
CREATE TABLE suppliers (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    phone   VARCHAR(20),
    address VARCHAR(255)
);

CREATE TABLE ingredients (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100)  NOT NULL UNIQUE,
    unit           VARCHAR(20)   NOT NULL,                     -- kg, lít, hộp, gói...
    stock_quantity DECIMAL(12,3) NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    min_quantity   DECIMAL(12,3) NOT NULL DEFAULT 0 CHECK (min_quantity >= 0)
);

CREATE TABLE recipes (
    product_id    BIGINT        NOT NULL,
    ingredient_id BIGINT        NOT NULL,
    quantity      DECIMAL(12,3) NOT NULL CHECK (quantity > 0), -- lượng nguyên liệu cho 1 phần món
    PRIMARY KEY (product_id, ingredient_id),
    FOREIGN KEY (product_id)    REFERENCES products(id),
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
);

CREATE TABLE stock_imports (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id BIGINT   NOT NULL,
    staff_id    BIGINT   NOT NULL,                             -- nhân viên lập phiếu nhập
    import_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (staff_id)    REFERENCES staff(id)
);

CREATE TABLE stock_import_items (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    import_id     BIGINT        NOT NULL,
    ingredient_id BIGINT        NOT NULL,
    quantity      DECIMAL(12,3) NOT NULL CHECK (quantity > 0),
    unit_cost     DECIMAL(12,0) NOT NULL CHECK (unit_cost >= 0),
    expiry_date   DATE NULL,                                   -- hạn dùng theo lô; NULL nếu không có hạn
    FOREIGN KEY (import_id)     REFERENCES stock_imports(id),
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
    INDEX idx_import_items_expiry (expiry_date)
);

-- 6. Thanh toán (M4)
CREATE TABLE payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT NOT NULL UNIQUE,                    -- một đơn thanh toán một lần (tách bill: stretch goal)
    voucher_id      BIGINT NULL,
    subtotal        DECIMAL(12,0) NOT NULL CHECK (subtotal >= 0),
    discount_amount DECIMAL(12,0) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    vat_amount      DECIMAL(12,0) NOT NULL DEFAULT 0 CHECK (vat_amount >= 0),
    total           DECIMAL(12,0) NOT NULL CHECK (total >= 0),
    method          ENUM('CASH','CARD','TRANSFER','EWALLET') NOT NULL,
    paid_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id)   REFERENCES orders(id),
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id),
    CHECK (discount_amount <= subtotal),
    CHECK (total = subtotal - discount_amount + vat_amount),   -- công thức SPEC D1: total = (subtotal - giảm) + VAT
    INDEX idx_payments_paid_at (paid_at)
);
