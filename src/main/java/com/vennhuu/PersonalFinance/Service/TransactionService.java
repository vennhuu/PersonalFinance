package com.vennhuu.PersonalFinance.Service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.vennhuu.PersonalFinance.Entity.Category;
import com.vennhuu.PersonalFinance.Entity.Request.Transaction.TransactionReq;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Entity.Response.Transaction.ResTransaction;
import com.vennhuu.PersonalFinance.Entity.Transaction;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Entity.Wallet;
import com.vennhuu.PersonalFinance.Enum.TransactionType;
import com.vennhuu.PersonalFinance.Exception.BadRequestException;
import com.vennhuu.PersonalFinance.Exception.ResourceNotFoundException;
import com.vennhuu.PersonalFinance.Repository.CategoryRepository;
import com.vennhuu.PersonalFinance.Repository.TransactionRepository;
import com.vennhuu.PersonalFinance.Repository.WalletRepository;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

import jakarta.transaction.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            SecurityUtil securityUtil,
            UserService userService,
            WalletRepository walletRepository,
            CategoryRepository categoryRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.walletRepository = walletRepository;
        this.categoryRepository = categoryRepository;
    }

    private User getCurrentUser() {
        String email = securityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadCredentialsException("Vui lòng đăng nhập"));
        return userService.findByEmail(email);
    }

    private Transaction getOwnedTransaction(Long id, Long userId) {
        return transactionRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch"));
    }

    private Wallet getOwnedWallet(Long walletId, Long userId) {
        return walletRepository.findByIdAndUser_Id(walletId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví"));
    }

    private Category getOwnedCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
    }

    private void validateCategoryMatchesType(Category category, TransactionType type) {
        if (category.getType() != type) {
            throw new BadRequestException(
                    "Loại giao dịch không khớp với danh mục đã chọn (danh mục này chỉ dùng cho "
                            + category.getType() + ")"
            );
        }
    }

    private Transaction buildTransaction(TransactionReq req, User user, Wallet wallet, Category category) {
        Transaction tx = new Transaction();
        tx.setUser(user);
        applyNewTransactionData(tx, req, wallet, category);
        return tx;
    }

    private void applyNewTransactionData(Transaction tx, TransactionReq req, Wallet wallet, Category category) {
        tx.setWallet(wallet);
        tx.setCategory(category);
        tx.setType(req.getType());
        tx.setAmount(req.getAmount());
        tx.setTransactionDate(req.getTransactionDate());
        tx.setNote(req.getNote());
    }

    private void applyBalanceChange(Wallet wallet, TransactionType type, BigDecimal amount) {
        BigDecimal newBalance = (type == TransactionType.INCOME)
                ? wallet.getMoney().add(amount)
                : wallet.getMoney().subtract(amount);
        wallet.setMoney(newBalance);
        walletRepository.save(wallet);
    }

    private void reverseBalanceChange(Wallet wallet, TransactionType type, BigDecimal amount) {
        BigDecimal newBalance = (type == TransactionType.INCOME)
                ? wallet.getMoney().subtract(amount)
                : wallet.getMoney().add(amount);
        wallet.setMoney(newBalance);
        walletRepository.save(wallet);
    }

    private void rollbackOldBalance(Transaction tx) {
        reverseBalanceChange(tx.getWallet(), tx.getType(), tx.getAmount());
    }

    public ResTransaction convertToResTransaction(Transaction tx) {
        return new ResTransaction(
                tx.getId(),
                tx.getWallet().getId(),
                tx.getWallet().getName(),
                tx.getCategory().getId(),
                tx.getCategory().getName(),
                tx.getType(),
                tx.getAmount(),
                tx.getTransactionDate(),
                tx.getNote()
        );
    }

    @Transactional
    public ResTransaction newTransaction(TransactionReq req) {
        User user = getCurrentUser();
        Wallet wallet = getOwnedWallet(req.getWalletId(), user.getId());
        Category category = getOwnedCategory(req.getCategoryId(), user.getId());
        validateCategoryMatchesType(category, req.getType());

        Transaction saved = transactionRepository.save(buildTransaction(req, user, wallet, category));
        applyBalanceChange(wallet, req.getType(), req.getAmount());

        return convertToResTransaction(saved);
    }

    public ResultPaginationDTO getAllTransactionByUser(Pageable pageable) {
        
        User user = getCurrentUser();
        Page<Transaction> pageTransaction = this.transactionRepository.getAllTransactionByUserId(user.getId(), pageable) ;

        ResultPaginationDTO res = new ResultPaginationDTO() ;

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setCurrentPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotalElements(pageTransaction.getTotalElements());
        meta.setTotalPages(pageTransaction.getTotalPages());

        List<ResTransaction> listTransaction = pageTransaction.getContent().stream().map(this::convertToResTransaction).toList();

        res.setMeta(meta);
        res.setResult(listTransaction);

        return res ;
    }

    public ResTransaction getDetailTransaction(Long id) {
        User user = getCurrentUser();
        Transaction tx = getOwnedTransaction(id, user.getId());
        return convertToResTransaction(tx);
    }

    @Transactional
    public ResTransaction updateTransaction(Long id, TransactionReq req) {
        User user = getCurrentUser();

        Transaction tx = getOwnedTransaction(id, user.getId());
        Wallet newWallet = getOwnedWallet(req.getWalletId(), user.getId());
        Category newCategory = getOwnedCategory(req.getCategoryId(), user.getId());
        validateCategoryMatchesType(newCategory, req.getType());

        rollbackOldBalance(tx);
        applyNewTransactionData(tx, req, newWallet, newCategory);
        Transaction updated = transactionRepository.save(tx);
        applyBalanceChange(newWallet, req.getType(), req.getAmount());

        return convertToResTransaction(updated);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        User user = getCurrentUser();
        Transaction tx = getOwnedTransaction(id, user.getId());

        rollbackOldBalance(tx);
        transactionRepository.delete(tx);
    }
}