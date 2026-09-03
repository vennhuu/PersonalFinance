package com.vennhuu.PersonalFinance.Service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.vennhuu.PersonalFinance.Entity.RefreshToken;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Repository.RefreshTokenRepository;
import com.vennhuu.PersonalFinance.Repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User sampleUser;
    private RefreshToken sampleRefreshToken;

    @BeforeEach
    void setUp() {
        // Inject @Value field manually
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 604800L);

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("a@example.com");

        sampleRefreshToken = new RefreshToken();
        sampleRefreshToken.setId(1L);
        sampleRefreshToken.setTokenHash("sample-token");
        sampleRefreshToken.setUser(sampleUser);
        sampleRefreshToken.setRevoked(false);
        sampleRefreshToken.setCreatedAt(Instant.now());
        sampleRefreshToken.setExpiresAt(Instant.now().plusSeconds(604800));
    }

    // createToken
    @Nested
    @DisplayName("createToken(String token, String email, String device)")
    class CreateTokenByEmail {

        @Test
        @DisplayName("Save token with device info when user found by email")
        void shouldSaveTokenWhenUserFound() {
            when(userRepository.findByEmail("a@example.com")).thenReturn(sampleUser);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(sampleRefreshToken);

            refreshTokenService.createToken("my-token", "a@example.com", "Chrome/Windows");

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());

            RefreshToken saved = captor.getValue();
            assertThat(saved.getTokenHash()).isEqualTo("my-token");
            assertThat(saved.getUser()).isEqualTo(sampleUser);
            assertThat(saved.getDevice()).isEqualTo("Chrome/Windows");
            assertThat(saved.isRevoked()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("Return early without saving when user not found by email")
        void shouldReturnEarlyWhenUserNotFound() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(null);

            refreshTokenService.createToken("some-token", "ghost@example.com", "Firefox");

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // createToken(String, User)
    @Nested
    @DisplayName("createToken(String token, User user)")
    class CreateTokenByUser {

        @Test
        @DisplayName("Save token with null device when called with User object")
        void shouldSaveTokenWithNullDevice() {
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(sampleRefreshToken);

            refreshTokenService.createToken("direct-token", sampleUser);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());

            RefreshToken saved = captor.getValue();
            assertThat(saved.getTokenHash()).isEqualTo("direct-token");
            assertThat(saved.getUser()).isEqualTo(sampleUser);
            assertThat(saved.getDevice()).isNull();
        }
    }

    // revokeToken
    @Nested
    @DisplayName("revokeToken(String)")
    class RevokeToken {

        @Test
        @DisplayName("Set revoked=true and save when token exists")
        void shouldRevokeExistingToken() {
            when(refreshTokenRepository.findByTokenHash("sample-token"))
                    .thenReturn(Optional.of(sampleRefreshToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(sampleRefreshToken);

            refreshTokenService.revokeToken("sample-token");

            assertThat(sampleRefreshToken.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(sampleRefreshToken);
        }

        @Test
        @DisplayName("Do nothing when token does not exist")
        void shouldDoNothingWhenTokenNotFound() {
            when(refreshTokenRepository.findByTokenHash("unknown-token"))
                    .thenReturn(Optional.empty());

            refreshTokenService.revokeToken("unknown-token");

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // findByToken
    @Nested
    @DisplayName("findByToken(String)")
    class FindByToken {

        @Test
        @DisplayName("Return RefreshToken when token found")
        void shouldReturnTokenWhenFound() {
            when(refreshTokenRepository.findByTokenHash("sample-token"))
                    .thenReturn(Optional.of(sampleRefreshToken));

            RefreshToken result = refreshTokenService.findByToken("sample-token");

            assertThat(result).isNotNull();
            assertThat(result.getTokenHash()).isEqualTo("sample-token");
            assertThat(result.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("Return null when token not found")
        void shouldReturnNullWhenNotFound() {
            when(refreshTokenRepository.findByTokenHash("no-such-token"))
                    .thenReturn(Optional.empty());

            RefreshToken result = refreshTokenService.findByToken("no-such-token");

            assertThat(result).isNull();
        }
    }

    // deleteByToken
    @Nested
    @DisplayName("deleteByToken(String)")
    class DeleteByToken {

        @Test
        @DisplayName("Delegate to repository deleteByTokenHash")
        void shouldDelegateToRepository() {
            refreshTokenService.deleteByToken("token-to-delete");

            verify(refreshTokenRepository).deleteByTokenHash("token-to-delete");
        }
    }
}
