package com.foodstore.menu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodstore.common.BusinessException;

@Service 
public class ProductAdminServiceImpl implements ProductAdminService{
    
    private final ProductDao productDao;
    private final CategoryDao categoryDao;

    public ProductAdminServiceImpl(ProductDao productDao, CategoryDao categoryDao) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
    }


    private void validateProduct(Product product) {

        if(product == null) {
            throw new BusinessException("Thông tin món không hợp lệ!");
        }

        if(product.getName() == null || product.getName().trim().isEmpty()) {
            throw new BusinessException("Tên món không được để trống!");
        }

        BigDecimal price = product.getPrice();

        if(price == null) {
            throw new BusinessException("Giá món không được để trống!");
        }

        if(price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Giá món không được âm!");
        }

        BigDecimal normalized = price.stripTrailingZeros();//Loại bỏ số 0 phần thập phân

        if(normalized.scale() > 0) {     // đếm số chữ số của phần thập phân
            throw new BusinessException("Giá món không được có phần thập phân!");
        }

        if(normalized.precision() > 12) {    //đếm số chữ số
            throw new BusinessException("Giá không được vượt quá 12 chữ số!");
        }

        String type = product.getType();
        if(!List.of("FOOD", "DRINK", "COMBO").contains(type)) {
            throw new BusinessException("Loại sản phẩm không hợp lệ!");
        }
    }


    private void validateCategory(long categoryId) {
        if(categoryId <= 0 || categoryDao.findById(categoryId).isEmpty()) {
            throw new BusinessException("Danh mục không tồn tại!");
        }
    }


    private List<ComboComponent> validateComponents(Product product, List<ComboComponent> components) {
        if(!(product instanceof Combo)) {
            if(components != null && !components.isEmpty()) {
                throw new BusinessException("Chỉ Combo mới có thành phần!");
            }
        return List.of(); //danh sách rỗng
        }

        if(components == null || components.isEmpty()) {
            throw new BusinessException("Combo phải có ít nhất 1 thành phần!");
        }

        List<ComboComponent> result =  new ArrayList<>();
        Set<Long> componentIds = new HashSet<>();

        for(ComboComponent component : components) {

            if(component == null || component.getProduct() == null) {
                throw new BusinessException("Thành phần combo không hợp lệ!");
            }

            long componentId = component.getProduct().getId();

            if(componentId <= 0) {
                throw new BusinessException("Món trong combo không hợp lệ!");
            }

            if(component.getQuantity() < 1) {
                throw new BusinessException("Số lượng thành phần phải từ 1 trở lên!");
            }

            if(!componentIds.add(componentId)){   //nếu dữ liệu thêm vào mà trùng với phần tử trong componentIds thì false, không trùng thì true
                throw new BusinessException("Không được thêm trùng món vào combo!");
            }

            Product actualProduct = productDao.findById(componentId).orElseThrow(()->
                new BusinessException("Không tìm thấy món có Id là: " + componentId));

            if(actualProduct instanceof Combo) {
                throw new BusinessException("Không được đặt combo bên trong combo khác!");
            }

            if(product.isActive() && !actualProduct.isActive()) {
                throw new BusinessException("Combo đang bán không được chưa món ngừng bán");
            }

            result.add(new ComboComponent(actualProduct, component.getQuantity()));
        }
        return result;
    }

    
    private void validateActiveComponents(List<ComboComponent> components) {
        if(components == null || components.isEmpty()) {
            throw new BusinessException("Không thể mở bán Combo chưa có thành phần!");
        }

        for(ComboComponent component : components) {
            if(!component.getProduct().isActive()) {
                throw new BusinessException("Không thể bán Combo vì món " + component.getProduct().getName() + "chưa được bán");
            }
        }
    }

    @Override 
    @Transactional(readOnly = true)
    public List<Product> search(String name, Long categoryId, Boolean active) {
        return productDao.search(name, categoryId, active);
    }


    @Override 
    @Transactional(rollbackFor = Exception.class)
    public long create(Product product, List<ComboComponent> components) {

        validateProduct(product);
        validateCategory(product.getCategoryId());

        List<ComboComponent> validComponents = validateComponents(product, components);

        long newId = productDao.save(product);

        if(product instanceof Combo) {
            productDao.replaceComponents(newId, validComponents);
        }
        return newId;
    }


    @Override 
    @Transactional(rollbackFor = Exception.class)
    public void update(Product product, List<ComboComponent> components) {
        if(product == null) {
            throw new BusinessException("Thông tin món không hợp lệ");
        }

        Product current = productDao.findById(product.getId()).orElseThrow(() -> 
            new BusinessException("Không tìm thấy món cần cập nhật"));

        if(!current.getType().equals(product.getType())) {
            throw new BusinessException("Không được đổi loại sản phẩm sau khi tạo");
        }

        validateProduct(product);
        validateCategory(product.getCategoryId());

        List<ComboComponent> validComponents = validateComponents(product, components);

        productDao.update(product);

        if(product instanceof Combo) {
            productDao.replaceComponents(product.getId(), validComponents);
        }
    }


    @Override 
    @Transactional(rollbackFor = Exception.class)
    public void setActive(long id, boolean active) {

        Product product = productDao.findById(id).orElseThrow(()->
            new BusinessException("Không tìm thấy món cần cập nhật"));

        //Nếu một sản phẩm bị chuyển sang trạng thái ngừng hoạt động, chương trình sẽ tìm tất cả Combo đang hoạt động có chứa sản phẩm đó.
        if(!active) {
            List<Product> activeCombos = productDao.findActiveCombosContaining(id);
            if(!activeCombos.isEmpty()) {
                String comboNames = activeCombos.stream()
                    .map(Product::getName)
                    .reduce((a,b) -> a + ", " + b)
                    .orElse("");
                
                throw new BusinessException("Không thể ngừng bán món này vì đang thuộc combo đang bán: " + comboNames);
            } 
        }

        //kiểm tra thành phần trong combo trước khi bán lại combo đó
        if(active == true && product instanceof Combo) {
            List<ComboComponent> components = productDao.findComponents(id);

            validateActiveComponents(components);
        }

        productDao.setActive(id, active);
    }

    @Override 
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        Product product = productDao.findById(id).orElseThrow(() -> 
                new BusinessException("Không tìm thấy món cần xóa"));

        //Nếu là thành phần của combo đang bán thì từ chối xóa.
        List<Product> activeCombos = productDao.findActiveCombosContaining(id);

        if(!activeCombos.isEmpty()) {
            String comboNames = activeCombos.stream()
                .map(Product::getName)
                .reduce((a,b) -> a + ", " + b)
                .orElse("");

            throw new BusinessException("Không thể xóa món này vì đang thuộc combo đang bán: " + comboNames);
        }

        //xóa mềm(Soft delete) bằng cách đặt trạng thái active = false
        boolean referenced = productDao.existsInOrders(id) || productDao.existsInRecipes(id) || productDao.existsAsComponent(id);
        if(referenced) {
            productDao.setActive(id, false);
            return;
        }

        //xóa cứng(Hard delete) bằng cách xóa dữ liệu khỏi cơ sở dữ liệu
        try{
            if(product instanceof Combo) {
                productDao.replaceComponents(id, List.of());
            }

            productDao.delete(id);
            
        } catch(DataIntegrityViolationException ex) {     //Có tham chiếu phát sinh trong lúc xóa: chuyển sang ngừng bán.
            productDao.setActive(id, false);
        }
    }
}
