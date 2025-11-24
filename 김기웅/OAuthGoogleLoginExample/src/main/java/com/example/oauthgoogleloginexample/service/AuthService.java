package com.example.oauthgoogleloginexample.service;

import com.example.oauthgoogleloginexample.domain.RefreshToken;
import com.example.oauthgoogleloginexample.domain.UserAccount;
import com.example.oauthgoogleloginexample.dto.auth.GoogleLoginRequest;
import com.example.oauthgoogleloginexample.dto.auth.LoginRequest;
import com.example.oauthgoogleloginexample.dto.auth.SignUpRequest;
import com.example.oauthgoogleloginexample.dto.auth.TokenResponse;
import com.example.oauthgoogleloginexample.repository.RefreshTokenRepository;
import com.example.oauthgoogleloginexample.repository.UserAccountRepository;
import com.example.oauthgoogleloginexample.security.JwtTokenProvider;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleOAuthService googleOAuthService;

    @Transactional
    public void signUp(SignUpRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("username already exists");
        }

        UserAccount account = UserAccount.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                // role 값은 UserAccount의 @PrePersist 기본 설정에 맡깁니다.
                .build();

        userAccountRepository.save(account);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserAccount user = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("invalid username or password"));

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse loginWithGoogle(GoogleLoginRequest request) {
        if (!StringUtils.hasText(request.getIdToken())) {
            throw new IllegalArgumentException("idToken is required");
        }

        GoogleOAuthService.GoogleUser googleUser =
                googleOAuthService.verifyIdToken(request.getIdToken());

        UserAccount user = userAccountRepository.findByUsername(googleUser.email())
                .orElseGet(() -> registerGoogleUser(googleUser));

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse issueTokens(UserAccount user) {
        String access = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refresh = jwtTokenProvider.generateRefreshToken(user.getUsername());

        // 기존 리프레시 토큰 삭제 후 새 토큰 발급
        refreshTokenRepository.deleteByUser(user);

        RefreshToken entity = RefreshToken.builder()
                .token(refresh)
                .user(user)
                .expiresAt(
                        Instant.now()
                                .plusSeconds(jwtTokenProvider.getRefreshValiditySeconds())
                )
                // revoked 필드는 현재 로직에서 사용하지 않으므로 명시적으로 세팅하지 않습니다.
                .build();

        refreshTokenRepository.save(entity);

        return TokenResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessValiditySeconds())
                .build();
    }

    private UserAccount registerGoogleUser(GoogleOAuthService.GoogleUser googleUser) {
        UserAccount account = UserAccount.builder()
                .username(googleUser.email())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                // role 값은 UserAccount의 @PrePersist 기본 설정에 맡깁니다.
                .build();

        return userAccountRepository.save(account);
    }
}
