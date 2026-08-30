package com.vennhuu.PersonalFinance.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.vennhuu.PersonalFinance.Entity.Budget;
import com.vennhuu.PersonalFinance.Entity.Category;
import com.vennhuu.PersonalFinance.Entity.Request.Budget.BudgetReq;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Entity.Response.Budget.ResBudget;
import com.vennhuu.PersonalFinance.Entity.Response.Category.ResCategory;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Enum.TransactionType;
import com.vennhuu.PersonalFinance.Exception.BadRequestException;
import com.vennhuu.PersonalFinance.Exception.ResourceNotFoundException;
import com.vennhuu.PersonalFinance.Repository.BudgetRepository;
import com.vennhuu.PersonalFinance.Repository.CategoryRepository;
import com.vennhuu.PersonalFinance.Repository.TransactionRepository;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityUtil securityUtil;
    private final UserService userService;

    public BudgetService(
            BudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            SecurityUtil securityUtil,
            UserService userService
    ) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.securityUtil = securityUtil;
        this.userService = userService;
    }


    private User getCurrentUser() {
        String email = securityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadCredentialsException("Vui lòng đăng nhập"));
        return userService.findByEmail(email);
    }

    private Budget getOwnedBudget(Long id, Long userId) {
        return budgetRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân sách"));
    }

    private Category getOwnedCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
    }


    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private void validateCategoryIsExpenseType(Category category) {
        if (category.getType() != TransactionType.EXPENSE) {
            throw new BadRequestException("Ngân sách chỉ áp dụng cho danh mục loại chi tiêu (EXPENSE)");
        }
    }

    private void applyBudgetData(Budget budget, BudgetReq req, Category category) {
        budget.setCategory(category);
        budget.setAmountLimit(req.getAmountLimit());
        budget.setStartDate(req.getStartDate());
        budget.setEndDate(req.getEndDate());
    }

    private ResBudget convertToResBudget(Budget budget) {
        BigDecimal spent = transactionRepository.sumExpenseByCategoryAndDateRange(
                budget.getUser().getId(),
                budget.getCategory().getId(),
                budget.getStartDate(),
                budget.getEndDate()
        );

        BigDecimal remaining = budget.getAmountLimit().subtract(spent);

        double percentUsed = budget.getAmountLimit().compareTo(BigDecimal.ZERO) > 0
                ? spent.divide(budget.getAmountLimit(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        return new ResBudget(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getAmountLimit(),
                spent,
                remaining,
                percentUsed,
                budget.getStartDate(),
                budget.getEndDate()
        );
    }

    // api

    public ResBudget createBudget(BudgetReq req) {
        User user = getCurrentUser();
        Category category = getOwnedCategory(req.getCategoryId(), user.getId());

        validateDateRange(req.getStartDate(), req.getEndDate());
        validateCategoryIsExpenseType(category);

        Budget budget = new Budget();
        budget.setUser(user);
        applyBudgetData(budget, req, category);

        Budget saved = budgetRepository.save(budget);
        return convertToResBudget(saved);
    }

    public ResultPaginationDTO getAllBudgetByUser(Pageable pageable) {
        User user = this.getCurrentUser();
        
        Page<Budget> pageBudget = this.budgetRepository.getAllBudgetsByUserId(user.getId(), pageable) ;

        ResultPaginationDTO res = new ResultPaginationDTO() ;

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setCurrentPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotalElements(pageBudget.getTotalElements());
        meta.setTotalPages(pageBudget.getTotalPages());

        List<ResBudget> listBudget = pageBudget.getContent().stream().map(this::convertToResBudget).toList();

        res.setMeta(meta);
        res.setResult(listBudget);

        return res ;
    }

    public ResBudget getDetailBudget(Long id) {
        User user = getCurrentUser();
        Budget budget = getOwnedBudget(id, user.getId());
        return convertToResBudget(budget);
    }

    public ResBudget updateBudget(Long id, BudgetReq req) {
        User user = getCurrentUser();
        Budget budget = getOwnedBudget(id, user.getId());
        Category category = getOwnedCategory(req.getCategoryId(), user.getId());

        validateDateRange(req.getStartDate(), req.getEndDate());
        validateCategoryIsExpenseType(category);

        applyBudgetData(budget, req, category);
        Budget updated = budgetRepository.save(budget);

        return convertToResBudget(updated);
    }

    public void deleteBudget(Long id) {
        User user = getCurrentUser();
        Budget budget = getOwnedBudget(id, user.getId());
        budgetRepository.delete(budget);
    }
}
