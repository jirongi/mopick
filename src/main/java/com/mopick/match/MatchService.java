package com.mopick.match;

import com.mopick.portfolio.PortfolioEntry;
import com.mopick.portfolio.PortfolioRepository;
import com.mopick.stylespec.ConfirmedSpec;
import com.mopick.stylespec.StyleField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 결정론적 후보 매칭. AI를 한 번도 부르지 않는다.
 *
 * <p>후보 포함 여부와 순서는 전적으로 여기서 정해지고, 이후 AI Evidence Match는 이 결과를
 * 바꿀 수 없다. 같은 입력에는 항상 같은 후보가 같은 순서로 나온다.
 *
 * <pre>
 * 권리·게시 검수 → 지역 → 회피 미용사
 *   → 8개 확정 태그 점수 (중요 필드 2배)
 *   → 미용사당 최고 사례 1건으로 접기
 *   → 상위 3명
 *   → 2명 미만이면 추천하지 않음
 * </pre>
 */
@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    /** 스펙: 후보는 최대 3명. */
    private static final int MAX_CANDIDATES = 3;

    /** 스펙: 후보가 2명 미만이면 억지로 추천하지 않는다. */
    private static final int MIN_CANDIDATES = 2;

    private final PortfolioRepository portfolioRepository;

    public MatchService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    /**
     * @param region          희망 지역. null이면 지역을 따지지 않는다.
     * @param avoidStylistIds 사용자가 제외하고 싶은 미용사.
     */
    public record MatchCriteria(ConfirmedSpec goal, String region, Set<String> avoidStylistIds) {
    }

    /**
     * @param candidates 관련도 순. 계약 금액은 이 순서에 영향을 주지 않는다.
     * @param reason     후보를 내보내지 못한 이유. 후보가 있으면 null.
     */
    public record MatchResult(List<Candidate> candidates, String reason) {

        public record Candidate(
                String stylistId,
                String stylistName,
                String region,
                String portfolioId,
                String dataVersion
        ) {
        }
    }

    public MatchResult match(MatchCriteria criteria) {
        List<PortfolioEntry> pool = portfolioRepository.findMatchable().stream()
                .filter(entry -> matchesRegion(entry, criteria.region()))
                .filter(entry -> notAvoided(entry, criteria.avoidStylistIds()))
                .toList();

        if (pool.isEmpty()) {
            return new MatchResult(List.of(), "조건에 맞는 작업 사례가 없습니다.");
        }

        List<CandidateScore> scored = pool.stream()
                .map(entry -> score(criteria.goal(), entry))
                .filter(CandidateScore::isEligible)
                .toList();

        // 한 미용사가 여러 사례를 갖고 있으면 가장 근거가 좋은 한 건만 대표로 세운다.
        // 그러지 않으면 사례가 많은 미용사가 후보 3자리를 독점한다.
        List<CandidateScore> best = bestPerStylist(scored);

        if (best.size() < MIN_CANDIDATES) {
            log.info("후보 {}명 - 억지 추천하지 않고 비운다", best.size());
            return new MatchResult(List.of(),
                    "원하는 스타일과 관련된 작업 사례가 충분하지 않습니다. 조건을 넓혀 보세요.");
        }

        List<MatchResult.Candidate> candidates = best.stream()
                .limit(MAX_CANDIDATES)
                .map(this::toCandidate)
                .toList();

        if (log.isDebugEnabled()) {
            best.stream().limit(MAX_CANDIDATES).forEach(s -> log.debug(
                    "후보 {} ({}) 일치 {}/{} 커버리지 {}% 필드 {}",
                    s.entry().portfolioId(), s.entry().stylistName(),
                    s.matchedWeight(), s.comparableWeight(),
                    Math.round(s.coverage() * 100), s.matchedFields()));
        }
        return new MatchResult(candidates, null);
    }

    /**
     * 8개 필드를 확정 태그끼리 비교한다.
     *
     * <p>한쪽이라도 값이 없는 필드는 점수에 넣지 않는다. 0점으로 깎으면 "기록하지 않은 것"이
     * "다른 것"과 같은 취급을 받아, 꼼꼼히 기록한 미용사가 손해를 본다.
     */
    CandidateScore score(ConfirmedSpec goal, PortfolioEntry entry) {
        Map<StyleField, String> goalValues = goal.valueByField();
        Map<StyleField, String> portfolioValues = ConfirmedSpec.of(entry.confirmedTags()).valueByField();

        int matched = 0;
        int comparable = 0;
        List<StyleField> matchedFields = new ArrayList<>();

        for (StyleField field : StyleField.values()) {
            String g = goalValues.get(field);
            String p = portfolioValues.get(field);
            if (g == null || p == null) {
                continue;
            }
            int weight = MatchWeights.of(field);
            comparable += weight;
            if (g.equals(p)) {
                matched += weight;
                matchedFields.add(field);
            }
        }
        return new CandidateScore(entry, matched, comparable, List.copyOf(matchedFields));
    }

    /**
     * 미용사별 최고 사례만 남기고 관련도 순으로 정렬한다.
     * 정렬 기준에 계약 금액은 들어가지 않는다.
     */
    private List<CandidateScore> bestPerStylist(List<CandidateScore> scored) {
        Comparator<CandidateScore> byRelevance = Comparator
                .comparingDouble(CandidateScore::ratio).reversed()
                // 같은 비율이면 더 많은 필드를 비교할 수 있었던 쪽이 근거가 두껍다.
                .thenComparing(Comparator.comparingDouble(CandidateScore::coverage).reversed())
                // 완전 동점이면 id 순으로 고정해 매 호출 같은 순서를 보장한다.
                .thenComparing(s -> s.entry().portfolioId());

        Map<String, CandidateScore> bestByStylist = new LinkedHashMap<>();
        for (CandidateScore s : scored) {
            bestByStylist.merge(s.entry().stylistId(), s,
                    (a, b) -> byRelevance.compare(a, b) <= 0 ? a : b);
        }
        return bestByStylist.values().stream().sorted(byRelevance).toList();
    }

    private boolean matchesRegion(PortfolioEntry entry, String region) {
        if (region == null || region.isBlank()) {
            return true;
        }
        String target = entry.region() == null ? "" : entry.region();
        return target.toLowerCase(Locale.KOREAN).contains(region.trim().toLowerCase(Locale.KOREAN));
    }

    private boolean notAvoided(PortfolioEntry entry, Set<String> avoid) {
        return avoid == null || avoid.isEmpty() || !avoid.contains(entry.stylistId());
    }

    private MatchResult.Candidate toCandidate(CandidateScore s) {
        PortfolioEntry e = s.entry();
        return new MatchResult.Candidate(
                e.stylistId(), e.stylistName(), e.region(), e.portfolioId(), e.dataVersion());
    }
}
