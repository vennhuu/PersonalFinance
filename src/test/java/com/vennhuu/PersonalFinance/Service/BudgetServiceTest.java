package com.vennhuu.PersonalFinance.Service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.vennhuu.PersonalFinance.Entity.Budget;
import com.vennhuu.PersonalFinance.Entity.Category;
import com.vennhuu.PersonalFinance.Entity.Request.Budget.BudgetReq;
import com.vennhuu.PersonalFinance.Entity.Response.Budget.ResBudget;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Enum.TransactionType;
import com.vennhuu.PersonalFinance.Exception.BadRequestException;
import com.vennhuu.PersonalFinance.Exception.ResourceNotFoundException;
import com.vennhuu.PersonalFinance.Repository.BudgetRepository;
import com.vennhuu.PersonalFinance.Repository.CategoryRepository;
import com.vennhuu.PersonalFinance.Repository.TransactionRepository;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private UserService userService;

    @InjectMocks
    private BudgetService budgetService;

    private User sampleUser;
    private Category expenseCategory;
    private Category incomeCategory;
    private Budget sampleBudget;
    private BudgetReq budgetReq;

    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 30);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("a@example.com", null));

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("a@example.com");

        expenseCategory = new Category();
        expenseCategory.setId(5L);
        expenseCategory.setName("An uong");
        expenseCategory.setType(TransactionType.EXPENSE);
        expenseCategory.setUser(sampleUser);

        incomeCategory = new Category();
        incomeCategory.setId(6L);
        incomeCategory.setName("Luong");
        incomeCategory.setType(TransactionType.INCOME);
        incomeCategory.setUser(sampleUser);

        sampleBudget = new Budget();
        sampleBudget.setId(20L);
        sampleBudget.setUser(sampleUser);
        sampleBudget.setCategory(expenseCategory);
        sampleBudget.setAmountLimit(BigDecimal.valueOf(2_000_000));
        sampleBudget.setStartDate(START);
        sampleBudget.setEndDate(END);

        budgetReq = new BudgetReq();
        budgetReq.setCategoryId(5L);
        budgetReq.setAmountLimit(BigDecimal.valueOf(2_000_000));
        budgetReq.setStartDate(START);
        budgetReq.setEndDate(END);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockCurrentUser() {
        when(userService.findByEmail("a@example.com")).thenReturn(sampleUser);
    }

    private void mockSpentAmount(BigDecimal spent) {
        when(transactionRepository.sumExpenseByCategoryAndDateRange(
                eq(1L), eq(5L), eq(START), eq(END)))
                .thenReturn(spent);
    }

    // createBudget
    @Nested
    @DisplayName("createBudget(BudgetReq)")
    class CreateBudget {

        @Test
        @DisplayName("Throw BadRequestException when endDate is before startDate")
        void shouldThrowWhenEndDateBeforeStartDate() {
            mockCurrentUser();
            when(categoryRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(expenseCategory));

            BudgetReq invalidReq = new BudgetReq();
            invalidReq.setCategoryId(5L);
            invalidReq.setAmountLimit(BigDecimal.valueOf(1_000_000));
            invalidReq.setStartDate(END); // start = Sep 30
            invalidReq.setEndDate(START); // end = Sep 1 → invalid

            assertThatThrownBy(() -> budgetService.createBudget(invalidReq))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Ngày kết thúc phải sau ngày bắt đầu");
        }

        @Test
        @DisplayName("Throw BadRequestException when category type is not EXPENSE")
        void shouldThrowWhenCategoryIsNotExpense() {
            mockCurrentUser();
            when(categoryRepository.findByIdAndUser_Id(6L, 1L)).thenReturn(Optional.of(incomeCategory));

            BudgetReq incomeReq = new BudgetReq();
            incomeReq.setCategoryId(6L);
            incomeReq.setAmountLimit(BigDecimal.valueOf(1_000_000));
            incomeReq.setStartDate(START);
            incomeReq.setEndDate(END);

            assertThatThrownBy(() -> budgetService.createBudget(incomeReq))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("EXPENSE");
        }

        @Test
        @DisplayName("Create budget successfully and return correct amounts")
        void shouldCreateBudgetSuccessfully() {
            mockCurrentUser();
            when(categoryRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(expenseCategory));
            when(budgetRepository.save(any(Budget.class))).thenReturn(sampleBudget);
            mockSpentAmount(BigDecimal.valueOf(500_000));

            ResBudget result = budgetService.createBudget(budgetReq);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(20L);
            assertThat(result.getAmountLimit()).isEqualByComparingTo(BigDecimal.valueOf(2_000_000));
            assertThat(result.getSpentAmount()).isEqualByComparingTo(BigDecimal.valueOf(500_000));
            assertThat(result.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_500_000));
            assertThat(result.getPercentUsed()).isEqualTo(25.0, within(0.01));
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when category not found")
        void shouldThrowWhenCategoryNotFound() {
            mockCurrentUser();
            when(categoryRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.createBudget(budgetReq))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // convertToResBudget (indirectly via createBudget / getDetailBudget)
    @Nested
    @DisplayName("convertToResBudget - calculation edge cases")
    class ConvertToResBudget {

        @Test
        @DisplayName("percentUsed = 0 when amountLimit is zero")
        void shouldReturnZeroPercentWhenLimitIsZero() {
            mockCurrentUser();
            sampleBudget.setAmountLimit(BigDecimal.ZERO);
            when(budgetRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(sampleBudget));
            when(transactionRepository.sumExpenseByCategoryAndDateRange(1L, 5L, START, END))
                    .thenReturn(BigDecimal.ZERO);

            ResBudget result = budgetService.getDetailBudget(20L);

            assertThat(result.getPercentUsed()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("remaining = 0 when spent exceeds limit")
        void shouldHaveZeroRemainingWhenSpentExceedsLimit() {
            mockCurrentUser();
            when(budgetRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(sampleBudget));
            // spent > limit
            when(transactionRepository.sumExpenseByCategoryAndDateRange(1L, 5L, START, END))
                    .thenReturn(BigDecimal.valueOf(3_000_000));

            ResBudget result = budgetService.getDetailBudget(20L);

            assertThat(result.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(-1_000_000));
            assertThat(result.getPercentUsed()).isGreaterThan(100.0);
        }
    }

    // getAllBudgetByUser
    @Nested
    @DisplayName("getAllBudgetByUser(Pageable)")
    class GetAllBudgetByUser {

        @Test
        @DisplayName("Return paginated budgets for current user")
        void shouldReturnPaginatedBudgets() {
            mockCurrentUser();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Budget> page = new PageImpl<>(List.of(sampleBudget), pageable, 1);

            when(budgetRepository.getAllBudgetsByUserId(1L, pageable)).thenReturn(page);
            mockSpentAmount(BigDecimal.valueOf(300_000));

            ResultPaginationDTO result = budgetService.getAllBudgetByUser(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getMeta().getTotalElements()).isEqualTo(1);
        }
    }

    // getDetailBudget
    @Nested
    @DisplayName("getDetailBudget(Long)")
    class GetDetailBudget {

        @Test
        @DisplayName("Return budget with correct computed fields")
        void shouldReturnBudgetDetails() {
            mockCurrentUser();
            when(budgetRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(sampleBudget));
            mockSpentAmount(BigDecimal.valueOf(1_000_000));

            ResBudget result = budgetService.getDetailBudget(20L);

            assertThat(result.getId()).isEqualTo(20L);
            assertThat(result.getSpentAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
            assertThat(result.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
            assertThat(result.getPercentUsed()).isEqualTo(50.0, within(0.01));
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when budget not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(budgetRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.getDetailBudget(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // updateBudget
    @Nested
    @DisplayName("updateBudget(Long, BudgetReq)")
    class UpdateBudget {

        @Test
        @DisplayName("Update budget successfully")
        void shouldUpdateBudgetSuccessfully() {
            mockCurrentUser();
            when(budgetRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(sampleBudget));
            when(categoryRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(expenseCategory));
            when(budgetRepository.save(any(Budget.class))).thenReturn(sampleBudget);
            mockSpentAmount(BigDecimal.valueOf(500_000));

            ResBudget result = budgetService.updateBudget(20L, budgetReq);

            assertThat(result).isNotNull();
            verify(budgetRepository).save(sampleBudget);
        }

        @Test
        @DisplayName("Throw BadRequestException when date range is invalid on update")
        void shouldThrowOnInvalidDateRange() {
            mockCurrentUser();
            when(budgetRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(sampleBudget));
            when(categoryRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(expenseCategory));

            budgetReq.setStartDate(END);
            budgetReq.setEndDate(START);

            assertThatThrownBy(() -> budgetService.updateBudget(20L, budgetReq))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Throw BadRequestException when category is INCOME type")
        void shouldThrowWhenCategoryIsIncome() {
            mockCurrentUser();
            BudgetReq incomeReq = new BudgetReq(6L, BigDecimal.valueOf(1_000_000), START, END);

            when(budgetRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(sampleBudget));
            when(categoryRepository.findByIdAndUser_Id(6L, 1L)).thenReturn(Optional.of(incomeCategory));

            assertThatThrownBy(() -> budgetService.updateBudget(20L, incomeReq))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("EXPENSE");
        }
    }

    // deleteBudget
    @Nested
    @DisplayName("deleteBudget(Long)")
    class DeleteBudget {

        @Test
        @DisplayName("Delete budget when found for user")
        void shouldDeleteBudgetSuccessfully() {
            mockCurrentUser();
            when(budgetRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(sampleBudget));

            budgetService.deleteBudget(20L);

            verify(budgetRepository).delete(sampleBudget);
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when budget not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(budgetRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.deleteBudget(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
