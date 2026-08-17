package com.mopick.stylespec;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 사진 1장에 대한 8개 필드 관찰 묶음.
 *
 * @param retake       흐림·노출·가림·다중 인물 등으로 재촬영이 필요한 사진
 * @param retakeReason retake일 때의 사용자 안내 문구, 아니면 null
 * @param status       분석이 실제로 수행되었는지. 전부 NOT_VISIBLE인 응답의 원인을 구분해준다.
 */
public record StyleSpec(
        List<StyleObservation> observations,
        boolean retake,
        String retakeReason,
        AnalysisStatus status
) {
    private static List<StyleObservation> allNotVisible() {
        return Arrays.stream(StyleField.values()).map(StyleObservation::notVisible).toList();
    }

    /**
     * 관찰이 하나도 없는 스펙. 분석을 못 한 이유를 함께 담는다.
     * 사용자는 이 상태에서도 태그를 직접 고를 수 있어야 한다.
     */
    public static StyleSpec unavailable(AnalysisStatus status) {
        return new StyleSpec(allNotVisible(), false, null, status);
    }

    public static StyleSpec retake(String reason) {
        return new StyleSpec(allNotVisible(), true, reason, AnalysisStatus.OK);
    }

    public Map<StyleField, StyleObservation> byField() {
        Map<StyleField, StyleObservation> map = new EnumMap<>(StyleField.class);
        for (StyleObservation o : observations) {
            map.put(o.field(), o);
        }
        for (StyleField f : StyleField.values()) {
            map.putIfAbsent(f, StyleObservation.notVisible(f));
        }
        return map;
    }
}
