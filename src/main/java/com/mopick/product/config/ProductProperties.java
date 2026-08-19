package com.mopick.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mopick.product")
public record ProductProperties(Auth auth) {

    public record Auth(boolean devMode) {
    }
}
