# M2: Orders & Tables

Folder: `src/main/java/com/foodstore/orders/` · Template: `templates/orders/`, `templates/tables/`, `templates/kitchen/` · Owner: **Owner 2** (điền tên) · Phase 1 (song song, sau khi Foundation merge)

## Files

| File                                                       | Mục đích                                                                                                                                                              |
|------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `orders/OrderStatus.java`                                  | Enum `NEW, PREPARING, SERVED, PAID, CANCELLED` + `canTransitionTo(OrderStatus)` (bảng chuyển trạng thái, hoặc State pattern)                                          |
| `orders/OrderType.java`                                    | Enum `DINE_IN`, `TAKEAWAY`, `DELIVERY` (giao hàng: xem `m6-delivery.md`)                                                                                                                                            |
| `orders/Order.java`                                        | Model — `id`, `type`, `tableId` (nullable), `customerId` (nullable), `staffId`, `status`, `createdAt`, `note`; composition: danh sách `OrderItem`                                        |
| `orders/OrderItem.java`                                    | Model — `productId`, `quantity` (> 0), `unitPrice` (giá **chụp** lúc gọi), `note`                                                                                     |
| `orders/TableStatus.java` / `orders/DiningTable.java`      | Enum `FREE, OCCUPIED, RESERVED` và model bàn (`code` duy nhất, `seats` > 0)                                                                                                                          |
| `orders/OrderDao.java` / `JdbcOrderDao.java`               | CRUD đơn + dòng món; `int updateStatusIf(long id, OrderStatus from, OrderStatus to)` (`UPDATE ... WHERE id=? AND status=?`, trả số dòng đổi); `findByStatuses(...)`    |
| `orders/TableDao.java` / `JdbcTableDao.java`               | CRUD bàn; `int updateStatusIf(long id, TableStatus from, TableStatus to)`; `hasOpenOrder(long tableId)`                                                               |
| `orders/OrderService.java` / `OrderServiceImpl.java`       | Interface đã chốt ở Foundation (`getById`, `getItems`, `calcSubtotal`, `markPaid`, `attachCustomer`) — không đổi; M2 **thêm** phương thức bên dưới                    |
| `orders/TableService.java` / `TableServiceImpl.java`       | Đã chốt `release`; M2 thêm `findAll`, `create`, `update`, `reserve`, `occupy`                                                                                         |
| `orders/OrderForm.java`, `AddItemForm.java`                | Form: loại đơn, bàn (bắt buộc khi `DINE_IN`), ghi chú đơn (`@Size(max=255)`), món + số lượng (`@Min(1)`) + ghi chú món (`@Size(max=255)`)                                                                          |
| `orders/OrderController.java`                              | `/orders` — danh sách, tạo đơn, chi tiết đơn, thêm/sửa/xóa món, gắn khách (tìm bằng `CustomerService.findByPhone`), hủy đơn. Danh sách món chọn lấy từ `ProductService.findActive()` |
| `orders/TableController.java`                              | `/tables` — lưới bàn theo trạng thái, đặt trước/trả bàn, quản lý bàn                                                                                                  |
| `orders/KitchenController.java`                            | `/kitchen` — đơn `NEW`/`PREPARING`; POST "Bắt đầu làm" và "Xong". Trang tự làm mới định kỳ (`<meta http-equiv="refresh" content="15">` hoặc một đoạn JS `fetch`)         |
| `templates/orders/list.html`, `detail.html`, `new.html`    | Danh sách đơn, chi tiết đơn (bảng món + form thêm món + nút chuyển trạng thái), tạo đơn                                                                               |
| `templates/tables/list.html`, `templates/kitchen/list.html` | Lưới bàn; danh sách cho bếp                                                                                                                                         |
| `src/test/java/com/foodstore/orders/OrderStatusTest.java`  | Unit test bảng chuyển trạng thái                                                                                                                                      |
| `src/test/java/com/foodstore/orders/OrderServiceTest.java` | Unit test nghiệp vụ đơn — DAO và `ProductService` mock                                                                                                                |
| `src/test/java/com/foodstore/orders/OrderControllerTest.java` | MockMvc: tạo đơn redirect đúng; số lượng 0 bị trả lại kèm lỗi; Chef (không có `ORDERS_EDIT`) POST nhận `403`; Chef (không có `ORDERS_VIEW`) GET cũng `403`           |

### Phương thức M2 thêm vào `OrderService`

```java
long createOrder(OrderType type, Long tableId, long staffId, String note);  // DINE_IN: bàn phải FREE; TAKEAWAY: không bàn; staffId = người đang đăng nhập (orders.staff_id NOT NULL)
void addItem(long orderId, long productId, int quantity, String note);
void updateItem(long orderId, long itemId, int quantity, String note);
void removeItem(long orderId, long itemId);
void startPreparing(long orderId);                           // NEW → PREPARING
void markServed(long orderId);                               // PREPARING → SERVED (bếp bấm "Xong")
void cancel(long orderId);                                   // NEW/PREPARING → CANCELLED, trả bàn
List<Order> findForKitchen();                                // NEW + PREPARING
```

### Đường dẫn chính

| Method | URL                              | Quyền                     | Việc                             |
|--------|----------------------------------|---------------------------|----------------------------------|
| GET    | `/orders`, `/orders/{id}`        | `ORDERS_VIEW`             | Danh sách, chi tiết              |
| POST   | `/orders`                        | `ORDERS_EDIT`             | Tạo đơn → redirect `/orders/{id}` |
| POST   | `/orders/{id}/items`, `/items/{itemId}`, `/items/{itemId}/delete` | `ORDERS_EDIT` | Thêm/sửa/xóa món |
| POST   | `/orders/{id}/customer`          | `ORDERS_EDIT`             | Gắn khách                        |
| POST   | `/orders/{id}/cancel`            | `ORDERS_EDIT`             | Hủy đơn                          |
| GET    | `/tables`                        | `TABLES_VIEW`             | Lưới bàn                         |
| POST   | `/tables/...`                    | `TABLES_EDIT`             | Đặt trước/trả bàn/quản lý        |
| GET    | `/kitchen`                       | `KITCHEN_VIEW`            | Danh sách cho bếp                |
| POST   | `/kitchen/{orderId}/start`, `/done` | `KITCHEN_EDIT`         | Bắt đầu làm / Xong               |

> Quyền do `SecurityConfig` sinh từ `Feature`; không tự viết thêm trong controller.

## Quy tắc nghiệp vụ

| Chủ đề            | Quy tắc                                                                                                                                                                                     |
|-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Chuyển trạng thái | Chỉ `NEW→PREPARING→SERVED→PAID` và `NEW/PREPARING→CANCELLED`; `PAID`, `CANCELLED` là cuối                                                                                                     |
| Đổi trạng thái    | Dùng `updateStatusIf(id, from, to)` và kiểm số dòng; 0 dòng nghĩa là người khác đã đổi trạng thái → ném `BusinessException("Đơn đã được cập nhật bởi người khác, hãy tải lại trang")`      |
| Thêm món          | Món phải `active`; `quantity > 0`; `unit_price` lấy từ `ProductService.getById(...).getPrice()` **tại thời điểm thêm**. Chỉ thêm/sửa/xóa món khi đơn `NEW`                                  |
| Bàn               | Đơn `DINE_IN` cần bàn `FREE`. Chiếm bàn bằng `updateStatusIf(tableId, FREE, OCCUPIED)`: hai nhân viên chọn cùng bàn cùng lúc thì chỉ một người thành công, người còn lại nhận lỗi          |
| Hủy đơn           | Đơn `DINE_IN` hủy xong trả bàn về `FREE`. Đơn `SERVED` không hủy được (phải thanh toán), **trừ đơn `DELIVERY`** (giao thất bại/hủy giao sau khi món đã xong)                                                                                                      |
| Gắn khách         | `attachCustomer` chỉ cho đơn chưa `PAID`/`CANCELLED`; khách phải tồn tại                                                                                                                    |
| `calcSubtotal`    | `Σ unit_price × quantity`, `BigDecimal`                                                                                                                                                     |
| `markPaid`        | Chỉ `SERVED → PAID` bằng `updateStatusIf`; không phải `SERVED` thì ném lỗi                                                                                                                  |
| Đơn rỗng          | Không cho `startPreparing` khi đơn chưa có món                                                                                                                                              |
| Người tạo         | `staffId` luôn lấy từ người đang đăng nhập (`@AuthenticationPrincipal`), **không** nhận từ form hay URL                                                                                      |
| ID từ URL         | `orderId`, `itemId` luôn được kiểm là thuộc đúng đơn (`itemId` phải thuộc `orderId`) — không tin tham số trên URL                                                                           |

## Tasks

- Task: `OrderStatus` + `OrderType` + `TableStatus`

  - Acceptance: từng cặp `(from, to)` hợp lệ/không hợp lệ đều có test; trạng thái cuối không chuyển đi đâu
  - Verify: `mvn test` (`OrderStatusTest`)
  - Files: `orders/OrderStatus.java`, `OrderType.java`, `TableStatus.java`, `OrderStatusTest.java`
- Task: Model + DAO (`Order`, `OrderItem`, `DiningTable`, `OrderDao`, `TableDao` + Jdbc)

  - Acceptance: DAO chạy trên seed; `updateStatusIf` trả 0 khi trạng thái hiện tại khác `from`; `table_id`/`customer_id` NULL đọc/ghi không lỗi
  - Verify: chạy tay / smoke test
  - Files: `orders/*.java`
- Task: `OrderServiceImpl` + `TableServiceImpl` + unit test (DAO, `ProductService`, `CustomerService` mock)

  - Acceptance: test cho mọi dòng bảng quy tắc; giá chụp không đổi khi giá món đổi sau đó; hủy đơn `DINE_IN` trả bàn; thêm món không active bị từ chối; `updateStatusIf` trả 0 thì ném lỗi
  - Verify: `mvn test`
  - Files: `orders/OrderServiceImpl.java`, `TableServiceImpl.java`, `OrderServiceTest.java`
- Task: Form + `OrderController`, `TableController`, `KitchenController` + template

  - Acceptance: trên trình duyệt tạo đơn tại bàn và đơn mang đi, thêm món, gắn khách; bếp thấy đơn mới (tự làm mới) và bấm Xong; đơn sang `SERVED` để M4 thanh toán; form sai hiện lỗi cạnh ô; sau POST redirect; hai trình duyệt cùng chọn một bàn thì chỉ một thành công
  - Verify: `mvn test` (`OrderControllerTest`); chạy tay với `waiter`, `chef`
  - Files: `orders/*Form.java`, `*Controller.java`, `templates/orders/*`, `templates/tables/*`, `templates/kitchen/*`, `OrderControllerTest.java`

## Dependencies

- **Cần:** Foundation; `ProductService` (M1) và `CustomerService` (M4) — dùng seed + mock nếu chưa xong.
- **Được dùng bởi:** M3 (`getItems`), M4 (`getById`, `calcSubtotal`, `markPaid`, `release`), M5 (báo cáo).

## Checkpoint B contribution

Demo tạo đơn → bếp làm → `SERVED` trên MySQL thật; `OrderStatusTest`, `OrderServiceTest`, `OrderControllerTest` đạt.
