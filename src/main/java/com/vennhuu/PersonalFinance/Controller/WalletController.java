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

import com.vennhuu.PersonalFinance.Entity.Request.Wallet.WalletReq;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Entity.Response.Wallet.ResWallet;
import com.vennhuu.PersonalFinance.Service.WalletService;
import com.vennhuu.PersonalFinance.Utils.Annotation.APIMessage;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {
    
    private final WalletService walletService ;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("")
    @APIMessage("Add new wallet")
    public ResponseEntity<ResWallet> addNewWallet(@Valid @RequestBody WalletReq wallet) {
        //TODO: process POST request
        
        return ResponseEntity.status(HttpStatus.CREATED).body(this.walletService.addNewWallet(wallet));
    }

    @GetMapping("")
    @APIMessage("Get all wallet by user")
    public ResponseEntity<ResultPaginationDTO> getAllWalletByUserId(
            @RequestParam String currentPage, 
            @RequestParam String pageSize,
            Pageable pageable
        ) {
        return ResponseEntity.ok(this.walletService.getAllWalletByUserId(pageable));
    }

    @GetMapping("/{walletId}")
    @APIMessage("Get detail wallet by user")
    public ResponseEntity<ResWallet> getDetailWallet(@PathVariable long walletId) {
        return ResponseEntity.ok(this.walletService.getDetailWallet(walletId));
    }

    @DeleteMapping("{walletId}")
    @APIMessage("Delete Wallet")
    public ResponseEntity<Void> deleteWallet(@PathVariable Long walletId) {
        this.walletService.deleteWallet(walletId);
        return ResponseEntity.noContent().build() ;
    }

    @PutMapping("/{walletId}")
    @APIMessage("Update wallet")
    public ResponseEntity<ResWallet> putMethodName(@PathVariable Long walletId, @Valid @RequestBody WalletReq req) {
        //TODO: process PUT request
        
        return ResponseEntity.ok(this.walletService.updateWallet(walletId, req));
    }

}
