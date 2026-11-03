# M2: Orders & Tables

Folder: `src/main/java/com/foodstore/orders/` · Owner: **Owner 2** (điền tên) · Phase 1 (song song, sau khi Foundation merge)

## Files

| File | Mục đích |
|------|----------|
| `orders/Order.java` | Model (Foundation đã tạo bản đầu) — `id`, `tableId` (nullable), `customerId` (nullable), `staffId`, `orderType`, `status`, `createdAt`, `note`, `List<OrderItem>` |
| `orders/OrderItem.java` | Model — `id`, `orderId`, `productId`, `quantity`, `unitPrice` (giá chụp), `note`; `lineTotal()` |
| `orders/OrderType.java` | Enum `DINE_IN`, `TAKEAWAY` |
| `orders/OrderStatus.java` | `NEW`, `PREPARING`, `SERVED`, `PAID`, `CANCELLED` + `canTransitionTo(OrderStatus)` (State pattern hoặc enum có bảng chuyển hợp lệ) |
| `orders/DiningTable.java` / `orders/TableStatus.java` | Bàn (`FREE`, `OCCUPIED`, `RESERVED`) |
| `orders/OrderDao.java` | Interface — `save(Order)`, `findById`, `findByStatus(OrderStatus)`, `findOpenByTable(long)`, `updateStatus(long, OrderStatus)`, `addItem`, `updateItem`, `removeItem`, `findItems(long orderId)` |
| `orders/JdbcOrderDao.java` | `JdbcTemplate` — SQL viết tay |
| `orders/TableDao.java` / `orders/JdbcTableDao.java` | CRUD `dining_tables`, `updateStatus` |
| `orders/OrderService.java` | **Interface đã chốt ở Foundation** (`getById`, `getItems`, `calcSubtotal`, `markPaid`) — không sửa chữ ký |
| `orders/OrderServiceImpl.java` | `@Service` cài đặt: tạo đơn, thêm/xóa món, đổi trạng thái hợp lệ, hủy đơn, `markPaid` |
| `orders/TableService.java` | **Interface đã chốt ở Foundation** (`release`); M2 được **thêm** phương thức (CRUD bàn, `occupy`) |
| `orders/TableServiceImpl.java` | `@Service` cài đặt `TableService` |
| `orders/OrderPanel.java` | Swing `JPanel` — chọn bàn/mang đi, chọn món (từ M1), sửa số lượng, ghi chú, đổi trạng thái, xem danh sách đơn |
| `orders/TablePanel.java` | Swing `JPanel` — sơ đồ/danh sách bàn theo trạng thái, CRUD bàn |
| `orders/KitchenPanel.java` | Swing `JPanel` cho Chef — đơn `NEW`/`PREPARING`, nút nhận chế biến và đánh dấu xong |
| `src/test/java/com/foodstore/orders/OrderServiceTest.java` | Unit test — DAO và `ProductService` mock |

## Tasks

- Task: `Order`, `OrderItem`, `OrderStatus`, `DiningTable` + `OrderDao`/`TableDao` (interface + Jdbc)
  - Acceptance: tạo đơn kèm các dòng món trên dữ liệu seed; `findByStatus` và `findOpenByTable` trả đúng
  - Verify: chạy tay / smoke test
  - Files: `orders/Order.java`, `orders/OrderItem.java`, `orders/OrderType.java`, `orders/OrderStatus.java`, `orders/DiningTable.java`, `orders/TableStatus.java`, `orders/OrderDao.java`, `orders/JdbcOrderDao.java`, `orders/TableDao.java`, `orders/JdbcTableDao.java`
- Task: `OrderService` + unit test: đơn `DINE_IN` bắt buộc có bàn và bàn phải không `OCCUPIED`; thêm món lấy `unit_price` **hiện tại** của `Product` và lưu vào dòng đơn; không thêm món đang ngừng bán; chỉ sửa/xóa món khi đơn còn `NEW`; chuyển trạng thái sai thứ tự bị từ chối; hủy đơn chỉ khi chưa `PAID`
  - Acceptance: mỗi quy tắc có cặp test đạt/không đạt; sửa giá `Product` sau đó không làm đổi `unitPrice` của dòng đã tạo
  - Verify: `mvn test`
  - Files: `orders/OrderServiceImpl.java`, `orders/TableServiceImpl.java`, `src/test/java/com/foodstore/orders/OrderServiceTest.java`
- Task: `OrderPanel`, `TablePanel`, `KitchenPanel`
  - Acceptance: tạo đơn cho một bàn, thêm 3 món, bếp nhận và đánh dấu xong, phục vụ chuyển `SERVED` — tất cả qua UI trên MySQL thật; bàn tự chuyển `OCCUPIED`
  - Verify: chạy tay
  - Files: `orders/OrderPanel.java`, `orders/TablePanel.java`, `orders/KitchenPanel.java`

## Dependencies

- **Cần:** `schema.sql` + interface Service đã merge (Foundation); đọc `Product` qua `ProductService` (M1) — xây trên seed data nếu M1 chưa xong.
- **Được dùng bởi:** M3 (`getItems` để trừ kho), M4 (`calcSubtotal`, `markPaid`, `TableService.release`), M5 (báo cáo, nối panel).

## Checkpoint B contribution

Demo vòng đời đơn `NEW → PREPARING → SERVED` cùng quản lý bàn độc lập trên MySQL thật; `OrderServiceTest` đạt.
