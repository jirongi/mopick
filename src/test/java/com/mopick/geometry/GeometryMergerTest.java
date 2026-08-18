package com.mopick.geometry;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopick.stylespec.AnalysisStatus;
import com.mopick.stylespec.Confidence;
import com.mopick.stylespec.ObservationState;
import com.mopick.stylespec.StyleField;
import com.mopick.stylespec.StyleObservation;
import com.mopick.stylespec.StyleSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 잰 필드는 측정이 이기고, 못 잰 필드는 LLM 관찰이 남는다. */
class GeometryMergerTest {

    private final GeometryMerger merger = new GeometryMerger();

    private StyleSpec llmSpec() {
        List<StyleObservation> obs = new ArrayList<>();
        for (StyleField f : StyleField.values()) {
            obs.add(new StyleObservation(f, ObservationState.OBSERVED, "LLM값", Confidence.LOW));
        }
        return new StyleSpec(obs, false, null, AnalysisStatus.OK);
    }

    private StyleObservation measured(StyleField f, String v) {
        return new StyleObservation(f, ObservationState.OBSERVED, v, Confidence.HIGH);
    }

    @Test
    void 측정한_필드는_측정값이_이긴다() {
        var result = merger.merge(llmSpec(), Map.of(StyleField.LENGTH, measured(StyleField.LENGTH, "목덜미")));

        assertThat(result.spec().byField().get(StyleField.LENGTH).value()).isEqualTo("목덜미");
        assertThat(result.measuredFields()).containsExactly(StyleField.LENGTH);
    }

    @Test
    void 측정하지_못한_필드는_LLM_관찰이_남는다() {
        var result = merger.merge(llmSpec(), Map.of(StyleField.LENGTH, measured(StyleField.LENGTH, "목덜미")));

        assertThat(result.spec().byField().get(StyleField.TEXTURE).value()).isEqualTo("LLM값");
        assertThat(result.spec().byField().get(StyleField.STYLE_FAMILY).value()).isEqualTo("LLM값");
    }

    @Test
    void 측정_결과가_없으면_LLM_결과를_그대로_쓴다() {
        var result = merger.merge(llmSpec(), Map.of());

        assertThat(result.spec().observations()).hasSize(StyleField.values().length);
        assertThat(result.measuredFields()).isEmpty();
        assertThat(result.spec().byField().get(StyleField.LENGTH).value()).isEqualTo("LLM값");
    }

    @Test
    void 측정이_확정하지_못한_필드는_대체하지_않는다() {
        var unclear = new StyleObservation(StyleField.LENGTH, ObservationState.UNCLEAR, null, null);

        var result = merger.merge(llmSpec(), Map.of(StyleField.LENGTH, unclear));

        assertThat(result.spec().byField().get(StyleField.LENGTH).value()).isEqualTo("LLM값");
        assertThat(result.measuredFields()).isEmpty();
    }

    @Test
    void 재촬영_여부와_분석_상태는_그대로_보존한다() {
        StyleSpec retake = StyleSpec.retake("인물이 둘 이상입니다.");

        var result = merger.merge(retake, Map.of(StyleField.LENGTH, measured(StyleField.LENGTH, "귀위")));

        assertThat(result.spec().retake()).isTrue();
        assertThat(result.spec().retakeReason()).isEqualTo("인물이 둘 이상입니다.");
        assertThat(result.spec().status()).isEqualTo(AnalysisStatus.OK);
    }

    @Test
    void 항상_8개_필드를_유지한다() {
        var result = merger.merge(llmSpec(), Map.of(
                StyleField.LENGTH, measured(StyleField.LENGTH, "귀위"),
                StyleField.TOP_VOLUME, measured(StyleField.TOP_VOLUME, "높음")));

        assertThat(result.spec().observations()).hasSize(StyleField.values().length);
    }
}
