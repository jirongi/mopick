package com.mopick.stylespec;

/**
 * 사람이 확정한 태그. AI 관찰과 달리 이것만이 매칭·비교의 근거가 된다.
 *
 * @param value 확정 값. 사용자가 "모름"을 선택했거나 미용사가 비워두면 null.
 */
public record ConfirmedTag(StyleField field, String value) {

    public boolean isUnknown() {
        return value == null;
    }
}
