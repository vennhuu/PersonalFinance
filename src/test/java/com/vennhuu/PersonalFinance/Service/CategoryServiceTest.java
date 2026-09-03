package com.vennhuu.PersonalFinance.Service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vennhuu.PersonalFinance.Entity.Category;
import com.vennhuu.PersonalFinance.Entity.Request.Category.CategoryReq;
import com.vennhuu.PersonalFinance.Entity.Response.Category.ResCategory;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Enum.TransactionType;
import com.vennhuu.PersonalFinance.Exception.ResourceNotFoundException;
import com.vennhuu.PersonalFinance.Repository.CategoryRepository;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private UserService userService;

    @InjectMocks
    private CategoryService categoryService;

    private User sampleUser;
    private Category sampleCategory;
    private CategoryReq categoryReq;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("a@example.com", null));

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("a@example.com");

        sampleCategory = new Category();
        sampleCategory.setId(5L);
        sampleCategory.setName("An uong");
        sampleCategory.setType(TransactionType.EXPENSE);
        sampleCategory.setUser(sampleUser);

        categoryReq = new CategoryReq();
        categoryReq.setName("Du lich");
        categoryReq.setType(TransactionType.EXPENSE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockCurrentUser() {
        when(userService.findByEmail("a@example.com")).thenReturn(sampleUser);
    }

    // convertToResCategory
    @Nested
    @DisplayName("convertToResCategory(Category)")
    class ConvertToResCategory {

        @Test
        @DisplayName("Map all category fields correctly")
        void shouldMapAllFields() {
            ResCategory result = categoryService.convertToResCategory(sampleCategory);

            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getName()).isEqualTo("An uong");
            assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
        }
    }

    // save
    @Nested
    @DisplayName("save(Category)")
    class SaveCategory {

        @Test
        @DisplayName("Delegate to repository and return saved category")
        void shouldDelegateToRepository() {
            when(categoryRepository.save(sampleCategory)).thenReturn(sampleCategory);

            Category result = categoryService.save(sampleCategory);

            assertThat(result).isEqualTo(sampleCategory);
            verify(categoryRepository).save(sampleCategory);
        }
    }

    // addNewCategory
    @Nested
    @DisplayName("addNewCategory(CategoryReq)")
    class AddNewCategory {

        @Test
        @DisplayName("Create and return new category for current user")
        void shouldCreateCategorySuccessfully() {
            mockCurrentUser();

            Category saved = new Category();
            saved.setId(6L);
            saved.setName("Du lich");
            saved.setType(TransactionType.EXPENSE);

            when(categoryRepository.save(any(Category.class))).thenReturn(saved);

            ResCategory result = categoryService.addNewCategory(categoryReq);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(6L);
            assertThat(result.getName()).isEqualTo("Du lich");
            verify(categoryRepository).save(any(Category.class));
        }
    }

    // getAllCategoryByUserId
    @Nested
    @DisplayName("getAllCategoryByUserId(Pageable)")
    class GetAllCategoryByUserId {

        @Test
        @DisplayName("Return paginated category list for current user")
        void shouldReturnPaginatedCategories() {
            mockCurrentUser();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Category> page = new PageImpl<>(List.of(sampleCategory), pageable, 1);

            when(categoryRepository.getAllCategoriesByUserId(1L, pageable)).thenReturn(page);

            ResultPaginationDTO result = categoryService.getAllCategoryByUserId(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getMeta().getTotalElements()).isEqualTo(1);
            assertThat(result.getMeta().getCurrentPage()).isEqualTo(1);

            @SuppressWarnings("unchecked")
            List<ResCategory> categories = (List<ResCategory>) result.getResult();
            assertThat(categories).hasSize(1);
            assertThat(categories.get(0).getName()).isEqualTo("An uong");
        }

        @Test
        @DisplayName("Return empty result when user has no categories")
        void shouldReturnEmptyResultWhenNoCategories() {
            mockCurrentUser();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Category> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(categoryRepository.getAllCategoriesByUserId(1L, pageable)).thenReturn(emptyPage);

            ResultPaginationDTO result = categoryService.getAllCategoryByUserId(pageable);

            assertThat(result.getMeta().getTotalElements()).isEqualTo(0);
        }
    }

    // getDetailCategory
    @Nested
    @DisplayName("getDetailCategory(Long)")
    class GetDetailCategory {

        @Test
        @DisplayName("Return category details when found")
        void shouldReturnCategoryDetails() {
            mockCurrentUser();
            when(categoryRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(sampleCategory));

            ResCategory result = categoryService.getDetailCategory(5L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getName()).isEqualTo("An uong");
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when category not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(categoryRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getDetailCategory(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // updateCategory
    @Nested
    @DisplayName("updateCategory(Long, CategoryReq)")
    class UpdateCategory {

        @Test
        @DisplayName("Update category fields and return result")
        void shouldUpdateCategorySuccessfully() {
            mockCurrentUser();
            when(categoryRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(sampleCategory));

            Category updated = new Category();
            updated.setId(5L);
            updated.setName("Du lich");
            updated.setType(TransactionType.EXPENSE);
            when(categoryRepository.save(any(Category.class))).thenReturn(updated);

            ResCategory result = categoryService.updateCategory(5L, categoryReq);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Du lich");
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when category not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(categoryRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.updateCategory(99L, categoryReq))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // deleteCategory
    @Nested
    @DisplayName("deleteCategory(Long)")
    class DeleteCategory {

        @Test
        @DisplayName("Delete category when found for user")
        void shouldDeleteCategorySuccessfully() {
            mockCurrentUser();
            when(categoryRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(sampleCategory));

            categoryService.deleteCategory(5L);

            verify(categoryRepository).delete(sampleCategory);
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when category not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(categoryRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
