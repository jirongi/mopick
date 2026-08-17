package com.mopick.stylespec;

/**
 * 한 필드에 대한 관찰 결과.
 *
 * @param value {@link Vocabulary} 사전에 있는 값. OBSERVED가 아니면 null.
 */
public record StyleObservation(
        StyleField field,
        ObservationState state,
        String value,
        Confidence confidence
) {
    public static StyleObservation notVisible(StyleField field) {
        return new StyleObservation(field, ObservationState.NOT_VISIBLE, null, null);
    }

    public static StyleObservation unclear(StyleField field) {
        return new StyleObservation(field, ObservationState.UNCLEAR, null, null);
    }
}
