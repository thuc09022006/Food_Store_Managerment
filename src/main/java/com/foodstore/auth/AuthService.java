package com.foodstore.auth;

import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    static final String BAD_CREDENTIALS = "Sai tên đăng nhập hoặc mật khẩu";
    static final String ACCOUNT_LOCKED = "Tài khoản đã bị khóa, vui lòng liên hệ quản lý";

    private final StaffDao staffDao;
    private final PasswordEncoder passwordEncoder;

    public AuthService(StaffDao staffDao, PasswordEncoder passwordEncoder) {
        this.staffDao = staffDao;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * @return nhân viên đã xác thực (đúng lớp con theo vai trò)
     * @throws AuthenticationFailedException nếu thiếu thông tin, sai thông tin, hoặc tài khoản bị khóa
     */
    public Staff login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new AuthenticationFailedException("Vui lòng nhập tên đăng nhập và mật khẩu");
        }
        Optional<StaffAccount> found = staffDao.findByUsername(username.trim());
        // Cùng một thông báo cho "không có user" và "sai mật khẩu" để không lộ user nào tồn tại.
        if (found.isEmpty() || !passwordEncoder.matches(password, found.get().passwordHash())) {
            throw new AuthenticationFailedException(BAD_CREDENTIALS);
        }
        // Chỉ báo "bị khóa" SAU KHI mật khẩu đúng.
        if (!found.get().active()) {
            throw new AuthenticationFailedException(ACCOUNT_LOCKED);
        }
        return found.get().staff();
    }
}
