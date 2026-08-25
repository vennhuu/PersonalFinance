package com.vennhuu.PersonalFinance.Service;

import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.vennhuu.PersonalFinance.Entity.Category;
import com.vennhuu.PersonalFinance.Entity.Request.Category.CategoryReq;
import com.vennhuu.PersonalFinance.Entity.Response.Category.ResCategory;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Exception.ResourceNotFoundException;
import com.vennhuu.PersonalFinance.Repository.CategoryRepository;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

@Service
public class CategoryService {
    
    private final CategoryRepository categoryRepository ;
    private final SecurityUtil securityUtil ;
    private final UserService userService ;

    public CategoryService(CategoryRepository categoryRepository, SecurityUtil securityUtil, UserService userService) {
        this.categoryRepository = categoryRepository;
        this.securityUtil = securityUtil ;
        this.userService = userService ;
    }

    public ResCategory convertToResCategory(Category category) {
        return new ResCategory(category.getId(), category.getName(), category.getType());
    }

    public Category save( Category category ) {
        return this.categoryRepository.save(category);
    }

    private User getCurrentUser() {
        String email = this.securityUtil.getCurrentUserLogin()
            .orElseThrow(() -> new BadCredentialsException("Vui lòng đăng nhập"));
        return this.userService.findByEmail(email) ;
    }

    public ResCategory addNewCategory(CategoryReq req) {
        User user = this.getCurrentUser();

        Category newCategory = new Category();
        newCategory.setName(req.getName());
        newCategory.setType(req.getType());
        newCategory.setUser(user);

        Category saved = this.categoryRepository.save(newCategory);
        return convertToResCategory(saved);
    }

    public List<ResCategory> getAllCategoryByUserId() {
        User user = this.getCurrentUser();
        return this.categoryRepository.getAllCategoriesByUserId(user.getId())
                .stream().map(this::convertToResCategory).toList();
    }

    public ResCategory getDetailCategory(Long categoryId) {
        User user = this.getCurrentUser();
        Category category = this.categoryRepository.findByIdAndUser_Id(categoryId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        return convertToResCategory(category);
    }

    public ResCategory updateCategory(Long categoryId, CategoryReq req) {
        User user = this.getCurrentUser();
        Category category = this.categoryRepository.findByIdAndUser_Id(categoryId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        category.setName(req.getName());
        category.setType(req.getType());

        Category updated = this.categoryRepository.save(category);
        return convertToResCategory(updated);
    }

    public void deleteCategory(Long categoryId) {
        User user = this.getCurrentUser();
        Category category = this.categoryRepository.findByIdAndUser_Id(categoryId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        this.categoryRepository.delete(category);
    }
}
