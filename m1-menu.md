# M1: Menu

Folder: `src/main/java/com/foodstore/menu/` · Owner: **Owner 1** (điền tên) · Phase 1 (song song, sau khi Foundation merge)

## Files

| File | Mục đích |
|------|----------|
| `menu/Product.java` | **Abstract** — `id`, `categoryId`, `name`, `price`, `description`, `active`; phương thức trừu tượng `getType()` (Foundation đã tạo bản đầu, M1 mở rộng) |
| `menu/Food.java` | `extends Product` — món ăn |
| `menu/Drink.java` | `extends Product` — đồ uống |
| `menu/Combo.java` | `extends Product` — chứa `List<ComboComponent>` (Composite); có giá riêng, `getComponentsTotal()`, `getSavings()`; từ chối combo lồng combo |
| `menu/ComboComponent.java` | `product` + `quantity` |
| `menu/Category.java` | Model — `id`, `name` |
| `menu/ProductFactory.java` | `create(...)` — dựng đúng lớp con theo `product_type` |
| `menu/CategoryDao.java` / `menu/JdbcCategoryDao.java` | CRUD `categories` (bản mẫu từ Foundation) |
| `menu/ProductDao.java` | Interface — `findAll()`, `findById(long)`, `search(String name, Long categoryId, Boolean active)`, `save`, `update`, `setActive(long, boolean)`, `findComponents(long comboId)`, `saveComponents(long comboId, List<ComboComponent>)`, `existsInOrders(long productId)` |
| `menu/JdbcProductDao.java` | `JdbcTemplate` — SQL viết tay cho từng phương thức trên (`existsInOrders` đếm trong `order_items`) |
| `menu/ProductService.java` | **Interface đã chốt ở Foundation** (`getById`, `findActive`, `getComponents`) — không sửa chữ ký |
| `menu/ProductServiceImpl.java` | `@Service` cài đặt interface trên; validate và bật/tắt bán |
| `menu/MenuPanel.java` | Swing `JPanel` — bảng món, form thêm/sửa, ô tìm kiếm + lọc, quản lý danh mục, chỉnh thành phần combo |
| `src/test/java/com/foodstore/menu/ProductServiceTest.java` | Unit test — DAO mock |

## Tasks

- Task: Hierarchy `Product` (abstract) → `Food`, `Drink`, `Combo` + `ProductFactory`
  - Acceptance: `ProductFactory.create("COMBO", ...)` trả `Combo`; gọi `getType()` qua tham chiếu kiểu `Product` ra đúng loại của từng lớp con (đa hình); `Combo.getSavings()` đúng
  - Verify: `mvn test` (test factory + đa hình)
  - Files: `menu/Product.java`, `menu/Food.java`, `menu/Drink.java`, `menu/Combo.java`, `menu/ComboComponent.java`, `menu/ProductFactory.java`
- Task: `Category` + `ProductDao` (interface + `JdbcProductDao`): CRUD, tìm kiếm/lọc, bật/tắt bán, đọc/ghi thành phần combo
  - Acceptance: DAO hoạt động trên dữ liệu seed; `search` lọc đúng theo tên/danh mục/trạng thái; đọc `description` NULL không ném lỗi
  - Verify: chạy tay / smoke test bằng `main`
  - Files: `menu/Category.java`, `menu/ProductDao.java`, `menu/JdbcProductDao.java`
- Task: `ProductService` + unit test (DAO mock): tên bắt buộc, giá ≥ 0, combo phải có ít nhất một thành phần và thành phần **không được là combo**, món đã nằm trong đơn thì không xóa mà chuyển `active=false`
  - Acceptance: từng quy tắc có cặp test đạt/không đạt; xóa món đã có trong đơn chuyển thành ngừng bán
  - Verify: `mvn test`
  - Files: `menu/ProductServiceImpl.java`, `src/test/java/com/foodstore/menu/ProductServiceTest.java`
- Task: `MenuPanel` (bảng món + form + tìm kiếm/lọc + danh mục + chỉnh combo)
  - Acceptance: thêm, sửa, ngừng bán, tìm kiếm một món và tạo một combo qua giao diện trên MySQL thật
  - Verify: chạy tay
  - Files: `menu/MenuPanel.java`

## Dependencies

- **Cần:** `schema.sql` + interface Service đã merge (Foundation). Không phụ thuộc module nào khác — an toàn để bắt đầu đầu tiên ở Phase 1.
- **Được dùng bởi:** M2 (chọn món khi tạo đơn, kiểm món đang bán), M3 (công thức theo `Product`, mở rộng combo khi trừ kho), M5 (báo cáo món bán chạy), M5 Phase 2 (nối panel).

## Checkpoint B contribution

Demo CRUD `MenuPanel` độc lập trên MySQL thật, gồm một combo; `ProductServiceTest` đạt.
