# Phase 3: Integration & Demo Prep 

 Mục tiêu: luồng thanh toán xuyên 4 module chạy đúng, rollback và tranh chấp đồng thời đã kiểm chứng thật, phân quyền đúng ở server, mọi mục Success Criteria của `SPEC.md` được đánh dấu.

## Tasks

- Task: Luồng chính tạo đơn → chế biến → phục vụ → thanh toán qua trình duyệt

  - Acceptance: với đơn ăn tại chỗ và mang đi, có/không voucher, có/không khách; sau thanh toán: đơn `PAID`, đúng 1 dòng `payments` với số tiền khớp tính tay theo SPEC D1, kho trừ đúng (kể cả đơn có combo), điểm khách tăng, bàn về `FREE`
  - Verify: chạy tay + đối chiếu SQL
- Task: Kiểm rollback thật trên MySQL thật (Mockito không kiểm được)

  - Acceptance: cả hai kịch bản trong bảng "Kiểm rollback" đều cho kết quả "không có gì thay đổi"
  - Verify: chạy tay trên nhánh thử nghiệm (không merge code gây lỗi)
- Task: Kiểm tranh chấp đồng thời bằng hai trình duyệt

  - Acceptance: các kịch bản trong bảng "Kiểm đồng thời" đạt
  - Verify: chạy tay với hai trình duyệt/hai cửa sổ ẩn danh, hai tài khoản
- Task: Kiểm phân quyền ở server — gõ URL trực tiếp

  - Acceptance: ma trận "Kiểm quyền" đạt: vai trò không có quyền nhận `403` (hoặc chuyển `/login` nếu chưa đăng nhập), kể cả khi menu không hiện mục đó
  - Verify: chạy tay + `mvn test`
- Task: Kiểm bảo mật form và hiển thị

  - Acceptance: mọi form POST có CSRF token (POST thiếu token bị từ chối); nhập `<script>alert(1)</script>` vào tên món/ghi chú/tên khách thì hiển thị nguyên văn, không chạy script; lỗi bất ngờ hiện trang lỗi không có stack trace; F5 sau POST không gửi lại form
  - Verify: chạy tay
- Task: Kiểm quy tắc chống thanh toán sai

  - Acceptance: thanh toán lần hai cùng đơn bị từ chối; đơn chưa `SERVED` bị từ chối; voucher sai/hết hạn/hết lượt bị từ chối kèm lý do; voucher `FIXED` lớn hơn subtotal không làm `total` âm; thiếu nguyên liệu vẫn thanh toán được, tồn về 0 và trang cảnh báo hiện; sửa số tiền trong trường ẩn của form (nếu có) không ảnh hưởng số tiền thật
  - Verify: chạy tay + đối chiếu SQL
- Task: Kiểm máy trống (clone mới)

  - Acceptance: máy chưa từng có dự án, làm đúng `README.md` (cả PowerShell) là chạy được `mvn spring-boot:run`, mở http://localhost:8080 và đăng nhập bằng tài khoản seed
  - Verify: chạy tay trên máy/VM sạch
- Task: Hoàn thiện sơ đồ

  - Acceptance: `docs/er-diagram.md` khớp `schema.sql`; thêm `docs/class-diagram.md` (Mermaid `classDiagram`) cho các hierarchy chính: `Product`, `Staff`, `DiscountStrategy`, `OrderStatus`, `LowStockEvent`
  - Verify: xem trên GitHub
- Task: Duyệt Success Criteria của `SPEC.md` và tập demo

  - Acceptance: từng mục có bằng chứng (test, ảnh chụp, truy vấn); có kịch bản demo theo vai trò; đã đổi mật khẩu seed nếu demo ra ngoài nhóm
  - Verify: cả nhóm họp duyệt

## Kiểm rollback

Chuẩn bị: nạp lại `schema.sql` + `data.sql` trên DB thử. Chọn một đơn `SERVED` có khách, dùng voucher `GIAM10`. Ghi lại trước: trạng thái đơn, số dòng `payments`, `vouchers.used_count`, tồn kho các nguyên liệu của đơn, điểm khách, trạng thái bàn.

| Kịch bản | Cách gây lỗi (chỉ trên nhánh thử)                                                                                       | Kết quả mong đợi sau khi bấm Thanh toán                                                                                                               |
|----------|--------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| A        | Tạm cho `CustomerServiceImpl.addPoints` ném `new IllegalStateException("test")` (bước gần cuối)                          | Trang báo lỗi; đơn vẫn `SERVED`; **không** có dòng `payments` mới; `used_count`, tồn kho, điểm khách không đổi; bàn vẫn `OCCUPIED`                      |
| B        | Tạm cho `InventoryServiceImpl.deductForOrder` ném một **exception checked** (khai báo `throws Exception`)               | Kết quả như A. Nếu `payments` vẫn có dòng mới thì `rollbackFor` đang thiếu hoặc sai                                                                    |

Xong thì **xóa** code gây lỗi và ghi kết quả vào "Evidence".

## Kiểm đồng thời

Dùng hai cửa sổ trình duyệt khác nhau (một cửa sổ thường, một ẩn danh) đăng nhập hai tài khoản.

| Kịch bản                                    | Cách làm                                                                                           | Kết quả mong đợi                                                                                   |
|---------------------------------------------|----------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| Hai người tạo đơn cho cùng một bàn          | Cả hai mở form tạo đơn, chọn cùng bàn `FREE`, bấm Lưu lần lượt                                      | Chỉ một đơn được tạo; người thứ hai nhận thông báo lỗi nghiệp vụ; bàn `OCCUPIED` một lần           |
| Hai thu ngân thanh toán cùng một đơn        | Cả hai mở trang thanh toán cùng đơn `SERVED`, bấm Thanh toán lần lượt                              | Chỉ một `payments` được tạo; người sau nhận lỗi; kho trừ **một** lần; điểm cộng **một** lần         |
| Voucher còn một lượt, hai đơn cùng dùng     | Đặt `usage_limit = used_count + 1` cho một voucher; hai thu ngân thanh toán hai đơn khác nhau cùng mã | Chỉ một thanh toán thành công; người còn lại bị từ chối vì hết lượt; `used_count` không vượt giới hạn |
| Bếp bấm "Xong" trên đơn đã bị hủy           | Một người hủy đơn `NEW`, người kia bấm "Bắt đầu làm" trên trang cũ                                  | Thao tác thứ hai bị từ chối ("Đơn đã được cập nhật bởi người khác"), trạng thái không đổi            |

## Kiểm quyền (gõ URL trực tiếp)

Đăng nhập từng vai trò rồi gõ thẳng URL (không bấm menu). `✔` = vào được, `403` = bị chặn. Với `403`, thử cả gửi POST (ví dụ bằng công cụ như `curl` hoặc extension) để chắc chắn server chặn.

| URL                  | Admin | Cashier | Waiter | Chef |
|----------------------|:-----:|:-------:|:------:|:----:|
| GET `/menu`          | ✔     | 403     | ✔      | 403  |
| POST `/menu`         | ✔     | 403     | 403    | 403  |
| GET `/tables`        | ✔     | 403     | ✔      | 403  |
| GET `/orders`        | ✔     | ✔       | ✔      | 403  |
| POST `/orders`       | ✔     | ✔       | ✔      | 403  |
| GET `/kitchen`       | ✔     | 403     | 403    | ✔    |
| POST `/kitchen/...`  | ✔     | 403     | 403    | ✔    |
| GET `/inventory`     | ✔     | 403     | 403    | ✔    |
| POST `/inventory/...`| ✔     | 403     | 403    | 403  |
| GET `/payments`      | ✔     | ✔       | 403    | 403  |
| POST `/payments/...` | ✔     | ✔       | 403    | 403  |
| GET `/customers`     | ✔     | ✔       | 403    | 403  |
| GET `/staff`         | ✔     | 403     | 403    | 403  |
| GET `/reports`       | ✔     | 403     | 403    | 403  |

Bảng này suy ra từ phân quyền trong `SPEC.md`; nếu nhóm đổi phân quyền thì sửa bảng này.

## Kịch bản demo theo vai trò

1. **Admin:** xem Báo cáo (doanh thu, món bán chạy), quản lý nhân viên.
2. **Waiter:** xem Thực đơn (không có nút sửa), tạo đơn tại bàn, gắn khách thành viên.
3. **Chef:** thấy đơn ở Bếp (tự làm mới), bấm Xong; xem Kho (không sửa được), thấy cảnh báo tồn thấp.
4. **Cashier:** thanh toán đơn `SERVED` với voucher `GIAM10`, in hóa đơn; thử `HETHAN` để thấy bị từ chối; xem điểm khách tăng.
5. **Bảo mật (tùy chọn, ghi điểm):** gõ URL trái quyền để cho thấy `403`.

## Evidence (điền khi kiểm xong)

| Mục                               | Ngày | Người kiểm | Kết quả |
|-----------------------------------|------|------------|---------|
| Rollback kịch bản A               |      |            |         |
| Rollback kịch bản B (checked)     |      |            |         |
| Đồng thời: hai người cùng một bàn |      |            |         |
| Đồng thời: thanh toán cùng một đơn|      |            |         |
| Đồng thời: voucher một lượt       |      |            |         |
| Ma trận quyền (gõ URL)            |      |            |         |
| CSRF / XSS                        |      |            |         |
| Clone máy trống (README)          |      |            |         |

## Stretch goals (không bắt buộc)

Tách bill, trừ kho FIFO theo lô, xuất CSV, biểu đồ (Chart.js), AJAX tìm kiếm, đặt bàn trước, Testcontainers cho DAO test, kiểm quyền ở tầng service, đẩy cảnh báo tồn thấp tới trình duyệt (SSE/WebSocket).
