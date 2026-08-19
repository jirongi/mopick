package com.mopick.product.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "search_query_logs")
public class SearchQueryLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String query;

    @Column(nullable = false)
    private int count;

    @Column(nullable = false)
    private Instant lastSearchedAt;

    protected SearchQueryLogEntity() {
    }

    public SearchQueryLogEntity(String query) {
        this.query = query;
        this.count = 1;
        this.lastSearchedAt = Instant.now();
    }

    public void increment() {
        this.count++;
        this.lastSearchedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getQuery() {
        return query;
    }

    public int getCount() {
        return count;
    }

    public Instant getLastSearchedAt() {
        return lastSearchedAt;
    }
}
