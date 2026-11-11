package com.foodstore.menu;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service 
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryDao categoryDao;

    public CategoryServiceImpl(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    private String normalizeName(String name) {
        if(name == null || name.trim().isEmpty()) {
            throw new BusinessException("Tên danh mục không được để trống");
        }

        return name.trim();
    }

    @Override 
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryDao.findAll();
    }

    @Override 
    @Transactional(rollbackFor = Exception.class)
    public long create(String name) {
        String normalizeName = normalizeName(name);
        if(categoryDao.existsByName(normalizeName)) {
            throw new BusinessException("Tên danh mục đã tồn tại");
        }

        try{
            return categoryDao.save(new Category(0l, normalizeName));
        } catch(DuplicateKeyException ex ) {
            throw new BusinessException("Tên đã tồn tại");
        }
    }

    @Override 
    @Transactional(rollbackFor = Exception.class)
    public void rename(long id, String name) {
        Category category = categoryDao.findById(id).orElseThrow(()-> 
            new BusinessException("Không tìm thấy Id danh mục bạn tìm"));

        String normalizeName = normalizeName(name);

        //khi đổi tên kiểm tra tên mới có trùng với tên các combo trước đó hay không
        if(!category.getName().equalsIgnoreCase(normalizeName) && categoryDao.existsByName(normalizeName)) {
            throw new BusinessException("Tên danh mục đã tồn tại");
        }

        category.setName(normalizeName);

        try{
            categoryDao.update(category);
        } catch(DuplicateKeyException ex) {
            throw new BusinessException("Tên danh mục đã tồn tại");
        }
    }

    @Override 
    @Transactional(rollbackFor =  Exception.class)
    public void delete(long id) {
        if (categoryDao.findById(id).isEmpty()) {
            throw new BusinessException("Không tìm thấy danh mục");
        }

        if(categoryDao.existsProductIn(id)) {
            throw new BusinessException("Không thể xóa danh mục vì vẫn còn món thuộc danh mục này");
        }

            categoryDao.delete(id);
        
    }

}
