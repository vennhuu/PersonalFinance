package com.vennhuu.PersonalFinance.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vennhuu.PersonalFinance.Entity.Response.Summary.ResCategoryStat;
import com.vennhuu.PersonalFinance.Entity.Response.Summary.ResSummary;
import com.vennhuu.PersonalFinance.Entity.Response.Summary.ResTrendPoint;
import com.vennhuu.PersonalFinance.Enum.TransactionType;
import com.vennhuu.PersonalFinance.Service.ReportService;
import com.vennhuu.PersonalFinance.Utils.Annotation.APIMessage;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @APIMessage("Get all summary")
    public ResponseEntity<ResSummary> getSummary(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        return ResponseEntity.ok(reportService.getSummary(fromDate, toDate));
    }

    @GetMapping("/by-category")
    @APIMessage("Get summary by category")
    public ResponseEntity<List<ResCategoryStat>> getByCategory(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(defaultValue = "EXPENSE") TransactionType type) {
        return ResponseEntity.ok(reportService.getStatsByCategory(fromDate, toDate, type));
    }

    @GetMapping("/trend")
    @APIMessage("Get summary by trend")
    public ResponseEntity<List<ResTrendPoint>> getTrend(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        return ResponseEntity.ok(reportService.getTrend(fromDate, toDate));
    }
}