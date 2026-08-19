package com.mopick.product.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "stylist_registrations")
public class StylistRegistrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicantName;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 120)
    private String salonName;

    @Column(nullable = false, length = 120)
    private String region;

    @Lob
    private String introduction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected StylistRegistrationEntity() {
    }

    public StylistRegistrationEntity(String applicantName, String email, String salonName,
                                     String region, String introduction) {
        this.applicantName = applicantName;
        this.email = email;
        this.salonName = salonName;
        this.region = region;
        this.introduction = introduction;
        this.status = RegistrationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public String getEmail() {
        return email;
    }

    public String getSalonName() {
        return salonName;
    }

    public String getRegion() {
        return region;
    }

    public String getIntroduction() {
        return introduction;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
