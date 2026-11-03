package com.foodstore.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcStaffDao implements StaffDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcStaffDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<StaffAccount> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, full_name, role, is_active "
                + "FROM staff WHERE username = ?";
        return jdbcTemplate.query(sql, this::mapRow, username).stream().findFirst();
    }

    private StaffAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
        long id = rs.getLong("id");
        String username = rs.getString("username");
        String fullName = rs.getString("full_name");
        Staff staff = switch (StaffRole.valueOf(rs.getString("role"))) {
            case ADMIN -> new Admin(id, username, fullName);
            case CASHIER -> new Cashier(id, username, fullName);
            case WAITER -> new Waiter(id, username, fullName);
            case CHEF -> new Chef(id, username, fullName);
        };
        return new StaffAccount(staff, rs.getString("password_hash"), rs.getBoolean("is_active"));
    }
}
