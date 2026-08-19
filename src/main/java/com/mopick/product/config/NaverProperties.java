package com.mopick.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mopick.product.naver")
public record NaverProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String bookingBaseUrl
) {
    public boolean configured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
