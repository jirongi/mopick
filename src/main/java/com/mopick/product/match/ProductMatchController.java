package com.mopick.product.match;

import com.mopick.api.dto.MatchRequest;
import com.mopick.match.MatchEnrichmentService;
import com.mopick.match.MatchService;
import com.mopick.stylespec.ConfirmedSpec;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PDF용 한 줄 요약이 포함된 매칭 API. 기존 {@code /api/matches}는 그대로 둔다. */
@RestController
@RequestMapping("/api/product/matches")
public class ProductMatchController {

    private final MatchEnrichmentService enrichmentService;

    public ProductMatchController(MatchEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    @PostMapping
    public EnrichedMatchResponse match(@RequestBody MatchRequest request) {
        String analysisId = UUID.randomUUID().toString();
        var result = enrichmentService.match(new MatchService.MatchCriteria(
                ConfirmedSpec.of(request.confirmedTags()),
                request.region(),
                request.avoidStylistIds() == null ? Set.of() : Set.copyOf(request.avoidStylistIds())));

        List<EnrichedMatchResponse.Candidate> candidates = result.candidates().stream()
                .map(c -> new EnrichedMatchResponse.Candidate(
                        c.stylistId(),
                        c.stylistName(),
                        c.region(),
                        c.portfolioId(),
                        c.dataVersion(),
                        c.summary(),
                        c.matchedFields(),
                        c.mismatchedFields()))
                .toList();
        return new EnrichedMatchResponse(analysisId, candidates, result.reason());
    }

    public record EnrichedMatchResponse(
            String analysisId,
            List<Candidate> candidates,
            String reason
    ) {
        public record Candidate(
                String stylistId,
                String stylistName,
                String region,
                String portfolioId,
                String dataVersion,
                String summary,
                List<com.mopick.stylespec.StyleField> matchedFields,
                List<com.mopick.stylespec.StyleField> mismatchedFields
        ) {
        }
    }
}
