package com.mopick.geometry;

import java.util.OptionalDouble;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 얼굴 파싱 마스크에서 {@link HeadMeasurement}를 뽑아낸다.
 *
 * <p>마스크는 픽셀마다 부위 번호가 들어 있는 2차원 배열이다(CelebAMask-HQ 19분류 기준).
 * 여기서 하는 일은 순수한 배열 계산이라 모델 없이도 검증할 수 있다.
 *
 * <p>없는 부위는 좌표를 만들어내지 않는다. 뒷모습이면 얼굴 픽셀이 아예 없고, 그때
 * 턱 위치를 추정해 넣으면 그 아래 모든 판정이 조용히 틀어진다.
 */
@Component
public class FaceParsingMask {

    // CelebAMask-HQ 분류 번호
    public static final int BACKGROUND = 0;
    public static final int SKIN = 1;
    public static final int EYEGLASSES = 6;
    public static final int EAR_LEFT = 7;
    public static final int EAR_RIGHT = 8;
    public static final int NOSE = 10;
    public static final int MOUTH = 11;
    public static final int LIP_UPPER = 12;
    public static final int LIP_LOWER = 13;
    public static final int NECK = 14;
    public static final int CLOTH = 16;
    public static final int HAIR = 17;

    private static final Set<Integer> EARS = Set.of(EAR_LEFT, EAR_RIGHT);
    private static final Set<Integer> FACE = Set.of(SKIN, NOSE, MOUTH, LIP_UPPER, LIP_LOWER);
    /** 두상 윤곽 = 얼굴 + 귀. 머리카락은 뺀다(머리카락 두께를 재야 하므로). */
    private static final Set<Integer> HEAD = Set.of(SKIN, NOSE, MOUTH, LIP_UPPER, LIP_LOWER,
            EAR_LEFT, EAR_RIGHT, EYEGLASSES);

    /**
     * @param mask [height][width] 부위 번호
     */
    public HeadMeasurement extract(int[][] mask) {
        if (mask == null || mask.length == 0 || mask[0].length == 0) {
            return new HeadMeasurement.Builder().build();
        }
        HeadMeasurement.Builder b = new HeadMeasurement.Builder();

        Extent head = extentOf(mask, HEAD);
        Extent face = extentOf(mask, FACE);
        Extent hair = extentOf(mask, Set.of(HAIR));
        Extent ears = extentOf(mask, EARS);
        Extent cloth = extentOf(mask, Set.of(CLOTH));

        // 두상 최상단은 얼굴·귀 영역의 위쪽 끝. 머리카락은 그 위에 얹힌다.
        if (head.found()) {
            b.headTop(head.minY());
        }
        // 턱 끝은 얼굴 픽셀의 아래쪽 끝. 얼굴이 없으면(뒷모습) 만들지 않는다.
        if (face.found()) {
            b.chin(face.maxY());
        }
        if (hair.found()) {
            b.hairTop(hair.minY());
            b.hairBottom(hair.maxY());
        }
        if (ears.found()) {
            b.ear(ears.minY(), ears.maxY());
        }
        if (cloth.found()) {
            b.shoulder(cloth.minY());
        }

        // 옆 폭은 귀 높이에서 잰다. 그 높이가 옆선 부피를 가장 잘 드러낸다.
        if (ears.found()) {
            int row = (int) Math.round((ears.minY() + ears.maxY()) / 2.0);
            OptionalDouble headWidth = rowWidth(mask, row, HEAD);
            OptionalDouble hairWidth = rowWidth(mask, row, Set.of(HAIR, SKIN, NOSE, MOUTH,
                    LIP_UPPER, LIP_LOWER, EAR_LEFT, EAR_RIGHT, EYEGLASSES));
            if (headWidth.isPresent() && hairWidth.isPresent()) {
                b.width(headWidth.getAsDouble(), hairWidth.getAsDouble());
            }
        }
        b.angle(viewAngle(mask));
        return b.build();
    }

    /**
     * 촬영 각도. 얼굴이 안 보이면 뒷모습, 귀가 둘 다 보이면 정면, 하나면 옆모습으로 본다.
     * 이 판정으로 뒷모습에서 앞머리를 단정하는 문제를 코드가 막을 수 있다.
     */
    HeadMeasurement.ViewAngle viewAngle(int[][] mask) {
        boolean hasFace = extentOf(mask, FACE).found();
        if (!hasFace) {
            return HeadMeasurement.ViewAngle.BACK;
        }
        boolean left = extentOf(mask, Set.of(EAR_LEFT)).found();
        boolean right = extentOf(mask, Set.of(EAR_RIGHT)).found();
        if (left && right) {
            return HeadMeasurement.ViewAngle.FRONT;
        }
        if (left || right) {
            return HeadMeasurement.ViewAngle.SIDE;
        }
        return HeadMeasurement.ViewAngle.UNKNOWN;
    }

    /** 한 행에서 해당 부위들이 차지하는 가로 폭. */
    private OptionalDouble rowWidth(int[][] mask, int row, Set<Integer> labels) {
        if (row < 0 || row >= mask.length) {
            return OptionalDouble.empty();
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = 0; x < mask[row].length; x++) {
            if (labels.contains(mask[row][x])) {
                min = Math.min(min, x);
                max = Math.max(max, x);
            }
        }
        return min > max ? OptionalDouble.empty() : OptionalDouble.of(max - min + 1);
    }

    private record Extent(int minY, int maxY, boolean found) {
    }

    private Extent extentOf(int[][] mask, Set<Integer> labels) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[y].length; x++) {
                if (labels.contains(mask[y][x])) {
                    min = Math.min(min, y);
                    max = Math.max(max, y);
                }
            }
        }
        return min > max ? new Extent(0, 0, false) : new Extent(min, max, true);
    }
}
