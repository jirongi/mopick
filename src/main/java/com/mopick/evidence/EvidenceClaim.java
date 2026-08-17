package com.mopick.evidence;

import com.mopick.stylespec.StyleField;

/**
 * 한 필드에 대한 근거 1건.
 *
 * <p>claim은 항상 확정 태그로부터 먼저 만들어진다. AI는 claim을 생성하지 않고
 * explanation을 채우거나, 태그가 없는 필드에 한해 보조 관찰만 덧붙인다.
 */
public record EvidenceClaim(
        StyleField field,
        ClaimState state,
        String goalValue,
        String portfolioValue,
        String portfolioId,
        ClaimSource source,
        String explanation
) {
    public EvidenceClaim withExplanation(String newExplanation, ClaimSource newSource) {
        return new EvidenceClaim(field, state, goalValue, portfolioValue, portfolioId,
                newSource, newExplanation);
    }

    public EvidenceClaim withAiObservation(ClaimState newState, String newExplanation) {
        return new EvidenceClaim(field, newState, goalValue, portfolioValue, portfolioId,
                ClaimSource.PAIRWISE_AI, newExplanation);
    }
}
