package com.foodstore.auth;

/**
 * Kết quả tra cứu đăng nhập: nhân viên + hash mật khẩu + trạng thái.
 * Hash chỉ tồn tại trong DAO và AuthService, không nằm trong {@link Staff}.
 */
public record StaffAccount(Staff staff, String passwordHash, boolean active) {
}
