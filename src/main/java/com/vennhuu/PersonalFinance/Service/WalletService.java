package com.vennhuu.PersonalFinance.Service;

import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.vennhuu.PersonalFinance.Entity.Request.Wallet.WalletReq;
import com.vennhuu.PersonalFinance.Entity.Response.Wallet.ResWallet;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Entity.Wallet;
import com.vennhuu.PersonalFinance.Exception.ResourceNotFoundException;
import com.vennhuu.PersonalFinance.Repository.WalletRepository;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

@Service
public class WalletService {
    
    private final WalletRepository walletRepository ;
    private final SecurityUtil securityUtil ;
    private final UserService userService ;

    public WalletService(WalletRepository walletRepository, SecurityUtil securityUtil, UserService userService) {
        this.walletRepository = walletRepository;
        this.securityUtil = securityUtil;
        this.userService = userService ;
    }

    private User getCurrentUser() {
        String email = this.securityUtil.getCurrentUserLogin()
            .orElseThrow(() -> new BadCredentialsException("Vui lòng đăng nhập"));
        return this.userService.findByEmail(email) ;
    }

    public Wallet save( Wallet wallet ) {
        return this.walletRepository.save(wallet) ;
    }

    public ResWallet convertToResWallet(Wallet wallet) {
        return new ResWallet(wallet.getId(), wallet.getName(), wallet.getType(), wallet.getMoney());
    }

    public ResWallet addNewWallet(WalletReq req) {
        User user = this.getCurrentUser();

        Wallet newWallet = new Wallet();
        newWallet.setName(req.getName());
        newWallet.setType(req.getType());
        newWallet.setMoney(req.getMoney());
        newWallet.setUser(user);

        Wallet saved = this.walletRepository.save(newWallet);
        return convertToResWallet(saved);
    }

    public List<ResWallet> getAllWalletByUserId() {
        User user = this.getCurrentUser();
        return this.walletRepository.getAllWalletByUserId(user.getId())
                .stream().map(this::convertToResWallet).toList();
    }


    public ResWallet getDetailWallet(Long walletId) {
        User user = this.getCurrentUser();
        Wallet wallet = this.walletRepository.findByIdAndUser_Id(walletId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay vi cua nguoi dung nay"));
        return convertToResWallet(wallet);
    }

    public ResWallet updateWallet(Long walletId, WalletReq req) {
        User user = this.getCurrentUser();
        Wallet wallet = this.walletRepository.findByIdAndUser_Id(walletId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay vi cua nguoi dung nay"));

        wallet.setName(req.getName());
        wallet.setType(req.getType());
        wallet.setMoney(req.getMoney());

        Wallet updated = this.walletRepository.save(wallet);
        return convertToResWallet(updated);
    }

    public void deleteWallet(Long walletId) {
        User user = this.getCurrentUser();
        Wallet wallet = this.walletRepository.findByIdAndUser_Id(walletId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        this.walletRepository.delete(wallet);
    }
}
