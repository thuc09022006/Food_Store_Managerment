# M6: Delivery (giao hàng)

Folder: `src/main/java/com/foodstore/delivery/` · Template: `templates/delivery/` · Thêm sau Phase 1; phụ thuộc M1 (`ProductService`) và M2 (`OrderService`).

## Mô hình

- Đơn giao là một `orders` có `order_type = 'DELIVERY'` + một dòng `deliveries` (1-1, `order_id` UNIQUE). Bếp xử lý đơn bằng vòng đời đơn của M2 (NEW → PREPARING → SERVED).
- `shippers` là bảng riêng, không đăng nhập: nhân viên điều phối cập nhật trạng thái thay shipper. Không đổi bảng `staff`/phân quyền.
- Schema chỉ **thêm** 2 bảng và thêm giá trị `DELIVERY` vào `orders.order_type` (xem `schema.sql`, mục 4b).

## Trạng thái giao hàng (`DeliveryStatus`)

| Trạng thái   | Ý nghĩa                         | Chuyển được tới                          |
|--------------|---------------------------------|------------------------------------------|
| `PENDING`    | Chờ phân công                   | `ASSIGNED`, `CANCELLED`                  |
| `ASSIGNED`   | Đã phân công shipper            | `ASSIGNED` (đổi shipper), `DELIVERING`, `CANCELLED` |
| `DELIVERING` | Shipper đã lấy hàng, đang giao  | `DELIVERED`, `FAILED`                    |
| `DELIVERED`  | Đã giao (cuối)                  | —                                        |
| `FAILED`     | Giao thất bại (có lý do)        | `ASSIGNED` (giao lại), `CANCELLED`       |
| `CANCELLED`  | Đã hủy giao (cuối)              | —                                        |

## Quy tắc nghiệp vụ

| Chủ đề            | Quy tắc |
|-------------------|---------|
| Lấy hàng          | `ASSIGNED → DELIVERING` chỉ khi đơn đã `SERVED`; mỗi lần lấy hàng `attempt_count + 1` |
| Phân công         | Shipper phải đang hoạt động; mỗi shipper giữ tối đa `store.delivery.max-active-per-shipper` đơn (ASSIGNED + DELIVERING) |
| Giao lại          | `FAILED → ASSIGNED` chỉ khi `attempt_count < store.delivery.max-attempts`; hết lượt thì chỉ hủy |
| Giao thất bại     | Bắt buộc có lý do (≤ 255 ký tự) |
| Hủy               | Không hủy khi `DELIVERING` (báo thất bại trước). Hủy giao = hủy đơn; đơn đã `PAID` thì từ chối (cần hoàn tiền thủ công) |
| Đồng thời         | Mọi chuyển trạng thái dùng UPDATE có điều kiện và kiểm số dòng (SPEC D4); 0 dòng → `BusinessException` |
| Phí giao          | Lưu ở `deliveries.delivery_fee`, **chưa** cộng vào `payments` (M4 cần bổ sung nếu muốn thu phí) |
| Thanh toán        | Vẫn do M4 (`SERVED → PAID`); `DELIVERED` không tự thanh toán |

## Đường dẫn

| Method | URL                                   | Việc |
|--------|---------------------------------------|------|
| GET    | `/delivery?status=`                   | Danh sách, lọc theo trạng thái |
| GET/POST | `/delivery/new`, `/delivery`        | Form + tạo đơn giao |
| GET    | `/delivery/{id}`                      | Chi tiết + nút thao tác |
| POST   | `/delivery/{id}/assign` (`shipperId`) | Phân công / đổi shipper |
| POST   | `/delivery/{id}/pickup`, `/complete`, `/fail` (`reason`), `/cancel` | Chuyển trạng thái |
| POST   | `/delivery/{id}/order/start`, `/order/ready` | Đẩy đơn qua bếp (tạm, cho tới khi có `/kitchen`) |
| GET/POST | `/delivery/shippers`, `/{id}/update`, `/{id}/deactivate`, `/{id}/activate` | Quản lý shipper |

## Việc cần làm ở Foundation/M5 để chạy đầy đủ

- Thêm `Feature.DELIVERY("/delivery")` để `SecurityConfig` sinh quy tắc `DELIVERY_VIEW`/`DELIVERY_EDIT` (gợi ý: Admin, Cashier EDIT; Waiter VIEW).
- Controller hiện lấy người dùng bằng `Principal.getName()` rồi tra `staff.id`; khi có `StaffUserDetails` thì đổi sang `@AuthenticationPrincipal` và bỏ `findStaffIdByUsername`.
- Nếu có `GlobalExceptionHandler` thì các `try/catch BusinessException` trong controller có thể bỏ; đưa `BusinessException` về `common/` rồi sửa import.
- Dùng `templates/layout/base.html` thay cho khung HTML riêng trong 4 template.

## Cấu hình (`application.properties`)

`store.delivery.default-fee=15000`, `store.delivery.max-active-per-shipper=3`, `store.delivery.max-attempts=3`.

## Test

`DeliveryStatusTest` (bảng chuyển trạng thái), `DeliveryServiceTest` (nghiệp vụ, DAO giả trong bộ nhớ, `OrderServiceImpl` thật), `OrderStatusTest`.
