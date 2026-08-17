package com.mopick.portfolio;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 인메모리 포트폴리오 저장소.
 *
 * <p>DB를 붙이기 전 단계다. AI 계약이 굳은 뒤 같은 인터페이스로 JPA 구현을 갈아끼운다.
 * 현재 fixture 이미지는 AI 생성 가상 이미지이며 실제 인물·작업물이 아니다.
 */
@Repository
public class PortfolioRepository {

    private static final Logger log = LoggerFactory.getLogger(PortfolioRepository.class);
    private static final String FIXTURE = "data/portfolios.json";

    private final Map<String, PortfolioEntry> byId = new LinkedHashMap<>();

    public PortfolioRepository(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(FIXTURE).getInputStream()) {
            List<PortfolioEntry> entries = objectMapper.readValue(in, new TypeReference<List<PortfolioEntry>>() {
            });
            for (PortfolioEntry e : entries) {
                byId.put(e.portfolioId(), e);
            }
            log.info("포트폴리오 fixture 적재 완료: {}건 (매칭 가능 {}건)", byId.size(), findMatchable().size());
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(FIXTURE + " 적재 실패", e);
        }
    }

    public Optional<PortfolioEntry> findById(String portfolioId) {
        return Optional.ofNullable(byId.get(portfolioId));
    }

    /** 권리·게시 검수를 통과한 사례만. */
    public List<PortfolioEntry> findMatchable() {
        return byId.values().stream().filter(PortfolioEntry::isMatchable).toList();
    }

    public List<PortfolioEntry> findAll() {
        return List.copyOf(byId.values());
    }
}
