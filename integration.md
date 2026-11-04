# Phase 3: Integration & Demo Prep (cả nhóm)

Bắt đầu ngay khi 3-4 module sẵn sàng, không đợi đủ cả 5. Mục tiêu: luồng thanh toán xuyên 4 module chạy đúng, rollback đã được kiểm chứng thật, mọi mục Success Criteria của `SPEC.md` được đánh dấu.

## Tasks

- Task: Luồng chính tạo đơn → chế biến → phục vụ → thanh toán qua UI

  - Acceptance: với cả đơn ăn tại chỗ và mang đi, có và không có voucher, có và không có khách; sau thanh toán: đơn `PAID`, có 1 dòng `payments` với số tiền khớp tính tay theo SPEC D1, kho trừ đúng (kể cả đơn có combo), điểm khách tăng, bàn về `FREE`
  - Verify: chạy tay + đối chiếu bằng truy vấn SQL
- Task: Kiểm rollback thật trên MySQL thật (Mockito không kiểm được)

  - Acceptance: cả hai kịch bản dưới đây cho kết quả "không có gì thay đổi" (xem bảng kiểm bên dưới)
  - Verify: chạy tay trên nhánh thử nghiệm (không merge code gây lỗi)
- Task: Kiểm các quy tắc chống thanh toán sai

  - Acceptance: thanh toán lần hai cho cùng đơn bị từ chối; thanh toán đơn chưa `SERVED` bị từ chối; mã voucher sai/hết hạn/hết lượt bị từ chối kèm lý do; voucher `FIXED` lớn hơn subtotal không làm `total` âm; kho thiếu nguyên liệu vẫn thanh toán được, tồn về 0 và có cảnh báo
  - Verify: chạy tay + đối chiếu SQL
- Task: Kiểm giao diện không treo và phân quyền

  - Acceptance: thanh toán và báo cáo không làm đơ cửa sổ; đăng nhập 4 vai trò thấy đúng menu; màn hình `VIEW` (Waiter-MENU, Chef-INVENTORY) không sửa được
  - Verify: chạy tay
- Task: Kiểm máy trống (clone mới)

  - Acceptance: máy chưa từng có dự án, làm đúng `README.md` (cả PowerShell) là chạy được `mvn spring-boot:run` và đăng nhập bằng tài khoản seed
  - Verify: chạy tay trên máy/VM sạch
- Task: Hoàn thiện tài liệu sơ đồ

  - Acceptance: `docs/er-diagram.md` khớp `schema.sql`; thêm `docs/class-diagram.md` (Mermaid `classDiagram`) cho các hierarchy chính: `Product`, `Staff`, `DiscountStrategy`, `OrderStatus`, `LowStockEvent`
  - Verify: xem trên GitHub
- Task: Duyệt Success Criteria của `SPEC.md` và tập demo

  - Acceptance: từng mục Success Criteria được đánh dấu kèm bằng chứng (test, ảnh chụp hoặc truy vấn); có kịch bản demo theo vai trò (bên dưới); đã đổi mật khẩu seed nếu demo ra ngoài nhóm
  - Verify: cả nhóm họp duyệt

## Kiểm rollback thủ công

Chuẩn bị: nạp lại `schema.sql` + `data.sql` trên DB thử. Chọn một đơn `SERVED` có voucher `GIAM10` và có khách. Ghi lại trước khi thử: trạng thái đơn, số dòng `payments`, `vouchers.used_count`, tồn kho các nguyên liệu của đơn, điểm khách, trạng thái bàn.

| Kịch bản | Cách gây lỗi (chỉ trên nhánh thử)                                                                                  | Kết quả mong đợi sau khi bấm Thanh toán                                                                                       |
|----------|--------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| A        | Tạm cho `CustomerServiceImpl.addPoints` ném `new IllegalStateException("test")` (bước gần cuối)                    | Hộp thoại báo lỗi; đơn vẫn `SERVED`; **không** có dòng `payments` mới; `used_count` không đổi; tồn kho không đổi; bàn vẫn `OCCUPIED`; điểm không đổi |
| B        | Tạm cho `InventoryServiceImpl.deductForOrder` ném một **exception checked** (ví dụ `throw new Exception("test")` qua `sneaky` hoặc khai báo `throws Exception`) | Kết quả như A. Nếu `payments` vẫn có dòng mới thì `rollbackFor` đang thiếu hoặc sai                                              |

Sau khi kiểm xong, **xóa** đoạn code gây lỗi và ghi kết quả (ngày, người kiểm, đạt/không) vào phần "Evidence" dưới đây.

## Kịch bản demo theo vai trò

1. **Admin:** xem Báo cáo (doanh thu, món bán chạy), quản lý nhân viên.
2. **Waiter:** xem Thực đơn (không sửa được), tạo đơn tại bàn, gắn khách thành viên.
3. **Chef:** thấy đơn ở Bếp, bấm Xong; xem Kho (không sửa được), thấy cảnh báo tồn thấp.
4. **Cashier:** thanh toán đơn `SERVED` với voucher `GIAM10`, xem hóa đơn; thử `HETHAN` để thấy bị từ chối; xem điểm khách tăng.

## Evidence (điền khi kiểm xong)

| Mục                                | Ngày | Người kiểm | Kết quả |
|------------------------------------|------|------------|---------|
| Rollback kịch bản A                |      |            |         |
| Rollback kịch bản B (checked)      |      |            |         |
| Clone máy trống (README)           |      |            |         |
| Phân quyền 4 vai trò               |      |            |         |

## Stretch goals (không bắt buộc)

Giao hàng, tách bill, trừ kho FIFO theo lô, xuất CSV, `FlatLaf`, đặt bàn trước, Testcontainers cho DAO test, kiểm quyền ở tầng service.
