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

import com.vennhuu.PersonalFinance.Entity.Category;
import com.vennhuu.PersonalFinance.Entity.Request.Transaction.TransactionReq;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Entity.Response.Transaction.ResTransaction;
import com.vennhuu.PersonalFinance.Entity.Transaction;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Entity.Wallet;
import com.vennhuu.PersonalFinance.Enum.TransactionType;
import com.vennhuu.PersonalFinance.Enum.WalletType;
import com.vennhuu.PersonalFinance.Exception.BadRequestException;
import com.vennhuu.PersonalFinance.Exception.ResourceNotFoundException;
import com.vennhuu.PersonalFinance.Repository.CategoryRepository;
import com.vennhuu.PersonalFinance.Repository.TransactionRepository;
import com.vennhuu.PersonalFinance.Repository.WalletRepository;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private UserService userService;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User sampleUser;
    private Wallet sampleWallet;
    private Category expenseCategory;
    private Category incomeCategory;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("a@example.com", null));

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("a@example.com");

        sampleWallet = new Wallet();
        sampleWallet.setId(10L);
        sampleWallet.setName("Tien mat");
        sampleWallet.setType(WalletType.CASH);
        sampleWallet.setMoney(BigDecimal.valueOf(1_000_000));
        sampleWallet.setUser(sampleUser);

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

        sampleTransaction = new Transaction();
        sampleTransaction.setId(100L);
        sampleTransaction.setUser(sampleUser);
        sampleTransaction.setWallet(sampleWallet);
        sampleTransaction.setCategory(expenseCategory);
        sampleTransaction.setType(TransactionType.EXPENSE);
        sampleTransaction.setAmount(BigDecimal.valueOf(200_000));
        sampleTransaction.setTransactionDate(LocalDate.now());
        sampleTransaction.setNote("Bua trua");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockCurrentUser() {
        when(userService.findByEmail("a@example.com")).thenReturn(sampleUser);
    }

    // convertToResTransaction
    @Nested
    @DisplayName("convertToResTransaction(Transaction)")
    class ConvertToResTransaction {

        @Test
        @DisplayName("Map all transaction fields correctly")
        void shouldMapAllFields() {
            ResTransaction result = transactionService.convertToResTransaction(sampleTransaction);

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getWalletId()).isEqualTo(10L);
            assertThat(result.getWalletName()).isEqualTo("Tien mat");
            assertThat(result.getCategoryId()).isEqualTo(5L);
            assertThat(result.getCategoryName()).isEqualTo("An uong");
            assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200_000));
            assertThat(result.getNote()).isEqualTo("Bua trua");
        }
    }

    // newTransaction
    @Nested
    @DisplayName("newTransaction(TransactionReq)")
    class NewTransaction {

        @Test
        @DisplayName("Throw BadRequestException when category type mismatches transaction type")
        void shouldThrowWhenCategoryTypeMismatch() {
            mockCurrentUser();
            TransactionReq req = new TransactionReq();
            req.setWalletId(10L);
            req.setCategoryId(6L);
            req.setType(TransactionType.EXPENSE); // EXPENSE
            req.setAmount(BigDecimal.valueOf(100_000));
            req.setTransactionDate(LocalDate.now());

            when(walletRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(sampleWallet));
            when(categoryRepository.findByIdAndUser_Id(6L, 1L)).thenReturn(Optional.of(incomeCategory)); // INCOME !=
                                                                                                         // EXPENSE

            assertThatThrownBy(() -> transactionService.newTransaction(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Loại giao dịch không khớp");
        }

        @Test
        @DisplayName("Create EXPENSE transaction and subtract wallet balance")
        void shouldCreateExpenseAndSubtractBalance() {
            mockCurrentUser();
            TransactionReq req = new TransactionReq();
            req.setWalletId(10L);
            req.setCategoryId(5L);
            req.setType(TransactionType.EXPENSE);
            req.setAmount(BigDecimal.valueOf(200_000));
            req.setTransactionDate(LocalDate.now());
            req.setNote("Bua trua");

            when(walletRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(sampleWallet));
            when(categoryRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(expenseCategory));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);
            when(walletRepository.save(any(Wallet.class))).thenReturn(sampleWallet);

            ResTransaction result = transactionService.newTransaction(req);

            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
            // wallet balance should be reduced: 1_000_000 - 200_000 = 800_000
            assertThat(sampleWallet.getMoney()).isEqualByComparingTo(BigDecimal.valueOf(800_000));
            verify(walletRepository).save(sampleWallet);
        }

        @Test
        @DisplayName("Create INCOME transaction and add to wallet balance")
        void shouldCreateIncomeAndAddBalance() {
            mockCurrentUser();
            TransactionReq req = new TransactionReq();
            req.setWalletId(10L);
            req.setCategoryId(6L);
            req.setType(TransactionType.INCOME);
            req.setAmount(BigDecimal.valueOf(5_000_000));
            req.setTransactionDate(LocalDate.now());

            Transaction incomeTransaction = new Transaction();
            incomeTransaction.setId(101L);
            incomeTransaction.setWallet(sampleWallet);
            incomeTransaction.setCategory(incomeCategory);
            incomeTransaction.setType(TransactionType.INCOME);
            incomeTransaction.setAmount(BigDecimal.valueOf(5_000_000));
            incomeTransaction.setTransactionDate(LocalDate.now());

            when(walletRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(sampleWallet));
            when(categoryRepository.findByIdAndUser_Id(6L, 1L)).thenReturn(Optional.of(incomeCategory));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(incomeTransaction);
            when(walletRepository.save(any(Wallet.class))).thenReturn(sampleWallet);

            ResTransaction result = transactionService.newTransaction(req);

            assertThat(result).isNotNull();
            // wallet balance should increase: 1_000_000 + 5_000_000 = 6_000_000
            assertThat(sampleWallet.getMoney()).isEqualByComparingTo(BigDecimal.valueOf(6_000_000));
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when wallet not found")
        void shouldThrowWhenWalletNotFound() {
            mockCurrentUser();
            TransactionReq req = new TransactionReq();
            req.setWalletId(99L);
            req.setCategoryId(5L);
            req.setType(TransactionType.EXPENSE);
            req.setAmount(BigDecimal.valueOf(100_000));
            req.setTransactionDate(LocalDate.now());

            when(walletRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.newTransaction(req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when category not found")
        void shouldThrowWhenCategoryNotFound() {
            mockCurrentUser();
            TransactionReq req = new TransactionReq();
            req.setWalletId(10L);
            req.setCategoryId(99L);
            req.setType(TransactionType.EXPENSE);
            req.setAmount(BigDecimal.valueOf(100_000));
            req.setTransactionDate(LocalDate.now());

            when(walletRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(sampleWallet));
            when(categoryRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.newTransaction(req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // getAllTransactionByUser
    @Nested
    @DisplayName("getAllTransactionByUser(Pageable)")
    class GetAllTransactionByUser {

        @Test
        @DisplayName("Return paginated transaction list for current user")
        void shouldReturnPaginatedTransactions() {
            mockCurrentUser();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Transaction> page = new PageImpl<>(List.of(sampleTransaction), pageable, 1);

            when(transactionRepository.getAllTransactionByUserId(1L, pageable)).thenReturn(page);

            ResultPaginationDTO result = transactionService.getAllTransactionByUser(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getMeta().getTotalElements()).isEqualTo(1);
            assertThat(result.getMeta().getCurrentPage()).isEqualTo(1);

            @SuppressWarnings("unchecked")
            List<ResTransaction> transactions = (List<ResTransaction>) result.getResult();
            assertThat(transactions).hasSize(1);
        }
    }

    // getDetailTransaction
    @Nested
    @DisplayName("getDetailTransaction(Long)")
    class GetDetailTransaction {

        @Test
        @DisplayName("Return transaction when found")
        void shouldReturnTransactionDetails() {
            mockCurrentUser();
            when(transactionRepository.findByIdAndUser_Id(100L, 1L)).thenReturn(Optional.of(sampleTransaction));

            ResTransaction result = transactionService.getDetailTransaction(100L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(transactionRepository.findByIdAndUser_Id(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getDetailTransaction(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // updateTransaction
    @Nested
    @DisplayName("updateTransaction(Long, TransactionReq)")
    class UpdateTransaction {

        @Test
        @DisplayName("Rollback old balance, apply new, and return updated transaction")
        void shouldUpdateTransactionAndAdjustBalance() {
            mockCurrentUser();

            // Old: EXPENSE 200k → rollback adds 200k back to wallet (1M → 1.2M)
            // New: EXPENSE 300k → subtract 300k from wallet (1.2M → 900k)
            TransactionReq req = new TransactionReq();
            req.setWalletId(10L);
            req.setCategoryId(5L);
            req.setType(TransactionType.EXPENSE);
            req.setAmount(BigDecimal.valueOf(300_000));
            req.setTransactionDate(LocalDate.now());
            req.setNote("Updated");

            when(transactionRepository.findByIdAndUser_Id(100L, 1L)).thenReturn(Optional.of(sampleTransaction));
            when(walletRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(sampleWallet));
            when(categoryRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(expenseCategory));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);
            when(walletRepository.save(any(Wallet.class))).thenReturn(sampleWallet);

            ResTransaction result = transactionService.updateTransaction(100L, req);

            assertThat(result).isNotNull();
            // net effect: +200k (rollback) -300k (new) = -100k → 1M - 100k = 900k
            assertThat(sampleWallet.getMoney()).isEqualByComparingTo(BigDecimal.valueOf(900_000));
            verify(walletRepository, times(2)).save(sampleWallet);
        }

        @Test
        @DisplayName("Throw when new category type mismatches transaction type")
        void shouldThrowWhenCategoryTypeMismatch() {
            mockCurrentUser();
            TransactionReq req = new TransactionReq();
            req.setWalletId(10L);
            req.setCategoryId(6L); // INCOME category
            req.setType(TransactionType.EXPENSE); // but requesting EXPENSE
            req.setAmount(BigDecimal.valueOf(100_000));
            req.setTransactionDate(LocalDate.now());

            when(transactionRepository.findByIdAndUser_Id(100L, 1L)).thenReturn(Optional.of(sampleTransaction));
            when(walletRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(sampleWallet));
            when(categoryRepository.findByIdAndUser_Id(6L, 1L)).thenReturn(Optional.of(incomeCategory));

            assertThatThrownBy(() -> transactionService.updateTransaction(100L, req))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    // deleteTransaction
    @Nested
    @DisplayName("deleteTransaction(Long)")
    class DeleteTransaction {

        @Test
        @DisplayName("Rollback balance and delete transaction")
        void shouldRollbackAndDelete() {
            mockCurrentUser();
            when(transactionRepository.findByIdAndUser_Id(100L, 1L)).thenReturn(Optional.of(sampleTransaction));
            when(walletRepository.save(any(Wallet.class))).thenReturn(sampleWallet);

            transactionService.deleteTransaction(100L);

            // EXPENSE rollback: +200k to wallet: 1M + 200k = 1.2M
            assertThat(sampleWallet.getMoney()).isEqualByComparingTo(BigDecimal.valueOf(1_200_000));
            verify(transactionRepository).delete(sampleTransaction);
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when transaction not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(transactionRepository.findByIdAndUser_Id(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.deleteTransaction(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
