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

import com.vennhuu.PersonalFinance.Entity.Request.Goal.GoalContributeReq;
import com.vennhuu.PersonalFinance.Entity.Request.Goal.GoalReq;
import com.vennhuu.PersonalFinance.Entity.Response.Goal.ResGoal;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Service.GoalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping("")
    public ResponseEntity<ResGoal> createGoal(@Valid @RequestBody GoalReq req) {
        ResGoal result = goalService.createGoal(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("")
    public ResponseEntity<ResultPaginationDTO> getAllGoal(
        @RequestParam String currentPage,
        @RequestParam String pageSize,
        Pageable pageable
    ) {
        return ResponseEntity.ok(goalService.getAllGoalByUser(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResGoal> getDetailGoal(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getDetailGoal(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResGoal> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody GoalReq req) {
        return ResponseEntity.ok(goalService.updateGoal(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<ResGoal> contribute(
            @PathVariable Long id,
            @Valid @RequestBody GoalContributeReq req) {
        return ResponseEntity.ok(goalService.contributeToGoal(id, req));
    }
}