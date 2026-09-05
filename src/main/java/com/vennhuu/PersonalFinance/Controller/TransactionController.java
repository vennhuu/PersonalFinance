package com.vennhuu.PersonalFinance.Controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import org.springframework.core.io.Resource;
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
import org.springframework.web.multipart.MultipartFile;

import com.vennhuu.PersonalFinance.Entity.Request.Transaction.TransactionReq;
import com.vennhuu.PersonalFinance.Entity.Response.File.ResUploadFileDTO;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Entity.Response.Transaction.ResTransaction;
import com.vennhuu.PersonalFinance.Exception.BadRequestException;
import com.vennhuu.PersonalFinance.Exception.StorageException;
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
    public ResponseEntity<ResultPaginationDTO> getAllTransaction(
        @RequestParam String currentPage, 
        @RequestParam String pageSize,
        Pageable pageable
    ) {
        return ResponseEntity.ok(this.transactionService.getAllTransactionByUser(pageable));
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

    @PostMapping("/{id}/receipt")
    @APIMessage("Upload receipt for transaction")
    public ResponseEntity<ResUploadFileDTO> uploadReceipt(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws URISyntaxException, IOException, StorageException {
        return ResponseEntity.ok(transactionService.uploadReceipt(id, file));
    }

    @GetMapping("/{id}/receipt")
    @APIMessage("Get receipt of transaction")
    public ResponseEntity<Resource> getReceipt(@PathVariable Long id)
            throws URISyntaxException, FileNotFoundException, StorageException {
                
        return this.transactionService.getReceipt(id);
    }

    @DeleteMapping("/{id}/receipt")
    @APIMessage("Delete receipt of transaction")
    public ResponseEntity<Void> deleteReceipt(@PathVariable Long id) throws URISyntaxException {
        transactionService.deleteReceipt(id);
        return ResponseEntity.noContent().build();
    }

}
