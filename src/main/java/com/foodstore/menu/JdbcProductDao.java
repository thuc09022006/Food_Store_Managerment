package com.foodstore.menu;



import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository 
public class JdbcProductDao implements ProductDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Product> productRowMapper = (rs, rowNum) ->ProductFactory.create(
                    rs.getString("product_type"),
                    rs.getLong("id"),
                    rs.getLong("category_id"),
                    rs.getString("name"),
                    rs.getBigDecimal("price"),
                    rs.getString("description"),
                    rs.getBoolean("active")
            );

    public JdbcProductDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String PRODUCT_COLUMNS = """
            id, category_id, name, price, description, active, product_type
            """;

    private void loadComboComponents(List<Product> products) {
        List<Long> comboIds = products.stream()
                                .filter(product -> product instanceof Combo)
                                .map(product -> product.getId()).toList();

        if(comboIds.isEmpty()) {
            return;
        }

        Map<Long, List<ComboComponent>>componentsByCombo = findComponentsByComboIds(comboIds);
    

        for(Product product : products) {
            if(product instanceof Combo combo) {
                combo.setComponents(componentsByCombo.getOrDefault(combo.getId(),Collections.emptyList()));
            }
        }
    }

    private String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private  boolean exists(String sql, long id) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);

        return count != null && count > 0;
    }

    @Override 
    public List<Product> findAll() {
        String sql = """
                SELECT %s
                FROM products
                ORDER BY name
                """.formatted(PRODUCT_COLUMNS);

        List<Product> products = jdbcTemplate.query(sql, productRowMapper);
        loadComboComponents(products);

        return products;
    }

    @Override 
    public Optional<Product> findById(long id) {
        String sql = """
                SELECT %s
                FROM products
                WHERE id = ?
                """.formatted(PRODUCT_COLUMNS);

        List<Product> products = jdbcTemplate.query(sql, productRowMapper, id);

        if(products.isEmpty()) {
            return Optional.empty();
        }

        Product product = products.get(0);

        if(product instanceof Combo combo) {
            combo.setComponents(findComponents(combo.getId()));
        }

        return Optional.of(product);
    }

    @Override 
    public List<Product> search(String name, Long categoryId, Boolean active) {

        StringBuilder sql = new StringBuilder("""
                SELECT %s
                FROM products
                WHERE 1 = 1
                """.formatted(PRODUCT_COLUMNS));

        List<Object> params = new ArrayList<>();

        if (name != null && !name.trim().isEmpty()) {
            sql.append(" AND name LIKE ? ESCAPE '\\\\' ");

            String escapedName = escapeLike(name.trim());
            params.add("%" + escapedName + "%");
        }

        if(categoryId != null) {
            sql.append(" AND category_id = ? ");
            params.add(categoryId);
        }

        if(active != null) {
            sql.append(" AND active = ? ");
            params.add(active);
        }

        sql.append(" ORDER BY name ");

        List<Product> products = jdbcTemplate.query(sql.toString(), productRowMapper, params.toArray());

        loadComboComponents(products);
        return products;
    }

    @Override 
    public long save(Product product) {
        String sql = """
                INSERT INTO products
                    (category_id, name, price, description, active, product_type)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        PreparedStatementCreator creator = connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setLong(1, product.getCategoryId());
            ps.setString(2, product.getName());
            ps.setBigDecimal(3, product.getPrice());
            ps.setString(4, product.getDescription());
            ps.setBoolean(5, true);
            ps.setString(6, product.getType());

            return ps;
        };

        jdbcTemplate.update(creator, keyHolder);

        Number key = keyHolder.getKey();
        if(key == null) {
            throw new IllegalArgumentException("Không lấy được Id sản phẩm vừa tạo!");
        }

        return key.longValue();
    }

    @Override 
    public void update(Product product) {
        String sql = """
                UPDATE products
                SET category_id = ?,
                    name = ?,
                    price = ?,
                    description = ?
                WHERE id = ?
                """;

        jdbcTemplate.update(sql, product.getCategoryId(), product.getName(), product.getPrice(), product.getDescription(), product.getId());
    }

    @Override 
    public void setActive(long id, boolean active) {
        String sql = """
                UPDATE products
                SET active = ?
                WHERE id = ?
                """;

        jdbcTemplate.update(sql, active, id);
    }

    @Override 
    public void delete(long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override 
    public List<ComboComponent> findComponents(long comboId) {
        String sql = """
                SELECT
                    p.id,
                    p.category_id,
                    p.name,
                    p.price,
                    p.description,
                    p.active,
                    p.product_type,
                    cc.quantity
                FROM combo_components cc
                JOIN products p ON p.id = cc.component_id
                WHERE cc.combo_id = ?
                ORDER BY p.name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Product product = ProductFactory.create(
                    rs.getString("product_type"),
                    rs.getLong("id"),
                    rs.getLong("category_id"),
                    rs.getString("name"),
                    rs.getBigDecimal("price"),
                    rs.getString("description"),
                    rs.getBoolean("active")
            );

            return new ComboComponent(product, rs.getInt("quantity"));
        }, comboId);
    }

    @Override 
    public Map<Long, List<ComboComponent>> findComponentsByComboIds(Collection<Long> comboIds) {
        if(comboIds == null || comboIds.isEmpty()) {
            return Collections.emptyMap();
        }

        StringJoiner placeholders = new StringJoiner(",");
        for(int i = 0 ; i < comboIds.size(); i++ ) {
            placeholders.add("?");
        }
        String sql = """
                SELECT
                    cc.combo_id,
                    cc.quantity,
                    p.id,
                    p.category_id,
                    p.name,
                    p.price,
                    p.description,
                    p.active,
                    p.product_type
                FROM combo_components cc
                JOIN products p ON p.id = cc.component_id
                WHERE cc.combo_id IN (%s)
                ORDER BY cc.combo_id, p.name
                """.formatted(placeholders);

        Map<Long, List<ComboComponent>> result = new HashMap<>();
        
        for(Long comboId : comboIds) {
            result.put(comboId, new ArrayList<>());
        }

        jdbcTemplate.query(sql, rs -> {
            long comboId = rs.getLong("combo_id");

            Product product = ProductFactory.create(
                    rs.getString("product_type"),
                    rs.getLong("id"),
                    rs.getLong("category_id"),
                    rs.getString("name"),
                    rs.getBigDecimal("price"),
                    rs.getString("description"),
                    rs.getBoolean("active")
            );
            result.computeIfAbsent(comboId, key -> new ArrayList<>())
                            .add(new ComboComponent(product, rs.getInt("quantity")));
        }, comboIds.toArray());

        return result;
    }

    @Override 
    public void replaceComponents(long comboId, List<ComboComponent> components) {

        jdbcTemplate.update("DELETE FROM combo_components WHERE combo_id = ?", comboId);

        if(components == null || components.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO combo_components
                    (combo_id, component_id, quantity)
                VALUES (?, ?, ?)
                """;

        for(ComboComponent component : components) {
            jdbcTemplate.update(sql, comboId, component.getProduct().getId(), component.getQuantity());
        }
    }

    @Override 
    public boolean existsInOrders(long productId) {
        return exists(
            "SELECT COUNT(*) FROM order_items WHERE product_id = ?", productId
        );
    }

    @Override 
    public boolean existsAsComponent(long productId) {
        return exists("SELECT COUNT(*) FROM combo_components WHERE component_id = ?", productId);
    }

    @Override 
    public boolean existsInRecipes(long productId) {
        return exists("SELECT COUNT(*) FROM recipes WHERE product_id = ?", productId);
    }

    @Override 
    public List<Product> findActiveCombosContaining(long productId) {
        String sql = """
                SELECT DISTINCT
                    p.id,
                    p.category_id,
                    p.name,
                    p.price,
                    p.description,
                    p.active,
                    p.product_type
                FROM products p
                JOIN combo_components cc ON cc.combo_id = p.id
                WHERE cc.component_id = ?
                  AND p.product_type = 'COMBO'
                  AND p.active = TRUE
                ORDER BY p.name
                """;

        return jdbcTemplate.query(sql, productRowMapper, productId);
    }
}
