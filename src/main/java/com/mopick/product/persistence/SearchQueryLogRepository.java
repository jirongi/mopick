package com.mopick.product.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SearchQueryLogRepository extends JpaRepository<SearchQueryLogEntity, Long> {

    java.util.Optional<SearchQueryLogEntity> findByQuery(String query);

    @Query("select s.query from SearchQueryLogEntity s order by s.count desc, s.lastSearchedAt desc")
    List<String> topQueries(org.springframework.data.domain.Pageable pageable);

    default void record(String query) {
        SearchQueryLogEntity entity = findByQuery(query)
                .map(existing -> {
                    existing.increment();
                    return existing;
                })
                .orElseGet(() -> new SearchQueryLogEntity(query));
        save(entity);
    }

    default List<String> topQueries(int limit) {
        return topQueries(org.springframework.data.domain.PageRequest.of(0, limit));
    }
}
