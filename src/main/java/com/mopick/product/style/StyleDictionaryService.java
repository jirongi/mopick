package com.mopick.product.style;

import com.mopick.product.persistence.SearchQueryLogRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class StyleDictionaryService {

    private static final Logger log = LoggerFactory.getLogger(StyleDictionaryService.class);
    private static final String FIXTURE = "data/style-dictionary.json";

    private final List<StyleDictionaryEntry> entries;
    private final SearchQueryLogRepository searchQueryLogRepository;

    public StyleDictionaryService(ObjectMapper objectMapper, SearchQueryLogRepository searchQueryLogRepository) {
        this.entries = load(objectMapper);
        this.searchQueryLogRepository = searchQueryLogRepository;
        log.info("스타일 사전 fixture 적재: {}건", entries.size());
    }

    public List<StyleDictionaryEntry> findAll(StyleCategory category) {
        if (category == null || category == StyleCategory.ALL) {
            return entries;
        }
        return entries.stream().filter(e -> e.category() == category).toList();
    }

    public Optional<StyleDictionaryEntry> findById(String id) {
        return entries.stream().filter(e -> e.id().equals(id)).findFirst();
    }

    @Transactional
    public List<StyleDictionaryEntry> search(String query, StyleCategory category) {
        if (query == null || query.isBlank()) {
            return findAll(category);
        }
        String normalized = query.trim().toLowerCase(Locale.KOREAN);
        searchQueryLogRepository.record(normalized);

        return entries.stream()
                .filter(e -> category == null || category == StyleCategory.ALL || e.category() == category)
                .filter(e -> matches(e, normalized))
                .sorted(Comparator.comparingInt(StyleDictionaryEntry::popularRank))
                .toList();
    }

    public List<String> popularQueries(int limit) {
        List<String> fromDb = searchQueryLogRepository.topQueries(limit);
        if (!fromDb.isEmpty()) {
            return fromDb;
        }
        return entries.stream()
                .sorted(Comparator.comparingInt(StyleDictionaryEntry::popularRank))
                .map(StyleDictionaryEntry::name)
                .limit(limit)
                .toList();
    }

    public List<StyleDictionaryEntry> recommended(int limit) {
        return entries.stream()
                .sorted(Comparator.comparingInt(StyleDictionaryEntry::popularRank))
                .limit(limit)
                .toList();
    }

    private boolean matches(StyleDictionaryEntry entry, String normalized) {
        if (entry.name().toLowerCase(Locale.KOREAN).contains(normalized)) {
            return true;
        }
        if (entry.aliases() != null) {
            for (String alias : entry.aliases()) {
                if (alias.toLowerCase(Locale.KOREAN).contains(normalized)) {
                    return true;
                }
            }
        }
        return entry.styleFamily() != null
                && entry.styleFamily().toLowerCase(Locale.KOREAN).contains(normalized);
    }

    private List<StyleDictionaryEntry> load(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(FIXTURE).getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<StyleDictionaryEntry>>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException(FIXTURE + " 적재 실패", e);
        }
    }
}
