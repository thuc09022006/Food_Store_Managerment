# M2: Orders & Tables

Folder: `src/main/java/com/foodstore/orders/` · Owner: **Owner 2** (điền tên) · Phase 1 (song song, sau khi Foundation merge)

## Files

| File                                                      | Mục đích                                                                                                                                                      |
|-----------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `orders/OrderStatus.java`                                 | Enum `NEW, PREPARING, SERVED, PAID, CANCELLED` + `canTransitionTo(OrderStatus)` (bảng chuyển trạng thái, hoặc State pattern nếu nhóm muốn thể hiện)            |
| `orders/OrderType.java`                                   | Enum `DINE_IN`, `TAKEAWAY`                                                                                                                                    |
| `orders/Order.java`                                       | Model — `id`, `type`, `tableId` (nullable), `customerId` (nullable), `status`, `createdAt`; composition: danh sách `OrderItem`                                |
| `orders/OrderItem.java`                                   | Model — `productId`, `quantity` (> 0), `unitPrice` (giá **chụp** lúc gọi), `note`                                                                             |
| `orders/TableStatus.java` / `orders/DiningTable.java`     | Enum `FREE, OCCUPIED, RESERVED` và model bàn                                                                                                                  |
| `orders/OrderDao.java` / `orders/JdbcOrderDao.java`       | CRUD đơn + dòng món; `updateStatusIf(long id, OrderStatus from, OrderStatus to)` → số dòng bị đổi (`UPDATE ... WHERE id=? AND status=?`); `findByStatuses(...)`  |
| `orders/TableDao.java` / `orders/JdbcTableDao.java`       | CRUD bàn; `updateStatus`; `hasOpenOrder(long tableId)`                                                                                                        |
| `orders/OrderService.java`                                | **Interface đã chốt ở Foundation** (`getById`, `getItems`, `calcSubtotal`, `markPaid`, `attachCustomer`) — không sửa chữ ký; M2 **thêm** phương thức bên dưới   |
| `orders/OrderServiceImpl.java`                            | `@Service`; mọi thao tác ghi nhiều bước là `@Transactional(rollbackFor = Exception.class)`                                                                    |
| `orders/TableService.java` / `orders/TableServiceImpl.java` | **Interface đã chốt** (`release`); M2 thêm `findAll`, `create`, `update`, `reserve`, `occupy`                                                                 |
| `orders/OrderPanel.java`                                  | Swing — tạo đơn (chọn loại, bàn, món từ `ProductService.findActive()`), sửa/xóa món, ghi chú, gắn khách (tìm bằng `CustomerService`), hủy đơn                 |
| `orders/TablePanel.java`                                  | Swing — lưới bàn theo trạng thái, đặt trước/trả bàn                                                                                                           |
| `orders/KitchenPanel.java`                                | Swing — danh sách đơn `NEW`/`PREPARING`, nút "Bắt đầu làm" và "Xong"; tự làm mới định kỳ bằng `javax.swing.Timer`                                              |
| `src/test/java/com/foodstore/orders/OrderStatusTest.java` | Unit test bảng chuyển trạng thái                                                                                                                              |
| `src/test/java/com/foodstore/orders/OrderServiceTest.java`| Unit test nghiệp vụ đơn — DAO và `ProductService` mock                                                                                                        |

### Phương thức M2 thêm vào `OrderService`

```java
long createOrder(OrderType type, Long tableId);              // DINE_IN bắt buộc có bàn FREE; TAKEAWAY không có bàn
void addItem(long orderId, long productId, int quantity, String note);
void updateItem(long orderId, long itemId, int quantity, String note);
void removeItem(long orderId, long itemId);
void startPreparing(long orderId);                           // NEW → PREPARING
void markServed(long orderId);                               // PREPARING → SERVED (bếp bấm "Xong")
void cancel(long orderId);                                   // NEW/PREPARING → CANCELLED, trả bàn
List<Order> findForKitchen();                                // NEW + PREPARING
```

## Quy tắc nghiệp vụ

| Chủ đề            | Quy tắc                                                                                                                                                              |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Chuyển trạng thái | Chỉ `NEW→PREPARING→SERVED→PAID`, và `NEW/PREPARING→CANCELLED`. Mọi cách chuyển khác bị từ chối. `PAID` và `CANCELLED` là trạng thái cuối                              |
| Đổi trạng thái    | Dùng `updateStatusIf(id, from, to)` và kiểm số dòng; 0 dòng nghĩa là trạng thái đã đổi bởi thao tác khác → ném `BusinessException`                                    |
| Thêm món          | Món phải `active`; `quantity > 0`; `unit_price` lấy từ `ProductService.getById(...).getPrice()` **tại thời điểm thêm**. Chỉ thêm/sửa/xóa món khi đơn ở `NEW`         |
| Bàn               | Đơn `DINE_IN` cần bàn `FREE`, tạo đơn xong bàn thành `OCCUPIED`. Chặn tạo đơn thứ hai trên bàn đang `OCCUPIED` (`hasOpenOrder`)                                      |
| Hủy đơn           | Đơn `DINE_IN` hủy xong phải trả bàn về `FREE`. Đơn `SERVED` không hủy được (phải thanh toán)                                                                          |
| Gắn khách         | `attachCustomer` chỉ cho đơn chưa `PAID`/`CANCELLED`; khách phải tồn tại (`CustomerService.findById`)                                                                 |
| `calcSubtotal`    | `Σ unit_price × quantity` của các dòng món; dùng `BigDecimal`                                                                                                         |
| `markPaid`        | Chỉ `SERVED → PAID`, bằng `updateStatusIf`; không phải `SERVED` thì ném lỗi                                                                                           |
| Đơn rỗng          | Không cho `startPreparing` khi đơn chưa có món nào                                                                                                                    |

## Tasks

- Task: `OrderStatus` với bảng chuyển trạng thái + `OrderType`, `TableStatus`

  - Acceptance: từng cặp `(from, to)` hợp lệ/không hợp lệ đều có test; trạng thái cuối không chuyển đi đâu được
  - Verify: `mvn test` (`OrderStatusTest`)
  - Files: `orders/OrderStatus.java`, `orders/OrderType.java`, `orders/TableStatus.java`, `src/test/java/com/foodstore/orders/OrderStatusTest.java`
- Task: Model + DAO (`Order`, `OrderItem`, `DiningTable`, `OrderDao`, `TableDao` + bản Jdbc)

  - Acceptance: DAO chạy trên dữ liệu seed; `updateStatusIf` trả 0 khi trạng thái hiện tại khác `from`; `table_id`/`customer_id` NULL đọc/ghi không lỗi
  - Verify: chạy tay / smoke test bằng `main`
  - Files: `orders/*.java`
- Task: `OrderServiceImpl` + `TableServiceImpl` + unit test (DAO, `ProductService`, `CustomerService` mock)

  - Acceptance: test cho mọi dòng trong bảng quy tắc; giá chụp không đổi khi giá món đổi sau đó (mock trả giá khác ở lần gọi thứ hai); hủy đơn `DINE_IN` gọi trả bàn; thêm món không active bị từ chối
  - Verify: `mvn test`
  - Files: `orders/OrderServiceImpl.java`, `orders/TableServiceImpl.java`, `src/test/java/com/foodstore/orders/OrderServiceTest.java`
- Task: `OrderPanel`, `TablePanel`, `KitchenPanel`

  - Acceptance: tạo đơn tại bàn và đơn mang đi qua UI trên MySQL thật; bếp thấy đơn mới và đánh dấu xong; đơn chuyển `SERVED` để M4 thanh toán; lỗi nghiệp vụ hiện hộp thoại tiếng Việt
  - Verify: chạy tay
  - Files: `orders/OrderPanel.java`, `orders/TablePanel.java`, `orders/KitchenPanel.java`

## Dependencies

- **Cần:** `schema.sql` + interface Service đã merge; `ProductService` (M1) và `CustomerService` (M4) — dùng seed data và mock nếu chưa xong.
- **Được dùng bởi:** M3 (`getItems` khi trừ kho), M4 (`getById`, `calcSubtotal`, `markPaid`, `release`), M5 (báo cáo, nối panel).

## Checkpoint B contribution

Demo tạo đơn → bếp làm → `SERVED` trên MySQL thật; `OrderStatusTest` và `OrderServiceTest` đạt.
