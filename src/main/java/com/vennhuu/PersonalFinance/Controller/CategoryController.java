package com.vennhuu.PersonalFinance.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vennhuu.PersonalFinance.Entity.Category;
import com.vennhuu.PersonalFinance.Entity.Request.Category.CategoryReq;
import com.vennhuu.PersonalFinance.Entity.Response.Category.ResCategory;
import com.vennhuu.PersonalFinance.Service.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    
    private final CategoryService categoryService ;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("")
    public ResponseEntity<ResCategory> addNewCategory(@Valid @RequestBody CategoryReq req) {
        
        return ResponseEntity.status(HttpStatus.CREATED).body(this.categoryService.addNewCategory(req));
    }

    @GetMapping("")
    public ResponseEntity<List<ResCategory>> getAllCategory() {
        return ResponseEntity.ok(this.categoryService.getAllCategoryByUserId());
    }
    
    @GetMapping("/{categoryId}")
    public ResponseEntity<ResCategory> getDetailCategory(@PathVariable long categoryId) {
        return ResponseEntity.ok(this.categoryService.getDetailCategory(categoryId));
    }
    
    @DeleteMapping("{categoryId}")
    public ResponseEntity<Category> deleteCategory(@PathVariable Long categoryId) {
        this.categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build() ;
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<ResCategory> putMethodName(@PathVariable Long categoryId, @Valid @RequestBody CategoryReq req) {
        //TODO: process PUT request
        
        return ResponseEntity.ok(this.categoryService.updateCategory(categoryId, req));
    }
}
