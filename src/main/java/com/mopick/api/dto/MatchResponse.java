package com.mopick.api.dto;

import com.mopick.match.MatchService;
import java.util.List;

/**
 * 후보 목록. 관련도 순이며 <b>내부 유사도 점수는 담지 않는다.</b>
 *
 * <p>사용자가 보는 근거는 점수가 아니라 {@code /api/evidence-match}가 만드는 claim이다.
 * 여기서 받은 portfolioId를 그대로 evidence-match에 넘기면 된다.
 *
 * @param reason 후보가 비어 있을 때의 안내 문구. 후보가 있으면 null.
 */
public record MatchResponse(
        String analysisId,
        List<MatchService.MatchResult.Candidate> candidates,
        String reason
) {
}
