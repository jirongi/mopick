package com.mopick.geometry;

import com.mopick.stylespec.ObservationState;
import com.mopick.stylespec.StyleField;
import com.mopick.stylespec.StyleObservation;
import com.mopick.stylespec.StyleSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 기하 측정 결과를 LLM 관찰 위에 얹는다.
 *
 * <p>규칙은 하나다. <b>기하로 잴 수 있었던 필드는 기하가 이긴다.</b>
 * 측정은 같은 사진에 항상 같은 값을 주지만 LLM은 경계에서 흔들리기 때문이다.
 * 재지 못한 필드는 LLM 관찰을 그대로 쓴다.
 *
 * <p>이 구조는 evidence 쪽과 같다 — 결정론적 근거를 먼저 세우고 그 위에 LLM을 얹는다.
 */
@Component
public class GeometryMerger {

    private static final Logger log = LoggerFactory.getLogger(GeometryMerger.class);

    public record MergeResult(StyleSpec spec, List<StyleField> measuredFields) {
    }

    public MergeResult merge(StyleSpec llmSpec, Map<StyleField, StyleObservation> measured) {
        if (measured == null || measured.isEmpty()) {
            return new MergeResult(llmSpec, List.of());
        }
        Map<StyleField, StyleObservation> byField = llmSpec.byField();

        List<StyleObservation> merged = new ArrayList<>();
        List<StyleField> replaced = new ArrayList<>();
        for (StyleField field : StyleField.values()) {
            StyleObservation geo = measured.get(field);
            if (geo != null && geo.state() == ObservationState.OBSERVED) {
                merged.add(geo);
                replaced.add(field);
            } else {
                merged.add(byField.get(field));
            }
        }
        if (!replaced.isEmpty()) {
            log.debug("기하 측정으로 대체한 필드: {}", replaced);
        }
        return new MergeResult(
                new StyleSpec(merged, llmSpec.retake(), llmSpec.retakeReason(), llmSpec.status()),
                List.copyOf(replaced));
    }
}
