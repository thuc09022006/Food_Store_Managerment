-- FoodStore Management System — dữ liệu mẫu (seed)
-- Chạy MỘT LẦN trên database mới, SAU schema.sql:
--   mysql -u root -p --default-character-set=utf8mb4 foodstore_db -e "source src/main/resources/data.sql"
-- (dùng -e "source ..." vì toán tử "<" KHÔNG chạy trong PowerShell; chạy tại thư mục gốc của repo)
-- Chạy lần hai sẽ lỗi trùng khóa. Muốn nạp lại: DROP DATABASE, tạo lại, chạy schema.sql rồi data.sql.
--
-- Tài khoản đăng nhập mẫu (CHỈ DÙNG KHI PHÁT TRIỂN, đổi mật khẩu trước khi demo ngoài nhóm):
--   admin   / Admin@123
--   cashier / Cashier@123
--   waiter  / Waiter@123
--   chef    / Chef@123
-- Cột password_hash chứa hash BCrypt (cost 10), không phải mật khẩu thật.

SET NAMES utf8mb4;

-- ---------- staff (id 1-4) ----------
INSERT INTO staff (id, username, password_hash, full_name, phone, role, is_active) VALUES
 (1, 'admin',   '$2a$10$QExpsZZTQQmEURl/KtcEu..iQd77Ap/1PjiqPx58aiol6uGhtx0wm',   'Nguyễn Quản Lý', '0900000001', 'ADMIN',   TRUE),
 (2, 'cashier', '$2a$10$67zg6aG0nZCuleqqVuRDc.51Bz.bDNqzlaE4OoXyrnu5nEEL5CwWC', 'Trần Thu Ngân',  '0900000002', 'CASHIER', TRUE),
 (3, 'waiter',  '$2a$10$8pma8fB9vb4NOz9dsepbseJbretwk1x500S/UaD1OQIEWR1ECPy.y',  'Lê Phục Vụ',     '0900000003', 'WAITER',  TRUE),
 (4, 'chef',    '$2a$10$bM7e/3qAIk9QB5JroZtMZe0BKMUoWHF8G36sB46JyTbrROT5zSEv6',    'Phạm Đầu Bếp',   '0900000004', 'CHEF',    TRUE);

-- ---------- categories ----------
INSERT INTO categories (id, name) VALUES
 (1, 'Món chính'),
 (2, 'Món phụ'),
 (3, 'Đồ uống'),
 (4, 'Combo');

-- ---------- products (giá VND) ----------
INSERT INTO products (id, category_id, name, product_type, price, description, is_active) VALUES
 (1, 1, 'Phở bò',          'FOOD',  55000, 'Phở bò tái chín',            TRUE),
 (2, 1, 'Cơm gà',          'FOOD',  50000, 'Cơm gà xối mỡ',              TRUE),
 (3, 1, 'Bún chả',         'FOOD',  55000, NULL,                         TRUE),
 (4, 2, 'Gỏi cuốn',        'FOOD',  30000, 'Gỏi cuốn tôm (2 cuốn)',     TRUE),
 (5, 3, 'Trà đá',          'DRINK',  5000, NULL,                         TRUE),
 (6, 3, 'Cà phê sữa',      'DRINK', 25000, NULL,                         TRUE),
 (7, 3, 'Nước cam',        'DRINK', 30000, 'Cam vắt tươi',               TRUE),
 (8, 4, 'Combo Cơm gà + Cà phê sữa', 'COMBO', 65000, 'Tiết kiệm 10.000đ', TRUE);

-- combo 8 gồm: 1 Cơm gà + 1 Cà phê sữa
INSERT INTO combo_components (combo_id, component_id, quantity) VALUES
 (8, 2, 1),
 (8, 6, 1);

-- ---------- dining_tables ----------
INSERT INTO dining_tables (id, code, seats, status) VALUES
 (1, 'T01', 2, 'FREE'),
 (2, 'T02', 4, 'FREE'),
 (3, 'T03', 4, 'FREE'),
 (4, 'T04', 6, 'FREE'),
 (5, 'T05', 8, 'FREE');

-- ---------- customers (ngưỡng hạng lấy từ application.properties: SILVER >= 500 điểm) ----------
INSERT INTO customers (id, full_name, phone, points, tier) VALUES
 (1, 'Nguyễn Văn An', '0901000001', 120, 'NORMAL'),
 (2, 'Trần Thị Bình', '0901000002', 650, 'SILVER');

-- ---------- vouchers (1 còn hạn, 1 đã hết hạn để test từ chối) ----------
INSERT INTO vouchers (id, code, discount_type, discount_value, min_order, valid_from, valid_to, usage_limit, used_count) VALUES
 (1, 'GIAM10',  'PERCENT', 10.00,    100000, '2026-01-01', '2027-12-31', 100, 0),
 (2, 'HETHAN',  'FIXED',   20000.00,      0, '2025-01-01', '2025-12-31',  50, 0);

-- ---------- ingredients ----------
INSERT INTO ingredients (id, name, unit, stock_quantity, min_quantity) VALUES
 (1,  'Thịt bò',       'kg',  20.000,  5.000),
 (2,  'Bánh phở',      'kg',  15.000,  4.000),
 (3,  'Thịt gà',       'kg',  12.000,  4.000),
 (4,  'Gạo',           'kg',  30.000, 10.000),
 (5,  'Thịt heo',      'kg',  10.000,  3.000),
 (6,  'Bún tươi',      'kg',  12.000,  4.000),
 (7,  'Tôm',           'kg',   2.050,  2.000),   -- sát mức tối thiểu: bán 1 phần Gỏi cuốn (0,050 kg) đưa về đúng mức → phát cảnh báo
 (8,  'Bánh tráng',    'gói', 20.000,  5.000),
 (9,  'Trà',           'kg',   2.000,  0.500),
 (10, 'Cà phê bột',    'kg',   5.000,  1.000),
 (11, 'Sữa đặc',       'hộp', 24.000,  6.000),
 (12, 'Cam',           'kg',   2.500,  3.000);   -- đã dưới mức tối thiểu: trang Kho hiện cảnh báo ngay

-- ---------- recipes: lượng nguyên liệu cho 1 phần (combo không có công thức, được mở rộng theo thành phần) ----------
INSERT INTO recipes (product_id, ingredient_id, quantity) VALUES
 (1, 1, 0.150), (1, 2, 0.200),          -- Phở bò: thịt bò, bánh phở
 (2, 4, 0.150), (2, 3, 0.200),          -- Cơm gà: gạo, thịt gà
 (3, 5, 0.150), (3, 6, 0.200),          -- Bún chả: thịt heo, bún tươi
 (4, 7, 0.050), (4, 8, 0.100),          -- Gỏi cuốn: tôm, bánh tráng
 (5, 9, 0.005),                         -- Trà đá: trà
 (6, 10, 0.020), (6, 11, 0.050),        -- Cà phê sữa: cà phê bột, sữa đặc
 (7, 12, 0.300);                        -- Nước cam: cam

-- ---------- suppliers ----------
INSERT INTO suppliers (id, name, phone, address) VALUES
 (1, 'Công ty Thực phẩm Tươi Sống', '0281234567', '12 Nguyễn Huệ, Quận 1'),
 (2, 'Đại lý Nông sản Ba Miền',     '0287654321', '45 Lê Lợi, Quận 3');

-- ---------- 1 phiếu nhập mẫu, có lô sắp hết hạn trong 3 ngày để thấy cảnh báo ----------
INSERT INTO stock_imports (id, supplier_id, staff_id, import_date) VALUES
 (1, 1, 1, NOW());

INSERT INTO stock_import_items (id, import_id, ingredient_id, quantity, unit_cost, expiry_date) VALUES
 (1, 1, 1, 10.000, 250000, DATE_ADD(CURDATE(), INTERVAL 3 DAY)),   -- thịt bò, sắp hết hạn
 (2, 1, 3,  8.000,  90000, DATE_ADD(CURDATE(), INTERVAL 5 DAY)),   -- thịt gà
 (3, 1, 4, 20.000,  18000, NULL);                                    -- gạo, không có hạn

-- ---------- 2 đơn ĐÃ THANH TOÁN hôm qua để báo cáo doanh thu có dữ liệu ----------
-- (VAT trong seed tính theo 8%; đây là dữ liệu lịch sử cố định, không đổi khi đổi store.vat-rate)
INSERT INTO orders (id, table_id, customer_id, staff_id, order_type, status, created_at, note) VALUES
 (1, 1,    1,    3, 'DINE_IN',  'PAID', NOW() - INTERVAL 1 DAY, NULL),
 (2, NULL, NULL, 3, 'TAKEAWAY', 'PAID', NOW() - INTERVAL 1 DAY, 'Ít cay');

INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, note) VALUES
 (1, 1, 1, 2, 55000, NULL),          -- 2 Phở bò
 (2, 1, 5, 2,  5000, 'Ít đá'),       -- 2 Trà đá          => subtotal 120.000
 (3, 2, 2, 1, 50000, NULL),          -- 1 Cơm gà
 (4, 2, 6, 1, 25000, NULL);          -- 1 Cà phê sữa      => subtotal  75.000

INSERT INTO payments (id, order_id, voucher_id, subtotal, discount_amount, vat_amount, total, method, paid_at) VALUES
 (1, 1, NULL, 120000, 0, 9600, 129600, 'CASH',     NOW() - INTERVAL 1 DAY),
 (2, 2, NULL,  75000, 0, 6000,  81000, 'TRANSFER', NOW() - INTERVAL 1 DAY);
