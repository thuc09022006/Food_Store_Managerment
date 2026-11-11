package com.foodstore.menu;



import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository 
public class JdbcCategoryDao implements CategoryDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Category> rowMapper = (rs, row) -> new Category(
        rs.getLong("id"),
        rs.getString("name")
    );

    public JdbcCategoryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override 
    public List<Category> findAll() {
        String sql = """
                SELECT id, name
                FROM categories
                ORDER BY name
                """;
        
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override 
    public Optional<Category> findById(long id) {
        String sql = """
                SELECT id, name
                FROM categories
                WHERE id = ?
                """;

        List<Category> result = jdbcTemplate.query(sql, rowMapper, id);

        return result.stream().findFirst();
    }

    @Override 
    public long save(Category category) {
        String sql = """
                INSERT INTO categories (name)
                VALUES (?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, category.getName());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if(key == null) {
            throw new IllegalArgumentException("Không lấy được Id danh mục vừa tạo");
        }

        return key.longValue();
    }

    @Override 
    public void update(Category category) {
        String sql = """
                UPDATE categories
                SET name = ?
                WHERE id = ?
                """;

        jdbcTemplate.update(sql, category.getName(), category.getId());
    }

    @Override 
    public void delete(long id) {
        String sql = "DELETE FROM categories WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override 
    public boolean existsProductIn(long categoryId) {
        String sql = """
                SELECT COUNT(*)
                FROM products
                WHERE category_id = ?
                """;
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, categoryId);

        return count != null && count >0;
    }

    @Override 
    public boolean existsByName(String name) {
         String sql = """
                SELECT COUNT(*)
                FROM categories
                WHERE name = ?
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, name.trim());

        return count != null && count > 0;
    }
}
