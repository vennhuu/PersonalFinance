package com.vennhuu.PersonalFinance.Controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vennhuu.PersonalFinance.Entity.Request.Category.CategoryReq;
import com.vennhuu.PersonalFinance.Entity.Response.Category.ResCategory;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Service.CategoryService;
import com.vennhuu.PersonalFinance.Utils.Annotation.APIMessage;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    
    private final CategoryService categoryService ;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("")
    @APIMessage("Add new Category")
    public ResponseEntity<ResCategory> addNewCategory(@Valid @RequestBody CategoryReq req) {
        
        return ResponseEntity.status(HttpStatus.CREATED).body(this.categoryService.addNewCategory(req));
    }

    @GetMapping("")
    @APIMessage("Get all Category by user")
    public ResponseEntity<ResultPaginationDTO> getAllCategory(
        @RequestParam String currentPage, 
        @RequestParam String pageSize,
        Pageable pageable
    ) {
        return ResponseEntity.ok(this.categoryService.getAllCategoryByUserId(pageable));
    }
    
    @GetMapping("/{categoryId}")
    @APIMessage("Get detail category by user")
    public ResponseEntity<ResCategory> getDetailCategory(@PathVariable long categoryId) {
        return ResponseEntity.ok(this.categoryService.getDetailCategory(categoryId));
    }
    
    @DeleteMapping("{categoryId}")
    @APIMessage("Delete category")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        this.categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build() ;
    }

    @PutMapping("/{categoryId}")
    @APIMessage("Update Category")
    public ResponseEntity<ResCategory> updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryReq req) {
        //TODO: process PUT request
        
        return ResponseEntity.ok(this.categoryService.updateCategory(categoryId, req));
    }
}
