package com.foodstore.menu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** DAO mẫu của Foundation: SQL viết tay + JdbcTemplate. Các DAO khác bám theo mẫu này. */
@Repository
public class JdbcCategoryDao implements CategoryDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCategoryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Category> findAll() {
        return jdbcTemplate.query("SELECT id, name FROM categories ORDER BY name", this::mapRow);
    }

    @Override
    public Optional<Category> findById(long id) {
        return jdbcTemplate.query("SELECT id, name FROM categories WHERE id = ?", this::mapRow, id)
                .stream().findFirst();
    }

    @Override
    public Category save(Category category) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO categories (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, category.name());
            return ps;
        }, keys);
        return new Category(keys.getKey().longValue(), category.name());
    }

    @Override
    public void update(Category category) {
        jdbcTemplate.update("UPDATE categories SET name = ? WHERE id = ?", category.name(), category.id());
    }

    @Override
    public void delete(long id) {
        jdbcTemplate.update("DELETE FROM categories WHERE id = ?", id);
    }

    private Category mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Category(rs.getLong("id"), rs.getString("name"));
    }
}
