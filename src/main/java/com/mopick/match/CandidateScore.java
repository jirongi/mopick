package com.mopick.match;

import com.mopick.portfolio.PortfolioEntry;
import com.mopick.stylespec.StyleField;
import java.util.List;

/**
 * 후보 1건의 내부 점수. <b>이 값은 사용자에게 노출하지 않는다.</b>
 * 스펙상 내부 유사도 점수는 비공개이며, 사용자에게는 근거(evidence)만 보인다.
 *
 * @param matchedWeight    일치한 필드의 가중치 합
 * @param comparableWeight 양쪽 모두 값이 있어 비교가 성립한 필드의 가중치 합
 * @param matchedFields    일치한 필드 목록. 로그와 디버깅용.
 */
public record CandidateScore(
        PortfolioEntry entry,
        int matchedWeight,
        int comparableWeight,
        List<StyleField> matchedFields
) {
    /** 비교 가능한 것 중 얼마나 맞았는지. 0~1. 비교할 게 없으면 0. */
    public double ratio() {
        return comparableWeight == 0 ? 0.0 : (double) matchedWeight / comparableWeight;
    }

    /** 8개 중 얼마나 비교할 수 있었는지. 근거의 두께. 0~1. */
    public double coverage() {
        return (double) comparableWeight / MatchWeights.total();
    }

    /**
     * 후보로 내보낼 자격이 있는지.
     *
     * <p>비교할 근거가 아예 없거나 하나도 맞지 않은 사례를 "관련된 작업을 해본 근거가 있는 미용사"로
     * 보여줄 수는 없다. 억지 추천을 하지 않는다는 원칙이 여기서 지켜진다.
     */
    public boolean isEligible() {
        return comparableWeight > 0 && matchedWeight > 0;
    }
}
