package com.mopick.product.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "consultation_cards")
public class ConsultationCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 120)
    private String title;

    @Column(length = 64)
    private String stylistId;

    @Column(length = 64)
    private String portfolioId;

    @Column(length = 120)
    private String stylistName;

    @Column(length = 120)
    private String salonName;

    @Lob
    @Column(nullable = false)
    private String confirmedTagsJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected ConsultationCardEntity() {
    }

    public ConsultationCardEntity(Long userId, String title, String stylistId, String portfolioId,
                                  String stylistName, String salonName, String confirmedTagsJson) {
        this.userId = userId;
        this.title = title;
        this.stylistId = stylistId;
        this.portfolioId = portfolioId;
        this.stylistName = stylistName;
        this.salonName = salonName;
        this.confirmedTagsJson = confirmedTagsJson;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getStylistId() {
        return stylistId;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public String getStylistName() {
        return stylistName;
    }

    public String getSalonName() {
        return salonName;
    }

    public String getConfirmedTagsJson() {
        return confirmedTagsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
