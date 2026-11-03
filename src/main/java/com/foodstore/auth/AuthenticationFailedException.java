package com.foodstore.auth;

/** Đăng nhập thất bại vì lý do người dùng có thể sửa được (sai thông tin, tài khoản bị khóa). */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
