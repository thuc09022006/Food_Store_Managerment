package com.foodstore.menu;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service 
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductDao productDao;

    public ProductServiceImpl(ProductDao productDao) {
        this.productDao = productDao;
    }


    @Override 
    public Product getById(long id) {
        return productDao.findById(id).orElseThrow(() ->
                             new BusinessException("Không tìm thấy món có Id là: "+ id));
    }


    @Override 
    public List<Product> findActive() {
        return productDao.search(null, null, true);
    }


    @Override 
    public List<ComboComponent> getComponents(long comboId) {
        Product product = getById(comboId);

        if(!(product instanceof Combo)) {
            throw new BusinessException("Sản phầm này không phải Combo!");
        }

        return productDao.findComponents(comboId);
    }
}
