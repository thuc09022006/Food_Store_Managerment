# Implementation Plan: Hệ thống Quản lý Cửa hàng Đồ ăn (FoodStore)

Source spec: `SPEC.md`

## Components & Dependencies

```
Foundation + M5 Phase 0 (sequential, 1-2 người, chặn tất cả)
    │
    ├── schema.sql / data.sql (đóng băng sau khi merge)
    ├── Maven skeleton + Spring Boot bootstrap (Swing trên EDT)
    ├── Khai báo interface Service của 5 module (chữ ký, chưa cài đặt)
    ├── Staff abstract + Admin/Cashier/Waiter/Chef + Screen enum
    └── AuthService + StaffDao + LoginFrame (cơ bản, chưa style)
    │
    ▼
Feature modules (song song, mỗi người một module)
    ├── M1 Menu ───────────────┐
    ├── M2 Orders & Tables      │ M2 đọc Product (M1) khi thêm món vào đơn
    ├── M3 Inventory            │ M3 đọc công thức theo Product (M1) và order_items (M2)
    ├── M4 Payments/Customers   │ M4 đọc Order (M2), gọi M3 + M2 khi thanh toán
    └── M5 Staff/Reports (phần Phase 1: StaffService, ReportService)
    │
    ▼
M5 Phase 2: MainFrame (CardLayout) + menu theo vai trò, nối panel của M1-M4
    │
    ▼
Integration & Demo Prep (cả nhóm)
```

**Dependency cứng, tuần tự:** chưa module nào bắt đầu được khi Foundation chưa merge — nó định nghĩa schema, khung package, `Staff`/login và **chữ ký interface Service** mà mọi module phụ thuộc. Giữ Phase 0 ngắn (1-2 người) để 3-4 người còn lại không phải chờ lâu.

**Dependency mềm giữa các module:** M2 cần dữ liệu `products` (M1); M3 cần `products` (M1) và `order_items` (M2); M4 cần `orders` (M2) và gọi `InventoryService` (M3). Mỗi owner làm DAO/service/UI trên **seed data** (`data.sql`) và **interface Service đã chốt** ở Phase 0, không cần chờ module bạn xong, chỉ cần schema đã đóng băng.

> Khác bản Gym: luồng thanh toán của FoodStore chạm 4 module cùng lúc, nên chữ ký interface Service phải chốt từ Phase 0 (không chỉ schema). Đổi chữ ký sau đó đi qua boundary "Hỏi nhóm trước".

## Per-Module Files

Mỗi module là một thư mục dưới `src/main/java/com/foodstore/` (package-by-module, theo `SPEC.md`). Danh sách file và checklist task nằm trong file riêng của từng module:

| Module | File | Thư mục |
|--------|------|---------|
| Foundation (shared/root) | `foundation.md` | — (`pom.xml`, `FoodStoreApplication.java`, `config/`, `src/main/resources/`) |
| M1 — Menu | `m1-menu.md` | `menu/` |
| M2 — Orders & Tables | `m2-orders.md` | `orders/` |
| M3 — Inventory & Suppliers | `m3-inventory.md` | `inventory/` |
| M4 — Payments, Vouchers & Customers | `m4-payments.md` | `payments/` (+ `discount/`), `customers/` |
| M5 — Staff, Auth & Reports | `m5-staff-reports.md` | `auth/`, `reports/` |
| Phase 3 — Integration & Demo Prep | `integration.md` | — (cả nhóm) |

## Implementation Order

1. **Phase 0 — Foundation + M5 Phase 0** (tuần tự). Phải xong và merge trước Phase 1.
2. **Phase 1 — M1–M5** (song song, mỗi người một module). M5 ở Phase 1 làm `StaffService`, `ReportService` và màn hình đăng nhập hoàn chỉnh.
3. **Phase 2 — M5 `MainFrame`** (nối panel + menu theo vai trò): bắt đầu khi M1-M4 đã có panel chạy được, dù chưa hoàn chỉnh.
4. **Phase 3 — Integration & Demo Prep** (cả nhóm): chạy luồng thanh toán xuyên 4 module, kiểm rollback, duyệt Success Criteria, tập demo. Bắt đầu ngay khi 3-4 module sẵn sàng, không đợi đủ cả 5.

## Risks & Mitigations

| Rủi ro | Giải pháp |
|--------|-----------|
| Đổi schema sau khi Phase 1 bắt đầu làm gãy DAO của nhiều người | Đóng băng `schema.sql` cuối Phase 0; mọi thay đổi sau đó phải hỏi nhóm trước khi merge |
| Đổi chữ ký interface Service làm gãy module khác | Chữ ký chốt ở Phase 0 trong `foundation.md`; muốn thêm phương thức thì thêm mới, không đổi/xóa phương thức cũ |
| Conflict khi sửa file dùng chung (`pom.xml`, `FoodStoreApplication.java`, `schema.sql`) | Chỉ owner Foundation sửa sau Phase 0; owner module chỉ thêm file trong package của mình |
| Luồng thanh toán lỗi giữa chừng làm kho/đơn/điểm lệch nhau | `CheckoutService` là một `@Transactional`; Phase 3 có bước kiểm rollback thủ công trên MySQL thật (Mockito không kiểm được rollback) |
| M5 chậm làm kẹt MainFrame/Integration | Owner M5 làm login + MainFrame khung rỗng sớm ở Phase 1, Phase 2 chỉ còn nối panel |
| Lặp code DAO giữa 5 người (`findById`/`findAll`/`save`) | Chấp nhận, vì mục tiêu là luyện JDBC/SQL viết tay; không tạo DAO cơ sở chung |
| Có người chưa quen Swing/Spring | Foundation dựng sẵn một panel mẫu rỗng + một DAO mẫu (`JdbcCategoryDao`) làm tham chiếu cho cả nhóm |
| Hai thu ngân thao tác cùng một đơn/bàn | Ngoài phạm vi; service chặn tạo đơn thứ hai khi bàn đang `OCCUPIED`, chấp nhận race condition vì app demo một máy |

## Parallel vs. Sequential

- **Tuần tự, chặn:** Phase 0.
- **Song song:** M1–M5 Phase 1 — 5 luồng độc lập sau khi Phase 0 merge.
- **Song song một phần:** M5 Phase 2 (`MainFrame`) chờ panel của M1-M4.
- **Tuần tự, cả nhóm:** Phase 3.

## Verification Checkpoints

- **Checkpoint A (cuối Phase 0):** `mvn clean package` thành công; app mở một cửa sổ Swing; `schema.sql` + `data.sql` chạy sạch trên MySQL mới; đăng nhập bằng tài khoản seed hoạt động; cả 5 người pull về và build được trên máy mình.
- **Checkpoint B (giữa Phase 1):** mỗi owner demo panel của mình chạy độc lập trên MySQL thật (nhánh riêng, máy riêng); mỗi module có ít nhất một JUnit test của service đạt.
- **Checkpoint C (xong Phase 2):** đăng nhập với từng trong 4 vai trò thấy menu đúng; mọi panel truy cập được từ ít nhất một vai trò; luồng tạo đơn → thanh toán chạy được qua UI.
- **Checkpoint D (Phase 3 / cuối):** mọi mục trong Success Criteria của `SPEC.md` được đánh dấu; clone mới + MySQL mới + `mvn spring-boot:run` chạy end-to-end trên máy chưa từng có dự án; rollback thanh toán đã kiểm thủ công.

Xem file riêng từng module (`foundation.md`, `m1-menu.md` … `m5-staff-reports.md`, `integration.md`) để biết danh sách task chi tiết.
