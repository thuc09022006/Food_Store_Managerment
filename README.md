# FoodStore-Management-OOP

Hệ thống quản lý cửa hàng đồ ăn — đồ án OOP. Ứng dụng web: **Spring Boot + Spring Web MVC + Thymeleaf + Spring Security**, MySQL, SQL viết tay qua `JdbcTemplate`.

Tài liệu thiết kế: [`SPEC.md`](SPEC.md) · Kế hoạch: [`plan.md`](plan.md) · Phase 0: [`foundation.md`](foundation.md) · Sơ đồ: [`docs/er-diagram.md`](docs/er-diagram.md)

| Module                            | Tài liệu                                     |
|-----------------------------------|----------------------------------------------|
| M1 Menu                           | [`m1-menu.md`](m1-menu.md)                   |
| M2 Orders & Tables                | [`m2-orders.md`](m2-orders.md)               |
| M3 Inventory & Suppliers          | [`m3-inventory.md`](m3-inventory.md)         |
| M4 Payments, Vouchers & Customers | [`m4-payments.md`](m4-payments.md)           |
| M5 Staff, Auth & Reports          | [`m5-staff-reports.md`](m5-staff-reports.md) |
| M6 Delivery (giao hàng)           | [`m6-delivery.md`](m6-delivery.md)           |
| Tích hợp & demo                   | [`integration.md`](integration.md)           |

## Yêu cầu

- JDK 21
- Maven 3.9+
- MySQL 8.0.16+ (cần bản này trở lên để ràng buộc `CHECK` có hiệu lực)
- Trình duyệt hiện đại

## Cài đặt (từ máy trống)

1. Clone repo.
2. Tạo database và nạp dữ liệu. Dùng `-e "source ..."` vì toán tử `<` **không chạy trong PowerShell**; cách dưới chạy được ở bash, cmd và PowerShell. Chạy tại thư mục gốc repo:

   ```
   mysql -u root -p -e "CREATE DATABASE foodstore_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
   mysql -u root -p --default-character-set=utf8mb4 foodstore_db -e "source src/main/resources/schema.sql"
   mysql -u root -p --default-character-set=utf8mb4 foodstore_db -e "source src/main/resources/data.sql"
   ```

   `--default-character-set=utf8mb4` giữ đúng tiếng Việt trong dữ liệu seed.

3. (Khuyến nghị) Tạo user riêng thay vì dùng `root`:

   ```
   CREATE USER 'foodstore'@'localhost' IDENTIFIED BY '<mật khẩu của bạn>';
   GRANT ALL PRIVILEGES ON foodstore_db.* TO 'foodstore'@'localhost';
   ```

4. Đặt thông tin kết nối bằng biến môi trường (không commit mật khẩu):

   bash / zsh:

   ```
   export DB_URL="jdbc:mysql://localhost:3306/foodstore_db?useUnicode=true&characterEncoding=UTF-8"
   export DB_USERNAME="foodstore"
   export DB_PASSWORD="<mật khẩu của bạn>"
   ```

   PowerShell:

   ```
   $env:DB_URL="jdbc:mysql://localhost:3306/foodstore_db?useUnicode=true&characterEncoding=UTF-8"
   $env:DB_USERNAME="foodstore"
   $env:DB_PASSWORD="<mật khẩu của bạn>"
   ```

   cmd: `set DB_URL=...` (không dùng dấu nháy).

5. Chạy:

   ```
   mvn spring-boot:run
   ```

6. Mở trình duyệt tại **http://localhost:8080** và đăng nhập bằng một tài khoản seed.

## Tài khoản seed

`data.sql` tạo sẵn 4 tài khoản (mật khẩu trong DB là hash BCrypt). **Chỉ dùng khi phát triển** — repo công khai nên các mật khẩu dưới đây ai cũng đọc được; đổi mật khẩu trước khi demo ra ngoài nhóm.

| Vai trò | Username  | Mật khẩu      | Dùng được                                           |
|---------|-----------|---------------|-----------------------------------------------------|
| Admin   | `admin`   | `Admin@123`   | Tất cả                                              |
| Cashier | `cashier` | `Cashier@123` | Đơn hàng, Thanh toán, Khách hàng & Voucher          |
| Waiter  | `waiter`  | `Waiter@123`  | Thực đơn (chỉ xem), Bàn, Đơn hàng                   |
| Chef    | `chef`    | `Chef@123`    | Bếp, Kho (chỉ xem)                                  |

`data.sql` cũng có sẵn: 8 sản phẩm (có 1 combo), 5 bàn, 12 nguyên liệu + công thức (Cam đang dưới mức tối thiểu, Tôm sát mức tối thiểu để thử cảnh báo tồn thấp), 2 khách hàng, 2 voucher (`GIAM10` còn hạn, `HETHAN` đã hết hạn), 1 phiếu nhập có lô sắp hết hạn, và 2 đơn đã thanh toán để báo cáo có dữ liệu.

Quyền "chỉ xem" được **server kiểm tra** (gửi request thủ công cũng bị trả `403`), không chỉ ẩn nút trên giao diện.

## Trạng thái hiện tại

Phase 0 đã có bản đầu: `pom.xml`, `schema.sql`, `data.sql`, cấu hình, đăng nhập, layout chung, interface Service. Các module M1–M5 do từng owner cài đặt theo file `m*.md` tương ứng.

## Lệnh thường dùng

```
mvn clean package    # build
mvn test             # chạy unit test + test controller
mvn spring-boot:run  # chạy app tại http://localhost:8080
```

## Quy ước làm việc

Xem mục Boundaries trong `SPEC.md`: controller mỏng, nghiệp vụ ở service, validate ở service, SQL dùng tham số `?`, không tắt CSRF, không commit mật khẩu, hỏi nhóm trước khi sửa `schema.sql`, `SecurityConfig`, `layout/base.html` hoặc chữ ký interface Service đã chốt. Chạy `mvn test` trước khi tạo pull request.

## Lỗi thường gặp

| Triệu chứng                                          | Nguyên nhân / cách xử lý                                                                                       |
|------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| `The '<' operator is reserved` (PowerShell)          | Dùng lệnh `-e "source ..."` ở trên, không dùng `<`                                                             |
| `Port 8080 was already in use`                       | Tắt tiến trình đang dùng cổng, hoặc chạy với `--server.port=8081`                                              |
| Gửi form báo `403 Forbidden`                         | Form thiếu CSRF token: dùng `th:action` (Thymeleaf tự thêm token), không dùng `action="..."` thuần             |
| Đăng nhập xong bấm một trang bị `403`                | Vai trò không có quyền tính năng đó (đúng thiết kế) — kiểm bảng phân quyền trong `SPEC.md`                     |
| `TemplateInputException: Error resolving template`   | Tên view trả về từ controller không khớp đường dẫn file trong `src/main/resources/templates/`                  |
| Trang trắng với lỗi `Whitelabel Error Page`          | Xem log ở terminal; lỗi nghiệp vụ phải đi qua `GlobalExceptionHandler`                                         |
| `Access denied for user`                             | Sai `DB_USERNAME`/`DB_PASSWORD`, hoặc chưa cấp quyền trên `foodstore_db`                                       |
| `Public Key Retrieval is not allowed`                | Chỉ khi dev local: thêm `&allowPublicKeyRetrieval=true` vào `DB_URL`. Không dùng cấu hình này ngoài máy dev    |
| Chữ tiếng Việt thành `???` hoặc ký tự lạ             | Nạp lại dữ liệu với `--default-character-set=utf8mb4`; `DB_URL` có `characterEncoding=UTF-8`                   |
| `Unknown database 'foodstore_db'`                    | Chưa chạy bước tạo database                                                                                    |
| Sửa template mà trình duyệt không đổi                | Đặt `spring.thymeleaf.cache=false` khi dev (đừng dùng khi triển khai)                                          |
