package com.mopick.product.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.mopick.product.persistence")
@EntityScan(basePackages = "com.mopick.product.persistence")
@EnableConfigurationProperties({
        ProductProperties.class,
        NaverProperties.class
})
public class ProductJpaConfig {
}