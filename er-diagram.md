# ER diagram — `foodstore_db`

Sơ đồ dựng từ mục Data Model của `SPEC.md`, chỉ liệt kê khóa và cột then chốt. **Nguồn duy nhất của schema là `src/main/resources/schema.sql`**; khi `schema.sql` đổi thì cập nhật file này.

```mermaid
erDiagram
    categories ||--o{ products : "phân loại"
    products ||--o{ combo_components : "combo_id (combo chứa)"
    products ||--o{ combo_components : "component_id (là thành phần)"
    products ||--o{ order_items : "được gọi"
    products ||--o{ recipes : "có công thức"
    ingredients ||--o{ recipes : "được dùng"
    dining_tables |o--o{ orders : "phục vụ tại bàn"
    customers |o--o{ orders : "đặt"
    orders ||--|{ order_items : "gồm"
    orders ||--o| payments : "thanh toán một lần"
    vouchers |o--o{ payments : "áp dụng"
    suppliers ||--o{ stock_imports : "cung cấp"
    stock_imports ||--|{ stock_import_items : "gồm"
    ingredients ||--o{ stock_import_items : "được nhập"

    staff {
        bigint id PK
        varchar username
        varchar password_hash "BCrypt"
        enum role "ADMIN CASHIER WAITER CHEF"
        boolean is_active
    }
    categories {
        bigint id PK
        varchar name
    }
    products {
        bigint id PK
        bigint category_id FK
        varchar name
        enum product_type "FOOD DRINK COMBO"
        decimal price "DECIMAL(12,0)"
        varchar description "nullable"
        boolean is_active "xóa mềm"
    }
    combo_components {
        bigint combo_id PK
        bigint component_id PK
        int quantity
    }
    dining_tables {
        bigint id PK
        enum status "FREE OCCUPIED RESERVED"
    }
    customers {
        bigint id PK
        int points
        enum tier "NORMAL SILVER GOLD"
    }
    vouchers {
        bigint id PK
        varchar code
        enum discount_type "PERCENT FIXED"
        decimal discount_value
        decimal min_order
        int usage_limit
        int used_count
    }
    orders {
        bigint id PK
        bigint table_id FK "nullable"
        bigint customer_id FK "nullable"
        enum status "NEW PREPARING SERVED PAID CANCELLED"
    }
    order_items {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
        decimal unit_price "giá chụp lúc gọi"
    }
    suppliers {
        bigint id PK
        varchar name
    }
    ingredients {
        bigint id PK
        varchar name
        decimal stock_quantity "CHECK >= 0"
        decimal min_quantity "CHECK >= 0"
    }
    recipes {
        bigint product_id PK
        bigint ingredient_id PK
        decimal quantity "cho 1 phần"
    }
    stock_imports {
        bigint id PK
        bigint supplier_id FK
    }
    stock_import_items {
        bigint id PK
        bigint import_id FK
        bigint ingredient_id FK
        decimal quantity
        date expiry_date "nullable, theo lô"
    }
    payments {
        bigint id PK
        bigint order_id FK "UNIQUE"
        bigint voucher_id FK "nullable"
        decimal subtotal
        decimal discount_amount
        decimal vat_amount
        decimal total
        enum method "CASH CARD TRANSFER EWALLET"
    }
```

## Chuyển sang lược đồ quan hệ (tóm tắt)

- `combo_components(combo_id, component_id, quantity)` — khóa chính ghép; cả hai cột là khóa ngoại tới `products(id)`. Đây là quan hệ nhiều-nhiều tự tham chiếu của `products`.
- `recipes(product_id, ingredient_id, quantity)` — quan hệ nhiều-nhiều giữa `products` và `ingredients`, khóa chính ghép.
- `payments.order_id UNIQUE` — biến quan hệ `orders`–`payments` thành 1–0..1, chặn thanh toán đôi ở mức DB.
- `orders.table_id`, `orders.customer_id`, `payments.voucher_id` nullable — quan hệ tùy chọn (đơn mang đi không có bàn; khách vãng lai; không dùng voucher).
