package com.mopick.match;

import com.mopick.portfolio.PortfolioEntry;
import com.mopick.portfolio.PortfolioRepository;
import com.mopick.stylespec.ConfirmedSpec;
import com.mopick.stylespec.StyleField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 기존 {@link MatchService} 결과에 PDF용 한 줄 요약을 더한다.
 * {@link MatchService} 본체는 수정하지 않는다.
 */
@Service
public class MatchEnrichmentService {

    private final MatchService matchService;
    private final PortfolioRepository portfolioRepository;
    private final MatchSummaryGenerator summaryGenerator;

    public MatchEnrichmentService(MatchService matchService,
                                  PortfolioRepository portfolioRepository,
                                  MatchSummaryGenerator summaryGenerator) {
        this.matchService = matchService;
        this.portfolioRepository = portfolioRepository;
        this.summaryGenerator = summaryGenerator;
    }

    public EnrichedMatchResult match(MatchService.MatchCriteria criteria) {
        MatchService.MatchResult base = matchService.match(criteria);
        List<EnrichedCandidate> enriched = new ArrayList<>();
        for (MatchService.MatchResult.Candidate candidate : base.candidates()) {
            portfolioRepository.findById(candidate.portfolioId()).ifPresent(entry -> {
                CandidateScore score = matchService.score(criteria.goal(), entry);
                enriched.add(new EnrichedCandidate(
                        candidate.stylistId(),
                        candidate.stylistName(),
                        candidate.region(),
                        candidate.portfolioId(),
                        candidate.dataVersion(),
                        summaryGenerator.summarize(score, criteria.goal()),
                        score.matchedFields(),
                        mismatchedFields(criteria.goal(), entry, score.matchedFields())));
            });
        }
        return new EnrichedMatchResult(base.reason(), List.copyOf(enriched));
    }

    private List<StyleField> mismatchedFields(ConfirmedSpec goal, PortfolioEntry entry, List<StyleField> matched) {
        Map<StyleField, String> goalValues = goal.valueByField();
        Map<StyleField, String> portfolioValues = ConfirmedSpec.of(entry.confirmedTags()).valueByField();
        Set<StyleField> matchedSet = Set.copyOf(matched);
        List<StyleField> mismatched = new ArrayList<>();
        for (StyleField field : StyleField.values()) {
            String g = goalValues.get(field);
            String p = portfolioValues.get(field);
            if (g != null && p != null && !matchedSet.contains(field)) {
                mismatched.add(field);
            }
        }
        return mismatched;
    }

    public record EnrichedMatchResult(String reason, List<EnrichedCandidate> candidates) {
    }

    public record EnrichedCandidate(
            String stylistId,
            String stylistName,
            String region,
            String portfolioId,
            String dataVersion,
            String summary,
            List<StyleField> matchedFields,
            List<StyleField> mismatchedFields
    ) {
    }
}
