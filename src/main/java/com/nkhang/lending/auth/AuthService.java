package com.nkhang.lending.auth;

import com.nkhang.lending.application.LendingUser;
import com.nkhang.lending.application.LendingUserRepository;
import com.nkhang.lending.auth.dto.AuthResponse;
import com.nkhang.lending.auth.dto.LoginRequest;
import com.nkhang.lending.auth.dto.RegisterRequest;
import com.nkhang.lending.auth.dto.UserResponse;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.config.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final long ACCESS_SECONDS = 3600;
    private final LendingUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final CurrentUser currentUser;
    private final Clock clock;

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        LendingUser user;
        try {
            user = userRepository.saveAndFlush(LendingUser.customer(request.displayName().trim(), email,
                    passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        return token(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        LendingUser user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        return token(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return UserResponse.from(userRepository.findById(currentUser.id())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED)));
    }

    private AuthResponse token(LendingUser user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mini-lending").subject(user.getId().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(ACCESS_SECONDS))
                .claim("role", user.getRole().name()).build();
        String encoded = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(encoded, "Bearer", ACCESS_SECONDS, user.getId(),
                user.getDisplayName(), user.getRole().name());
    }
}
