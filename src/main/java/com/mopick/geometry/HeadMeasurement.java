package com.mopick.geometry;

import java.util.OptionalDouble;

/**
 * 사진 한 장에서 잰 머리 관련 좌표. 모든 y는 화면 위에서부터의 픽셀이다(아래로 갈수록 큼).
 *
 * <p>값이 없을 수 있다. 각도에 따라 귀가 안 보이거나 턱이 잘리기 때문이다.
 * 없는 값은 {@code null}로 두고, 그 값에 의존하는 필드는 판정하지 않는다.
 * 추측해서 채우는 것보다 모른다고 하는 편이 낫다는 원칙은 여기서도 같다.
 *
 * <p>임계값은 모두 <b>두상 높이 대비 비율</b>로 계산한다. 사진마다 인물 크기가 다르므로
 * 픽셀 절대값으로 기준을 잡으면 같은 머리도 사진에 따라 다르게 판정된다.
 *
 * @param headTopY      두상 최상단(머리카락 제외한 두개골 윤곽)
 * @param chinY         턱 끝
 * @param earTopY       귀 상단
 * @param earBottomY    귀 하단 — LENGTH 판정의 기준점
 * @param hairTopY      머리카락 최상단 — TOP_VOLUME용
 * @param hairBottomY   머리카락 최하단 — LENGTH 판정의 대상
 * @param shoulderY     어깨선. 화면에 없으면 null
 * @param headWidthPx   귀 높이에서의 두상 폭
 * @param hairWidthPx   귀 높이에서의 머리카락 포함 폭 — SIDE_SILHOUETTE용
 * @param viewAngle     촬영 각도
 */
public record HeadMeasurement(
        Double headTopY,
        Double chinY,
        Double earTopY,
        Double earBottomY,
        Double hairTopY,
        Double hairBottomY,
        Double shoulderY,
        Double headWidthPx,
        Double hairWidthPx,
        ViewAngle viewAngle
) {
    public enum ViewAngle {
        FRONT, SIDE, BACK, UNKNOWN
    }

    /**
     * 길이 단위. <b>귀 상단~턱</b>을 1로 삼는다.
     *
     * <p>정수리~턱을 쓰고 싶지만 두상 최상단은 언제나 머리카락에 덮여 있어 마스크로 볼 수 없다.
     * {@code headTopY}는 "머리카락에 안 가린 피부의 위쪽 끝"일 뿐이라 단위로 쓰면 사진마다 흔들린다.
     * 귀 상단과 턱은 옆모습·정면 모두에서 안정적으로 잡힌다.
     */
    public OptionalDouble scaleUnit() {
        if (earTopY == null || chinY == null || chinY <= earTopY) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(chinY - earTopY);
    }

    public boolean hasEar() {
        return earTopY != null && earBottomY != null;
    }

    /** 측정기가 잰 것만 채우고 나머지는 비워 두기 좋게. 테스트에서도 같은 방식으로 쓴다. */
    public static final class Builder {
        private Double headTopY;
        private Double chinY;
        private Double earTopY;
        private Double earBottomY;
        private Double hairTopY;
        private Double hairBottomY;
        private Double shoulderY;
        private Double headWidthPx;
        private Double hairWidthPx;
        private ViewAngle viewAngle = ViewAngle.UNKNOWN;

        public Builder headTop(double y) { this.headTopY = y; return this; }
        public Builder chin(double y) { this.chinY = y; return this; }
        public Builder ear(double topY, double bottomY) { this.earTopY = topY; this.earBottomY = bottomY; return this; }
        public Builder hairTop(double y) { this.hairTopY = y; return this; }
        public Builder hairBottom(double y) { this.hairBottomY = y; return this; }
        public Builder shoulder(double y) { this.shoulderY = y; return this; }
        public Builder width(double head, double hair) { this.headWidthPx = head; this.hairWidthPx = hair; return this; }
        public Builder angle(ViewAngle a) { this.viewAngle = a; return this; }

        public HeadMeasurement build() {
            return new HeadMeasurement(headTopY, chinY, earTopY, earBottomY, hairTopY,
                    hairBottomY, shoulderY, headWidthPx, hairWidthPx, viewAngle);
        }
    }
}
