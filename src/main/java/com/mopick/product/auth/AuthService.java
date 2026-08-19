package com.mopick.product.auth;

import com.mopick.api.ApiException;
import com.mopick.product.config.NaverProperties;
import com.mopick.product.config.ProductProperties;
import com.mopick.product.persistence.AuthProvider;
import com.mopick.product.persistence.AuthSessionEntity;
import com.mopick.product.persistence.AuthSessionRepository;
import com.mopick.product.persistence.UserEntity;
import com.mopick.product.persistence.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final long TOKEN_TTL_HOURS = 24;

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final NaverProperties naverProperties;
    private final ProductProperties productProperties;

    public AuthService(UserRepository userRepository,
                       AuthSessionRepository authSessionRepository,
                       NaverProperties naverProperties,
                       ProductProperties productProperties) {
        this.userRepository = userRepository;
        this.authSessionRepository = authSessionRepository;
        this.naverProperties = naverProperties;
        this.productProperties = productProperties;
    }

    public String buildNaverAuthorizeUrl() {
        if (!naverProperties.configured()) {
            throw new ApiException("NAVER_NOT_CONFIGURED",
                    "네이버 OAuth가 설정되지 않았습니다. dev-mode에서는 /api/product/auth/dev/login 을 사용하세요.");
        }
        return "https://nid.naver.com/oauth2.0/authorize?response_type=code"
                + "&client_id=" + naverProperties.clientId()
                + "&redirect_uri=" + naverProperties.redirectUri()
                + "&state=" + UUID.randomUUID();
    }

    @Transactional
    public AuthResponse completeNaverCallback(String code, String email, String name) {
        if (!naverProperties.configured()) {
            if (!productProperties.auth().devMode()) {
                throw new ApiException("NAVER_NOT_CONFIGURED", "네이버 OAuth 설정이 필요합니다.");
            }
            return loginDev(email != null ? email : "naver-user@example.com",
                    name != null ? name : "네이버 사용자", AuthProvider.NAVER, code);
        }
        // 실제 토큰 교환은 키가 있을 때 붙인다. 지금은 콜백 파라미터로 사용자 정보를 받는 MVP 스텁.
        return loginDev(
                email != null ? email : "naver-" + UUID.randomUUID() + "@users.mopick.local",
                name != null ? name : "네이버 사용자",
                AuthProvider.NAVER,
                code);
    }

    @Transactional
    public AuthResponse loginDev(String email, String name) {
        if (!productProperties.auth().devMode()) {
            throw new ApiException("DEV_AUTH_DISABLED", "개발용 로그인이 비활성화되어 있습니다.");
        }
        return loginDev(email, name, AuthProvider.DEV, "dev");
    }

    public Optional<UserEntity> resolveUser(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return Optional.empty();
        }
        String token = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken;
        return authSessionRepository.findById(token)
                .filter(session -> !session.isExpired())
                .flatMap(session -> userRepository.findById(session.getUserId()));
    }

    public UserEntity requireUser(String bearerToken) {
        return resolveUser(bearerToken)
                .orElseThrow(() -> new ApiException("UNAUTHORIZED", "로그인이 필요합니다."));
    }

    private AuthResponse loginDev(String email, String name, AuthProvider provider, String providerUserId) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new UserEntity(email, name, provider, providerUserId)));
        String token = UUID.randomUUID().toString().replace("-", "");
        authSessionRepository.save(new AuthSessionEntity(
                token, user.getId(), Instant.now().plus(TOKEN_TTL_HOURS, ChronoUnit.HOURS)));
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), provider.name());
    }

    public record AuthResponse(String accessToken, Long userId, String email, String name, String provider) {
    }
}
