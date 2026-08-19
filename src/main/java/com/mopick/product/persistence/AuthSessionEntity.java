package com.mopick.product.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "auth_sessions")
public class AuthSessionEntity {

    @Id
    @Column(length = 64)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Instant expiresAt;

    protected AuthSessionEntity() {
    }

    public AuthSessionEntity(String token, Long userId, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
