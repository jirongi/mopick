package com.mopick.api.dto;

import com.mopick.stylespec.AnalysisStatus;
import com.mopick.stylespec.StyleField;
import com.mopick.stylespec.StyleObservation;
import com.mopick.stylespec.StyleSpec;
import com.mopick.stylespec.Vocabulary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 사진 분석 응답.
 *
 * <p>{@code vocabulary}를 함께 내려준다. 사용자·미용사가 값을 수정하는 화면이
 * 서버와 같은 허용 목록을 써야 확정 태그가 어긋나지 않기 때문이다.
 *
 * <p>{@code analysisStatus}는 8필드가 전부 NOT_VISIBLE로 왔을 때 그 이유를 알려준다.
 * AI_FAILED면 잠시 후 다시 시도해볼 수 있고, AI_DISABLED면 재시도해도 소용없다.
 */
public record StyleSpecResponse(
        List<StyleObservation> observations,
        boolean retake,
        String retakeReason,
        boolean aiEnabled,
        AnalysisStatus analysisStatus,
        Map<String, List<String>> vocabulary
) {
    public static StyleSpecResponse from(StyleSpec spec, boolean aiEnabled) {
        Map<String, List<String>> vocab = new LinkedHashMap<>();
        for (StyleField field : StyleField.values()) {
            vocab.put(field.name(), Vocabulary.valuesOf(field));
        }
        return new StyleSpecResponse(spec.observations(), spec.retake(), spec.retakeReason(),
                aiEnabled, spec.status(), vocab);
    }
}
