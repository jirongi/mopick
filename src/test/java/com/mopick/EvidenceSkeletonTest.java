package com.mopick;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopick.ai.PairwiseAiResponse;
import com.mopick.evidence.ClaimSkeletonBuilder;
import com.mopick.evidence.ClaimSource;
import com.mopick.evidence.ClaimState;
import com.mopick.evidence.ClaimValidator;
import com.mopick.evidence.EvidenceClaim;
import com.mopick.evidence.ForbiddenPhrasePolicy;
import com.mopick.stylespec.ConfirmedSpec;
import com.mopick.stylespec.ConfirmedTag;
import com.mopick.stylespec.StyleField;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** AI 없이 돌아가는 부분만 검증한다. 여기가 fallback의 정확성을 보장한다. */
class EvidenceSkeletonTest {

    private final ClaimSkeletonBuilder builder = new ClaimSkeletonBuilder();
    private final ClaimValidator validator = new ClaimValidator(new ForbiddenPhrasePolicy());

    private ConfirmedSpec spec(String length, String bangs, String texture) {
        return ConfirmedSpec.of(List.of(
                new ConfirmedTag(StyleField.LENGTH, length),
                new ConfirmedTag(StyleField.BANGS, bangs),
                new ConfirmedTag(StyleField.TEXTURE, texture)));
    }

    private EvidenceClaim claimFor(List<EvidenceClaim> claims, StyleField field) {
        return claims.stream().filter(c -> c.field() == field).findFirst().orElseThrow();
    }

    @Test
    void 뼈대는_항상_8개_필드를_채운다() {
        List<EvidenceClaim> claims = builder.build(spec("쇄골", "시스루뱅", null), spec("쇄골", null, "C컬"), "pf_0001");

        assertThat(claims).hasSize(StyleField.values().length);
        assertThat(claims).allSatisfy(c -> assertThat(c.source()).isEqualTo(ClaimSource.CONFIRMED_TAG));
        assertThat(claims).allSatisfy(c -> assertThat(c.explanation()).isNotBlank());
    }

    @Test
    void 확정_태그_비교가_상태를_결정한다() {
        List<EvidenceClaim> claims = builder.build(spec("쇄골", "시스루뱅", null), spec("쇄골", "커튼뱅", "C컬"), "pf_0001");

        assertThat(claimFor(claims, StyleField.LENGTH).state()).isEqualTo(ClaimState.MATCH);
        assertThat(claimFor(claims, StyleField.BANGS).state()).isEqualTo(ClaimState.DIFFERENCE);
        // 사용자가 "모름"으로 둔 필드는 포트폴리오에 값이 있어도 비교하지 않는다.
        assertThat(claimFor(claims, StyleField.TEXTURE).state()).isEqualTo(ClaimState.UNKNOWN);
        // 양쪽 다 값이 없는 필드
        assertThat(claimFor(claims, StyleField.PART).state()).isEqualTo(ClaimState.NOT_VISIBLE);
    }

    @Test
    void 어휘_사전에_없는_확정값은_모름으로_처리한다() {
        ConfirmedSpec goal = ConfirmedSpec.of(List.of(new ConfirmedTag(StyleField.LENGTH, "어깨 아래쯤")));

        assertThat(goal.valueByField().get(StyleField.LENGTH)).isNull();
    }

    @Test
    void AI가_확정_태그와_다른_상태를_주장하면_반려한다() {
        List<EvidenceClaim> skeleton =
                builder.build(spec("쇄골", null, null), spec("턱선", null, null), "pf_0001");
        String tagExplanation = claimFor(skeleton, StyleField.LENGTH).explanation();

        ClaimValidator.MergeResult result = validator.merge(skeleton, Map.of(
                StyleField.LENGTH,
                new PairwiseAiResponse.ClaimDto("LENGTH", "MATCH", "두 사진의 길이가 비슷해 보입니다.")));

        EvidenceClaim length = claimFor(result.claims(), StyleField.LENGTH);
        assertThat(length.state()).isEqualTo(ClaimState.DIFFERENCE);
        assertThat(length.explanation()).isEqualTo(tagExplanation);
        assertThat(result.rejectedCount()).isEqualTo(1);
    }

    @Test
    void 금지_표현이_있으면_확정_태그_설명으로_되돌린다() {
        List<EvidenceClaim> skeleton =
                builder.build(spec("쇄골", null, null), spec("쇄골", null, null), "pf_0001");
        String tagExplanation = claimFor(skeleton, StyleField.LENGTH).explanation();

        ClaimValidator.MergeResult result = validator.merge(skeleton, Map.of(
                StyleField.LENGTH,
                new PairwiseAiResponse.ClaimDto("LENGTH", "MATCH", "이 미용사는 실력이 좋아 똑같이 나옵니다.")));

        assertThat(claimFor(result.claims(), StyleField.LENGTH).explanation()).isEqualTo(tagExplanation);
        assertThat(result.aiAppliedCount()).isZero();
    }

    @Test
    void AI가_상태를_빠뜨리면_검증할_수_없으므로_반려한다() {
        // 실제로 모델이 state 없이 "확인할 수 없습니다"만 보내와 MATCH claim에 그 문구가 붙은 적이 있다.
        List<EvidenceClaim> skeleton =
                builder.build(spec("쇄골", null, null), spec("쇄골", null, null), "pf_0001");
        String tagExplanation = claimFor(skeleton, StyleField.LENGTH).explanation();

        ClaimValidator.MergeResult result = validator.merge(skeleton, Map.of(
                StyleField.LENGTH,
                new PairwiseAiResponse.ClaimDto("LENGTH", null, "제공된 사진에서 모발의 길이를 확인할 수 없습니다.")));

        EvidenceClaim length = claimFor(result.claims(), StyleField.LENGTH);
        assertThat(length.state()).isEqualTo(ClaimState.MATCH);
        assertThat(length.explanation()).isEqualTo(tagExplanation);
        assertThat(result.rejectedCount()).isEqualTo(1);
        assertThat(result.aiAppliedCount()).isZero();
    }

    @Test
    void 상태는_맞췄어도_설명이_그_상태를_부정하면_반려한다() {
        // "비슷한 점" 칸에 "비교가 어렵습니다"가 실리는 모순을 막는다.
        List<EvidenceClaim> skeleton =
                builder.build(spec("쇄골", null, null), spec("쇄골", null, null), "pf_0001");
        String tagExplanation = claimFor(skeleton, StyleField.LENGTH).explanation();

        ClaimValidator.MergeResult result = validator.merge(skeleton, Map.of(
                StyleField.LENGTH,
                new PairwiseAiResponse.ClaimDto("LENGTH", "MATCH", "두 사진 모두 길이를 확인할 수 없어 비교가 어렵습니다.")));

        assertThat(claimFor(result.claims(), StyleField.LENGTH).explanation()).isEqualTo(tagExplanation);
        assertThat(result.rejectedCount()).isEqualTo(1);
    }

    @Test
    void 확인할_점_칸에서는_관찰_불가_표현을_그대로_쓴다() {
        // 확정값이 없는 필드는 "확인할 수 없습니다"가 정확한 설명이므로 막지 않는다.
        List<EvidenceClaim> skeleton =
                builder.build(spec("쇄골", null, null), spec("쇄골", null, null), "pf_0001");

        ClaimValidator.MergeResult result = validator.merge(skeleton, Map.of(
                StyleField.PART,
                new PairwiseAiResponse.ClaimDto("PART", "NOT_VISIBLE", "사진에서 가르마를 확인할 수 없습니다.")));

        EvidenceClaim part = claimFor(result.claims(), StyleField.PART);
        assertThat(part.explanation()).isEqualTo("사진에서 가르마를 확인할 수 없습니다.");
        assertThat(result.aiAppliedCount()).isEqualTo(1);
    }

    @Test
    void 상태가_같으면_AI_설명을_반영하되_출처는_확정_태그로_둔다() {
        List<EvidenceClaim> skeleton =
                builder.build(spec("쇄골", null, null), spec("쇄골", null, null), "pf_0001");

        ClaimValidator.MergeResult result = validator.merge(skeleton, Map.of(
                StyleField.LENGTH,
                new PairwiseAiResponse.ClaimDto("LENGTH", "MATCH", "두 사진 모두 쇄골 근처에서 끝납니다.")));

        EvidenceClaim length = claimFor(result.claims(), StyleField.LENGTH);
        assertThat(length.explanation()).isEqualTo("두 사진 모두 쇄골 근처에서 끝납니다.");
        assertThat(length.source()).isEqualTo(ClaimSource.CONFIRMED_TAG);
        assertThat(result.aiAppliedCount()).isEqualTo(1);
    }

    @Test
    void 확정값이_없는_필드에서만_AI가_상태를_채운다() {
        List<EvidenceClaim> skeleton =
                builder.build(spec("쇄골", null, null), spec("쇄골", null, null), "pf_0001");

        ClaimValidator.MergeResult result = validator.merge(skeleton, Map.of(
                StyleField.PART,
                new PairwiseAiResponse.ClaimDto("PART", "MATCH", "두 사진 모두 가운데 가르마로 보입니다.")));

        EvidenceClaim part = claimFor(result.claims(), StyleField.PART);
        assertThat(part.state()).isEqualTo(ClaimState.MATCH);
        assertThat(part.source()).isEqualTo(ClaimSource.PAIRWISE_AI);
    }
}
