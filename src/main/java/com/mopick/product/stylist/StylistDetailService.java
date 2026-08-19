package com.mopick.product.stylist;

import com.mopick.portfolio.PortfolioEntry;
import com.mopick.portfolio.PortfolioRepository;
import com.mopick.stylespec.ConfirmedTag;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class StylistDetailService {

    private static final Logger log = LoggerFactory.getLogger(StylistDetailService.class);
    private static final String PROFILES = "data/stylist-profiles.json";

    private final PortfolioRepository portfolioRepository;
    private final Map<String, StylistProfileFixture> profiles;

    public StylistDetailService(PortfolioRepository portfolioRepository, ObjectMapper objectMapper) {
        this.portfolioRepository = portfolioRepository;
        this.profiles = loadProfiles(objectMapper);
    }

    public Optional<StylistDetailResponse> findByStylistId(String stylistId) {
        List<PortfolioEntry> entries = portfolioRepository.findAll().stream()
                .filter(e -> stylistId.equals(e.stylistId()))
                .sorted(Comparator.comparing(PortfolioEntry::portfolioId))
                .toList();
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        PortfolioEntry first = entries.get(0);
        StylistProfileFixture profile = profiles.get(stylistId);
        return Optional.of(new StylistDetailResponse(
                stylistId,
                first.stylistName(),
                profile != null ? profile.salonName() : regionToSalon(first.region()),
                profile != null ? profile.title() : "디자이너",
                profile != null ? profile.introduction() : defaultIntro(first),
                first.region(),
                profile != null ? profile.naverPlaceId() : null,
                entries.stream().map(this::toPortfolioSummary).toList()));
    }

    public Optional<PortfolioDetailResponse> findPortfolio(String stylistId, String portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .filter(e -> stylistId.equals(e.stylistId()))
                .map(this::toPortfolioDetail);
    }

    private PortfolioSummary toPortfolioSummary(PortfolioEntry entry) {
        return new PortfolioSummary(
                entry.portfolioId(),
                entry.isMatchable(),
                entry.imagePath(),
                entry.dataVersion(),
                entry.stylistMetadata() != null ? entry.stylistMetadata().serviceSummary() : null);
    }

    private PortfolioDetailResponse toPortfolioDetail(PortfolioEntry entry) {
        return new PortfolioDetailResponse(
                entry.portfolioId(),
                entry.stylistId(),
                entry.stylistName(),
                entry.region(),
                entry.isMatchable(),
                entry.imagePath(),
                entry.dataVersion(),
                entry.confirmedTags(),
                entry.stylistMetadata());
    }

    private String regionToSalon(String region) {
        if (region == null || region.isBlank()) {
            return "미등록 샵";
        }
        return region + " 헤어";
    }

    private String defaultIntro(PortfolioEntry entry) {
        if (entry.stylistMetadata() != null && entry.stylistMetadata().serviceSummary() != null) {
            return entry.stylistMetadata().serviceSummary();
        }
        return "디자이너 소개가 준비 중입니다.";
    }

    private Map<String, StylistProfileFixture> loadProfiles(ObjectMapper objectMapper) {
        Map<String, StylistProfileFixture> map = new LinkedHashMap<>();
        try (InputStream in = new ClassPathResource(PROFILES).getInputStream()) {
            List<StylistProfileFixture> list = objectMapper.readValue(in, new TypeReference<List<StylistProfileFixture>>() {
            });
            for (StylistProfileFixture profile : list) {
                map.put(profile.stylistId(), profile);
            }
            log.info("디자이너 프로필 fixture 적재: {}건", map.size());
        } catch (IOException e) {
            log.warn("디자이너 프로필 fixture 없음 - 기본값 사용: {}", e.toString());
        }
        return map;
    }

    public record StylistDetailResponse(
            String stylistId,
            String stylistName,
            String salonName,
            String title,
            String introduction,
            String region,
            String naverPlaceId,
            List<PortfolioSummary> portfolios
    ) {
    }

    public record PortfolioSummary(
            String portfolioId,
            boolean matchable,
            String imagePath,
            String dataVersion,
            String serviceSummary
    ) {
    }

    public record PortfolioDetailResponse(
            String portfolioId,
            String stylistId,
            String stylistName,
            String region,
            boolean matchable,
            String imagePath,
            String dataVersion,
            List<ConfirmedTag> confirmedTags,
            PortfolioEntry.StylistMetadata stylistMetadata
    ) {
    }
}
