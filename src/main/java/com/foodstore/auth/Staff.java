package com.foodstore.auth;

import java.util.Set;

/**
 * Lớp cha trừu tượng của nhân viên đăng nhập. Mỗi lớp con quyết định mình được vào màn hình nào
 * (đa hình): MainFrame dựng menu từ {@link #allowedScreens()} mà không cần if/else theo vai trò.
 * Không lưu mật khẩu/hash trong đối tượng này.
 */
public abstract class Staff {

    private final long id;
    private final String username;
    private final String fullName;

    protected Staff(long id, String username, String fullName) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username không được để trống");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên không được để trống");
        }
        this.id = id;
        this.username = username;
        this.fullName = fullName;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public abstract StaffRole getRole();

    /** Các màn hình vai trò này được vào, theo thứ tự khai báo của {@link Screen}. */
    public abstract Set<Screen> allowedScreens();

    public boolean canAccess(Screen screen) {
        return allowedScreens().contains(screen);
    }
}
