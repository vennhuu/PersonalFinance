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

import com.vennhuu.PersonalFinance.Entity.Request.Transaction.TransactionReq;
import com.vennhuu.PersonalFinance.Entity.Response.Transaction.ResTransaction;
import com.vennhuu.PersonalFinance.Exception.BadRequestException;
import com.vennhuu.PersonalFinance.Service.TransactionService;
import com.vennhuu.PersonalFinance.Utils.Annotation.APIMessage;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    
    private final TransactionService transactionService ;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("")
    @APIMessage("Create a new Transaction")
    public ResponseEntity<ResTransaction> newTransaction(@Valid @RequestBody TransactionReq req) throws BadRequestException {
        //TODO: process POST request
        
        return ResponseEntity.status(HttpStatus.CREATED).body(this.transactionService.newTransaction(req));
    }
    
    @GetMapping("")
    @APIMessage("Get all transaction")
    public ResponseEntity<List<ResTransaction>> getAllTransaction() {
        return ResponseEntity.ok(this.transactionService.getAllTransactionByUser());
    }

    @GetMapping("/{id}")
    @APIMessage("Get detail transaction")
    public ResponseEntity<ResTransaction> getDetailTransaction(@PathVariable long id) {
        return ResponseEntity.ok(this.transactionService.getDetailTransaction(id));
    }

    @PutMapping("/{id}")
    @APIMessage("Update transaction")
    public ResponseEntity<ResTransaction> updateTransaction(@PathVariable long id, @Valid @RequestBody TransactionReq req) {
        //TODO: process PUT request
        
        return ResponseEntity.ok(this.transactionService.updateTransaction(id, req));
    }

    @DeleteMapping("/{id}")
    @APIMessage("Delete transaction")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        this.transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
