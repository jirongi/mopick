package com.mopick.api.dto;

import com.mopick.evidence.ClaimState;
import com.mopick.evidence.EvidenceClaim;
import com.mopick.evidence.EvidenceMatchService;
import java.util.List;

/**
 * Evidence 응답. UI의 비슷한 점 / 다른 점 / 확인할 점 세 묶음으로 나눠 내려준다.
 * 내부 유사도 점수는 어떤 필드로도 노출하지 않는다.
 */
public record EvidenceMatchResponse(
        String requestId,
        List<Candidate> candidates
) {
    public record Candidate(
            String portfolioId,
            String stylistName,
            String region,
            String dataVersion,
            EvidenceMatchService.EvidenceMode mode,
            List<EvidenceClaim> similar,
            List<EvidenceClaim> different,
            List<EvidenceClaim> toCheck
    ) {
        public static Candidate from(EvidenceMatchService.CandidateEvidence e) {
            return new Candidate(
                    e.portfolioId(), e.stylistName(), e.region(), e.dataVersion(), e.mode(),
                    filter(e.claims(), ClaimState.MATCH),
                    filter(e.claims(), ClaimState.DIFFERENCE),
                    e.claims().stream()
                            .filter(c -> c.state() == ClaimState.UNKNOWN || c.state() == ClaimState.NOT_VISIBLE)
                            .toList());
        }

        private static List<EvidenceClaim> filter(List<EvidenceClaim> claims, ClaimState state) {
            return claims.stream().filter(c -> c.state() == state).toList();
        }
    }

    public static EvidenceMatchResponse from(String requestId, List<EvidenceMatchService.CandidateEvidence> list) {
        return new EvidenceMatchResponse(requestId, list.stream().map(Candidate::from).toList());
    }
}
