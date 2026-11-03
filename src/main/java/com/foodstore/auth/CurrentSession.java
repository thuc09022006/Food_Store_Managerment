package com.foodstore.auth;

import org.springframework.stereotype.Component;

/** Giữ nhân viên đang đăng nhập trong suốt vòng đời ứng dụng (một máy, một người dùng). */
@Component
public class CurrentSession {

    private volatile Staff current;

    public void login(Staff staff) {
        this.current = staff;
    }

    public void logout() {
        this.current = null;
    }

    public boolean isLoggedIn() {
        return current != null;
    }

    /** @throws IllegalStateException nếu chưa đăng nhập */
    public Staff get() {
        Staff staff = current;
        if (staff == null) {
            throw new IllegalStateException("Chưa đăng nhập");
        }
        return staff;
    }
}
