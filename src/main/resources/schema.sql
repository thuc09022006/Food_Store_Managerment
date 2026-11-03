-- FoodStore Management System — schema (MySQL 8.0.16+)
-- Chạy một lần trên database trống:
--   mysql -u root -p -e "CREATE DATABASE foodstore_db CHARACTER SET utf8mb4;"
--   mysql -u root -p foodstore_db < src/main/resources/schema.sql
-- Muốn làm lại từ đầu: DROP DATABASE foodstore_db; rồi tạo lại và chạy lại file này.
-- Các bảng khai báo theo đúng thứ tự phụ thuộc khóa ngoại. SAU PHASE 0 FILE NÀY ĐƯỢC ĐÓNG BĂNG:
-- muốn sửa phải hỏi nhóm trước.

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
    FOREIGN KEY (component_id) REFERENCES products(id)
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
    min_order      DECIMAL(12,0) NOT NULL DEFAULT 0,
    valid_from     DATE NOT NULL,
    valid_to       DATE NOT NULL,
    usage_limit    INT  NOT NULL CHECK (usage_limit > 0),
    used_count     INT  NOT NULL DEFAULT 0 CHECK (used_count >= 0)
);

-- 4. Đơn hàng (M2)
CREATE TABLE orders (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id    BIGINT NULL,                                   -- NULL nếu mang đi; service bắt buộc có khi DINE_IN
    customer_id BIGINT NULL,                                   -- NULL nếu khách vãng lai
    staff_id    BIGINT NOT NULL,
    order_type  ENUM('DINE_IN','TAKEAWAY') NOT NULL,
    status      ENUM('NEW','PREPARING','SERVED','PAID','CANCELLED') NOT NULL DEFAULT 'NEW',
    created_at  DATETIME NOT NULL,
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
    unit_price DECIMAL(12,0) NOT NULL,                         -- giá CHỤP tại thời điểm gọi món
    note       VARCHAR(255),
    FOREIGN KEY (order_id)   REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
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
    staff_id    BIGINT   NOT NULL,
    import_date DATETIME NOT NULL,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (staff_id)    REFERENCES staff(id)
);

CREATE TABLE stock_import_items (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    import_id     BIGINT        NOT NULL,
    ingredient_id BIGINT        NOT NULL,
    quantity      DECIMAL(12,3) NOT NULL CHECK (quantity > 0),
    unit_cost     DECIMAL(12,0) NOT NULL,
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
    subtotal        DECIMAL(12,0) NOT NULL,
    discount_amount DECIMAL(12,0) NOT NULL DEFAULT 0,
    vat_amount      DECIMAL(12,0) NOT NULL DEFAULT 0,
    total           DECIMAL(12,0) NOT NULL,
    method          ENUM('CASH','CARD','TRANSFER','EWALLET') NOT NULL,
    paid_at         DATETIME NOT NULL,
    FOREIGN KEY (order_id)   REFERENCES orders(id),
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id),
    INDEX idx_payments_paid_at (paid_at)
);
