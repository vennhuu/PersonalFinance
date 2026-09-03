package com.vennhuu.PersonalFinance.Service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vennhuu.PersonalFinance.Entity.Request.Wallet.WalletReq;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Entity.Response.Wallet.ResWallet;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Entity.Wallet;
import com.vennhuu.PersonalFinance.Enum.WalletType;
import com.vennhuu.PersonalFinance.Exception.ResourceNotFoundException;
import com.vennhuu.PersonalFinance.Repository.WalletRepository;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private UserService userService;

    @InjectMocks
    private WalletService walletService;

    private User sampleUser;
    private Wallet sampleWallet;
    private WalletReq walletReq;

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

        walletReq = new WalletReq();
        walletReq.setName("Ngan hang");
        walletReq.setType(WalletType.BANK);
        walletReq.setMoney(BigDecimal.valueOf(500_000));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockCurrentUser() {
        when(userService.findByEmail("a@example.com")).thenReturn(sampleUser);
    }

    // convertToResWallet
    @Nested
    @DisplayName("convertToResWallet(Wallet)")
    class ConvertToResWallet {

        @Test
        @DisplayName("Map all fields correctly")
        void shouldMapAllFields() {
            ResWallet result = walletService.convertToResWallet(sampleWallet);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getName()).isEqualTo("Tien mat");
            assertThat(result.getType()).isEqualTo(WalletType.CASH);
            assertThat(result.getMoney()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
        }
    }

    // addNewWallet
    @Nested
    @DisplayName("addNewWallet(WalletReq)")
    class AddNewWallet {

        @Test
        @DisplayName("Create and return new wallet for current user")
        void shouldCreateWalletSuccessfully() {
            mockCurrentUser();
            Wallet savedWallet = new Wallet();
            savedWallet.setId(20L);
            savedWallet.setName("Ngan hang");
            savedWallet.setType(WalletType.BANK);
            savedWallet.setMoney(BigDecimal.valueOf(500_000));

            when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

            ResWallet result = walletService.addNewWallet(walletReq);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(20L);
            assertThat(result.getName()).isEqualTo("Ngan hang");
            verify(walletRepository).save(any(Wallet.class));
        }
    }

    // getAllWalletByUserId
    @Nested
    @DisplayName("getAllWalletByUserId(Pageable)")
    class GetAllWalletByUserId {

        @Test
        @DisplayName("Return paginated wallet list for current user")
        void shouldReturnPaginatedWallets() {
            mockCurrentUser();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Wallet> page = new PageImpl<>(List.of(sampleWallet), pageable, 1);

            when(walletRepository.getAllWalletByUserId(1L, pageable)).thenReturn(page);

            ResultPaginationDTO result = walletService.getAllWalletByUserId(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getMeta()).isNotNull();
            assertThat(result.getMeta().getTotalElements()).isEqualTo(1);
            assertThat(result.getMeta().getCurrentPage()).isEqualTo(1);
            assertThat(result.getMeta().getPageSize()).isEqualTo(10);

            @SuppressWarnings("unchecked")
            List<ResWallet> wallets = (List<ResWallet>) result.getResult();
            assertThat(wallets).hasSize(1);
            assertThat(wallets.get(0).getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Return empty result when no wallets")
        void shouldReturnEmptyResultWhenNoWallets() {
            mockCurrentUser();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Wallet> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(walletRepository.getAllWalletByUserId(1L, pageable)).thenReturn(emptyPage);

            ResultPaginationDTO result = walletService.getAllWalletByUserId(pageable);

            assertThat(result.getMeta().getTotalElements()).isEqualTo(0);
        }
    }

    // getDetailWallet
    @Nested
    @DisplayName("getDetailWallet(Long)")
    class GetDetailWallet {

        @Test
        @DisplayName("Return wallet when found for user")
        void shouldReturnWalletDetails() {
            mockCurrentUser();
            when(walletRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(sampleWallet));

            ResWallet result = walletService.getDetailWallet(10L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when wallet not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(walletRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.getDetailWallet(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // updateWallet
    @Nested
    @DisplayName("updateWallet(Long, WalletReq)")
    class UpdateWallet {

        @Test
        @DisplayName("Update wallet fields and return updated result")
        void shouldUpdateWalletSuccessfully() {
            mockCurrentUser();
            when(walletRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(sampleWallet));

            Wallet updated = new Wallet();
            updated.setId(10L);
            updated.setName("Ngan hang");
            updated.setType(WalletType.BANK);
            updated.setMoney(BigDecimal.valueOf(500_000));
            when(walletRepository.save(any(Wallet.class))).thenReturn(updated);

            ResWallet result = walletService.updateWallet(10L, walletReq);

            assertThat(result.getName()).isEqualTo("Ngan hang");
            assertThat(result.getType()).isEqualTo(WalletType.BANK);
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when wallet not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(walletRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.updateWallet(99L, walletReq))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // deleteWallet
    @Nested
    @DisplayName("deleteWallet(Long)")
    class DeleteWallet {

        @Test
        @DisplayName("Delete wallet when found for user")
        void shouldDeleteWalletSuccessfully() {
            mockCurrentUser();
            when(walletRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(sampleWallet));

            walletService.deleteWallet(10L);

            verify(walletRepository).delete(sampleWallet);
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when wallet not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(walletRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.deleteWallet(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
