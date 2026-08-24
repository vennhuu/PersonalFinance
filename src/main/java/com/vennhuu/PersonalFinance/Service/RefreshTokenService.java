package com.vennhuu.PersonalFinance.Service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vennhuu.PersonalFinance.Entity.RefreshToken;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Repository.RefreshTokenRepository;
import com.vennhuu.PersonalFinance.Repository.UserRepository;

@Service
public class RefreshTokenService {
    
    @Value("${venn.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void createToken(String token, String email, String device) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return;
        }
        saveToken(token, user, device);
    }

    public void createToken(String token, User user) {
        saveToken(token, user, null);
    }

    public void revokeToken(String token) {
        refreshTokenRepository.findByTokenHash(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByTokenHash(token).orElse(null);
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByTokenHash(token);
    }

    private void saveToken(String token, User user, String device) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(token);
        refreshToken.setUser(user);
        refreshToken.setDevice(device);
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpiration));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
    }
}
