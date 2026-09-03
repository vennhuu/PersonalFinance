package com.vennhuu.PersonalFinance.Service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vennhuu.PersonalFinance.Entity.Category;
import com.vennhuu.PersonalFinance.Entity.Response.Summary.ResCategoryStat;
import com.vennhuu.PersonalFinance.Entity.Response.Summary.ResSummary;
import com.vennhuu.PersonalFinance.Entity.Response.Summary.ResTrendPoint;
import com.vennhuu.PersonalFinance.Entity.Transaction;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Enum.TransactionType;
import com.vennhuu.PersonalFinance.Repository.TransactionRepository;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private UserService userService;

    @InjectMocks
    private ReportService reportService;

    private User sampleUser;
    private Category foodCategory;
    private Category salaryCategory;
    private Transaction expenseTx1;
    private Transaction expenseTx2;
    private Transaction incomeTx;

    private static final LocalDate DATE_1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate DATE_2 = LocalDate.of(2026, 9, 2);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("a@example.com", null));

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("a@example.com");

        foodCategory = new Category();
        foodCategory.setId(10L);
        foodCategory.setName("Food");
        foodCategory.setType(TransactionType.EXPENSE);

        salaryCategory = new Category();
        salaryCategory.setId(20L);
        salaryCategory.setName("Salary");
        salaryCategory.setType(TransactionType.INCOME);

        expenseTx1 = new Transaction();
        expenseTx1.setId(1L);
        expenseTx1.setUser(sampleUser);
        expenseTx1.setCategory(foodCategory);
        expenseTx1.setType(TransactionType.EXPENSE);
        expenseTx1.setAmount(BigDecimal.valueOf(100_000));
        expenseTx1.setTransactionDate(DATE_1);

        expenseTx2 = new Transaction();
        expenseTx2.setId(2L);
        expenseTx2.setUser(sampleUser);
        expenseTx2.setCategory(foodCategory);
        expenseTx2.setType(TransactionType.EXPENSE);
        expenseTx2.setAmount(BigDecimal.valueOf(150_000));
        expenseTx2.setTransactionDate(DATE_2);

        incomeTx = new Transaction();
        incomeTx.setId(3L);
        incomeTx.setUser(sampleUser);
        incomeTx.setCategory(salaryCategory);
        incomeTx.setType(TransactionType.INCOME);
        incomeTx.setAmount(BigDecimal.valueOf(1_000_000));
        incomeTx.setTransactionDate(DATE_1);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockCurrentUser() {
        when(userService.findByEmail("a@example.com")).thenReturn(sampleUser);
    }

    // getSummary
    @Nested
    @DisplayName("getSummary(LocalDate, LocalDate)")
    class GetSummary {

        @Test
        @DisplayName("Calculate total income, expense, and balance correctly")
        void shouldCalculateSummaryCorrectly() {
            mockCurrentUser();
            when(transactionRepository.findByUser_IdAndTransactionDateBetween(1L, DATE_1, DATE_2))
                    .thenReturn(List.of(expenseTx1, expenseTx2, incomeTx));

            ResSummary summary = reportService.getSummary(DATE_1, DATE_2);

            assertThat(summary).isNotNull();
            assertThat(summary.getTotalIncome()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
            assertThat(summary.getTotalExpense()).isEqualByComparingTo(BigDecimal.valueOf(250_000));
            assertThat(summary.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(750_000));
        }

        @Test
        @DisplayName("Return zeros when there are no transactions")
        void shouldReturnZerosWhenNoTransactions() {
            mockCurrentUser();
            when(transactionRepository.findByUser_IdAndTransactionDateBetween(1L, DATE_1, DATE_2))
                    .thenReturn(List.of());

            ResSummary summary = reportService.getSummary(DATE_1, DATE_2);

            assertThat(summary.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.getTotalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Throw BadCredentialsException when not logged in")
        void shouldThrowWhenNotLoggedIn() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> reportService.getSummary(DATE_1, DATE_2))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    // getStatsByCategory
    @Nested
    @DisplayName("getStatsByCategory(LocalDate, LocalDate, TransactionType)")
    class GetStatsByCategory {

        @Test
        @DisplayName("Group and sort category stats by total amount descending")
        void shouldGroupAndSortStatsDescending() {
            mockCurrentUser();

            Category transportCategory = new Category();
            transportCategory.setId(11L);
            transportCategory.setName("Transport");
            transportCategory.setType(TransactionType.EXPENSE);

            Transaction transportTx = new Transaction();
            transportTx.setId(4L);
            transportTx.setUser(sampleUser);
            transportTx.setCategory(transportCategory);
            transportTx.setType(TransactionType.EXPENSE);
            transportTx.setAmount(BigDecimal.valueOf(300_000)); // larger than Food (250_000)
            transportTx.setTransactionDate(DATE_1);

            when(transactionRepository.findByUser_IdAndTransactionDateBetween(1L, DATE_1, DATE_2))
                    .thenReturn(List.of(expenseTx1, expenseTx2, transportTx, incomeTx));

            List<ResCategoryStat> stats = reportService.getStatsByCategory(DATE_1, DATE_2, TransactionType.EXPENSE);

            assertThat(stats).hasSize(2);
            // First item should be Transport (300k > 250k)
            assertThat(stats.get(0).getCategoryName()).isEqualTo("Transport");
            assertThat(stats.get(0).getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(300_000));
            // Second item should be Food (100k + 150k = 250k)
            assertThat(stats.get(1).getCategoryName()).isEqualTo("Food");
            assertThat(stats.get(1).getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(250_000));
        }

        @Test
        @DisplayName("Filter correctly by type (INCOME)")
        void shouldFilterOnlyIncome() {
            mockCurrentUser();
            when(transactionRepository.findByUser_IdAndTransactionDateBetween(1L, DATE_1, DATE_2))
                    .thenReturn(List.of(expenseTx1, incomeTx));

            List<ResCategoryStat> stats = reportService.getStatsByCategory(DATE_1, DATE_2, TransactionType.INCOME);

            assertThat(stats).hasSize(1);
            assertThat(stats.get(0).getCategoryName()).isEqualTo("Salary");
            assertThat(stats.get(0).getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
        }
    }

    // getTrend
    @Nested
    @DisplayName("getTrend(LocalDate, LocalDate)")
    class GetTrend {

        @Test
        @DisplayName("Group by date and sort chronologically")
        void shouldGroupAndSortChronologically() {
            mockCurrentUser();
            when(transactionRepository.findByUser_IdAndTransactionDateBetween(1L, DATE_1, DATE_2))
                    .thenReturn(List.of(expenseTx1, expenseTx2, incomeTx));

            List<ResTrendPoint> trend = reportService.getTrend(DATE_1, DATE_2);

            assertThat(trend).hasSize(2);
            // DATE_1: income 1M, expense 100k
            assertThat(trend.get(0).getDate()).isEqualTo(DATE_1);
            assertThat(trend.get(0).getIncome()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
            assertThat(trend.get(0).getExpense()).isEqualByComparingTo(BigDecimal.valueOf(100_000));

            // DATE_2: income 0, expense 150k
            assertThat(trend.get(1).getDate()).isEqualTo(DATE_2);
            assertThat(trend.get(1).getIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(trend.get(1).getExpense()).isEqualByComparingTo(BigDecimal.valueOf(150_000));
        }
    }
}
