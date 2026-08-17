package com.mopick.evidence;

import com.mopick.ai.PairwiseAiResponse;
import com.mopick.stylespec.StyleField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AI 비교 결과를 확정 태그 뼈대에 병합한다. 여기가 AI 출력이 사용자에게 닿기 전 마지막 관문이다.
 *
 * <p>규칙은 두 가지다.
 * <ul>
 *   <li>양쪽 태그가 확정된 필드 — 상태는 확정 태그가 정한다. AI는 설명만 바꿀 수 있고,
 *       AI가 다른 상태를 주장하면 그 필드의 AI 출력을 통째로 버린다.</li>
 *   <li>한쪽이라도 태그가 비어 있는 필드 — 충돌할 확정 값이 없으므로 AI 보조 관찰을 허용한다.
 *       이때만 source가 {@code pairwise_ai}가 된다.</li>
 * </ul>
 * 어느 경우든 금지 표현이 섞이면 확정 태그 설명으로 되돌린다.
 */
@Component
public class ClaimValidator {

    private static final Logger log = LoggerFactory.getLogger(ClaimValidator.class);

    /** 한 문장을 넘어서는 길이는 설명이 아니라 서술로 흐른 것으로 보고 버린다. */
    private static final int MAX_EXPLANATION_LENGTH = 200;

    /**
     * "확인할 수 없다"류 표현. 확정 태그가 이미 같다/다르다를 정한 필드에 이런 설명이 붙으면
     * 비슷한 점 칸에 "비교가 어렵습니다"가 실리는 모순이 생긴다. 상태는 맞지만 설명이 태그와 충돌하는 경우다.
     */
    private static final Pattern NOT_OBSERVED = Pattern.compile(
            "확인할 수 없|확인이 어렵|확인되지 않|관찰되지 않|관찰할 수 없|보이지 않|"
                    + "알 수 없|비교가 어렵|비교할 수 없|판단할 수 없|나타나지 않|드러나지 않");

    private final ForbiddenPhrasePolicy forbiddenPhrasePolicy;

    public ClaimValidator(ForbiddenPhrasePolicy forbiddenPhrasePolicy) {
        this.forbiddenPhrasePolicy = forbiddenPhrasePolicy;
    }

    public record MergeResult(List<EvidenceClaim> claims, int aiAppliedCount, int rejectedCount) {
    }

    public MergeResult merge(List<EvidenceClaim> skeleton,
                             Map<StyleField, PairwiseAiResponse.ClaimDto> aiByField) {
        List<EvidenceClaim> merged = new ArrayList<>(skeleton.size());
        int applied = 0;
        int rejected = 0;

        for (EvidenceClaim base : skeleton) {
            PairwiseAiResponse.ClaimDto dto = aiByField.get(base.field());
            if (dto == null) {
                merged.add(base);
                continue;
            }
            String explanation = clean(dto.explanation());
            if (explanation == null || forbiddenPhrasePolicy.violates(explanation)) {
                log.debug("AI 설명 반려: field={}, reason={}", base.field(),
                        explanation == null ? "형식" : "금지 표현");
                merged.add(base);
                rejected++;
                continue;
            }

            boolean tagsDecided = base.state() == ClaimState.MATCH || base.state() == ClaimState.DIFFERENCE;
            if (tagsDecided) {
                ClaimState aiState = parseState(dto.state());
                // 상태가 다르면 충돌이고, 상태가 없으면 일치하는지 확인할 방법이 없다.
                // 둘 다 통과시키지 않는다. 검증하지 못한 설명은 반려하는 쪽이 안전하다.
                // (모델이 state를 빠뜨린 채 "확인할 수 없습니다"를 보내와 MATCH claim에 그 문구가
                //  붙는 일이 실제로 있었다.)
                if (aiState != base.state()) {
                    log.debug("AI 상태를 확정 태그로 검증하지 못함: field={}, tag={}, ai={}",
                            base.field(), base.state(), dto.state());
                    merged.add(base);
                    rejected++;
                    continue;
                }
                if (NOT_OBSERVED.matcher(explanation).find()) {
                    // 상태는 맞췄지만 설명이 그 상태를 부정한다. 확정 태그 설명으로 되돌린다.
                    log.debug("AI 설명이 확정 상태와 모순: field={}, state={}, explanation={}",
                            base.field(), base.state(), explanation);
                    merged.add(base);
                    rejected++;
                    continue;
                }
                merged.add(base.withExplanation(explanation, ClaimSource.CONFIRMED_TAG));
                applied++;
                continue;
            }

            // 확정 값이 없는 필드 — AI 보조 관찰 허용
            ClaimState aiState = parseState(dto.state());
            merged.add(base.withAiObservation(aiState == null ? base.state() : aiState, explanation));
            applied++;
        }
        return new MergeResult(merged, applied, rejected);
    }

    private String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_EXPLANATION_LENGTH) {
            return null;
        }
        return trimmed;
    }

    private ClaimState parseState(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ClaimState.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
