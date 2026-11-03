package com.foodstore.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private StaffDao staffDao;

    // BCrypt thật (cost 4 cho test chạy nhanh), không mock: kiểm đúng việc so khớp hash
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private AuthService authService;
    private String hash;

    @BeforeEach
    void setUp() {
        authService = new AuthService(staffDao, encoder);
        hash = encoder.encode("Admin@123");
    }

    @Test
    void correctPasswordReturnsStaffOfTheRightSubclass() {
        when(staffDao.findByUsername("admin"))
                .thenReturn(Optional.of(new StaffAccount(new Admin(1, "admin", "Quản Lý"), hash, true)));

        Staff staff = authService.login("admin", "Admin@123");

        assertInstanceOf(Admin.class, staff);
        assertEquals("admin", staff.getUsername());
    }

    @Test
    void usernameIsTrimmedBeforeLookup() {
        when(staffDao.findByUsername("admin"))
                .thenReturn(Optional.of(new StaffAccount(new Admin(1, "admin", "Quản Lý"), hash, true)));

        assertInstanceOf(Admin.class, authService.login("  admin  ", "Admin@123"));
    }

    @Test
    void wrongPasswordIsRejected() {
        when(staffDao.findByUsername("admin"))
                .thenReturn(Optional.of(new StaffAccount(new Admin(1, "admin", "Quản Lý"), hash, true)));

        AuthenticationFailedException e =
                assertThrows(AuthenticationFailedException.class, () -> authService.login("admin", "sai"));
        assertEquals(AuthService.BAD_CREDENTIALS, e.getMessage());
    }

    @Test
    void unknownUserGetsTheSameMessageAsWrongPassword() {
        when(staffDao.findByUsername("ghost")).thenReturn(Optional.empty());

        AuthenticationFailedException e =
                assertThrows(AuthenticationFailedException.class, () -> authService.login("ghost", "x"));
        assertEquals(AuthService.BAD_CREDENTIALS, e.getMessage());
    }

    @Test
    void lockedAccountIsRejectedEvenWithCorrectPassword() {
        when(staffDao.findByUsername("waiter"))
                .thenReturn(Optional.of(new StaffAccount(new Waiter(3, "waiter", "Phục Vụ"), hash, false)));

        AuthenticationFailedException e =
                assertThrows(AuthenticationFailedException.class, () -> authService.login("waiter", "Admin@123"));
        assertEquals(AuthService.ACCOUNT_LOCKED, e.getMessage());
    }

    @Test
    void lockedAccountWithWrongPasswordDoesNotRevealThatItExists() {
        when(staffDao.findByUsername("waiter"))
                .thenReturn(Optional.of(new StaffAccount(new Waiter(3, "waiter", "Phục Vụ"), hash, false)));

        AuthenticationFailedException e =
                assertThrows(AuthenticationFailedException.class, () -> authService.login("waiter", "sai"));
        assertEquals(AuthService.BAD_CREDENTIALS, e.getMessage());
    }

    @Test
    void blankInputNeverReachesTheDatabase() {
        assertThrows(AuthenticationFailedException.class, () -> authService.login("", "x"));
        assertThrows(AuthenticationFailedException.class, () -> authService.login("admin", ""));
        assertThrows(AuthenticationFailedException.class, () -> authService.login(null, null));
        verifyNoInteractions(staffDao);
    }
}
