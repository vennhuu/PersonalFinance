package com.vennhuu.PersonalFinance.Controller;

import java.util.List;

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

import com.vennhuu.PersonalFinance.Entity.Request.Budget.BudgetReq;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Entity.Response.Budget.ResBudget;
import com.vennhuu.PersonalFinance.Service.BudgetService;
import com.vennhuu.PersonalFinance.Utils.Annotation.APIMessage;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {
    

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping("")
    @APIMessage("Create a new budget")
    public ResponseEntity<ResBudget> createBudget(@Valid @RequestBody BudgetReq req) {
        ResBudget result = budgetService.createBudget(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("")
    @APIMessage("Get all Budget")
    public ResponseEntity<ResultPaginationDTO> getAllBudget(
        @RequestParam String currentPage,
        @RequestParam String pageSize,
        Pageable pageable
    ) {
        return ResponseEntity.ok(budgetService.getAllBudgetByUser(pageable));
    }

    @GetMapping("/{id}")
    @APIMessage("Get detail Budget")
    public ResponseEntity<ResBudget> getDetailBudget(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getDetailBudget(id));
    }

    @PutMapping("/{id}")
    @APIMessage("Update Budget")
    public ResponseEntity<ResBudget> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetReq req) {
        return ResponseEntity.ok(budgetService.updateBudget(id, req));
    }

    @DeleteMapping("/{id}")
    @APIMessage("Delete budget")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
