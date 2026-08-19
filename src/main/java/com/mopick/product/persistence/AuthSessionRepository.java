package com.mopick.product.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, String> {
}
