package com.mopick.product.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationCardRepository extends JpaRepository<ConsultationCardEntity, Long> {
    List<ConsultationCardEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
