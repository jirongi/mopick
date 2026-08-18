package com.mopick.geometry;

import static com.mopick.geometry.FaceParsingMask.CLOTH;
import static com.mopick.geometry.FaceParsingMask.EAR_LEFT;
import static com.mopick.geometry.FaceParsingMask.EAR_RIGHT;
import static com.mopick.geometry.FaceParsingMask.HAIR;
import static com.mopick.geometry.FaceParsingMask.SKIN;
import static org.assertj.core.api.Assertions.assertThat;

import com.mopick.stylespec.StyleField;
import org.junit.jupiter.api.Test;

/**
 * 마스크에서 좌표를 뽑는 부분. 모델 없이 합성 마스크로 검증한다.
 *
 * <p>마스크는 [행][열]이고 값은 부위 번호다. 아래 헬퍼로 "몇 행부터 몇 행까지 이 부위"를
 * 칠해 실제 사진의 배치를 흉내 낸다.
 */
class FaceParsingMaskTest {

    private final FaceParsingMask parser = new FaceParsingMask();
    private final GeometricFieldDeriver deriver = new GeometricFieldDeriver();

    private int[][] blank(int h, int w) {
        return new int[h][w];
    }

    /**
     * [y0,y1] × [x0,x1] 구간을 label로 칠한다.
     * 실제 마스크는 픽셀마다 부위가 하나뿐이므로, 머리카락을 먼저 깔고 얼굴·귀를 그 위에 올린다.
     */
    private void paint(int[][] m, int y0, int y1, int x0, int x1, int label) {
        for (int y = Math.max(0, y0); y <= Math.min(m.length - 1, y1); y++) {
            for (int x = Math.max(0, x0); x <= Math.min(m[y].length - 1, x1); x++) {
                m[y][x] = label;
            }
        }
    }

    @Test
    void 부위별_위아래_끝에서_좌표를_뽑는다() {
        int[][] m = blank(200, 100);
        paint(m, 10, 150, 25, 75, HAIR);   // 머리카락이 바깥 윤곽을 이룬다
        paint(m, 20, 120, 35, 65, SKIN);   // 얼굴이 그 안에
        paint(m, 60, 85, 30, 38, EAR_LEFT);

        HeadMeasurement mm = parser.extract(m);

        assertThat(mm.headTopY()).isEqualTo(20.0);   // 얼굴 최상단
        assertThat(mm.chinY()).isEqualTo(120.0);     // 얼굴 최하단
        assertThat(mm.hairTopY()).isEqualTo(10.0);
        assertThat(mm.hairBottomY()).isEqualTo(150.0);
        assertThat(mm.earTopY()).isEqualTo(60.0);
        assertThat(mm.earBottomY()).isEqualTo(85.0);
    }

    @Test
    void 얼굴이_없으면_뒷모습으로_보고_턱을_만들지_않는다() {
        // 뒷모습에서 턱 위치를 추정해 넣으면 그 아래 판정이 전부 조용히 틀어진다.
        int[][] m = blank(200, 100);
        paint(m, 10, 120, 25, 75, HAIR);

        HeadMeasurement mm = parser.extract(m);

        assertThat(mm.viewAngle()).isEqualTo(HeadMeasurement.ViewAngle.BACK);
        assertThat(mm.chinY()).isNull();
        assertThat(deriver.derive(mm)).doesNotContainKey(StyleField.LENGTH);
    }

    @Test
    void 귀가_둘이면_정면_하나면_옆모습() {
        int[][] front = blank(200, 100);
        paint(front, 20, 120, 35, 65, SKIN);
        paint(front, 60, 85, 30, 34, EAR_LEFT);
        paint(front, 60, 85, 66, 70, EAR_RIGHT);

        int[][] side = blank(200, 100);
        paint(side, 20, 120, 35, 65, SKIN);
        paint(side, 60, 85, 30, 34, EAR_LEFT);

        assertThat(parser.extract(front).viewAngle()).isEqualTo(HeadMeasurement.ViewAngle.FRONT);
        assertThat(parser.extract(side).viewAngle()).isEqualTo(HeadMeasurement.ViewAngle.SIDE);
    }

    @Test
    void 옷이_보이면_어깨선으로_삼는다() {
        int[][] m = blank(300, 100);
        paint(m, 20, 120, 35, 65, SKIN);
        paint(m, 60, 85, 30, 38, EAR_LEFT);
        paint(m, 200, 299, 0, 99, CLOTH);

        assertThat(parser.extract(m).shoulderY()).isEqualTo(200.0);
    }

    @Test
    void 옆_폭은_귀_높이에서_잰다() {
        int[][] m = blank(200, 100);
        paint(m, 60, 85, 30, 70, HAIR);       // 귀 높이에서 머리카락이 넓게
        paint(m, 20, 120, 40, 60, SKIN);      // 두상은 좁게
        paint(m, 60, 85, 38, 40, EAR_LEFT);

        HeadMeasurement mm = parser.extract(m);

        // 폭 자체는 여전히 재둔다(진단용). 다만 판정에는 쓰지 않는다 —
        // 옆·뒷모습에서 보이는 얼굴 폭이 작아져 비율이 부풀려지기 때문이다.
        assertThat(mm.headWidthPx()).isNotNull();
        assertThat(mm.hairWidthPx()).isGreaterThan(mm.headWidthPx());
    }

    @Test
    void 빈_마스크는_아무_좌표도_만들지_않는다() {
        HeadMeasurement mm = parser.extract(blank(50, 50));

        assertThat(mm.headTopY()).isNull();
        assertThat(mm.chinY()).isNull();
        assertThat(mm.hairBottomY()).isNull();
        assertThat(deriver.derive(mm)).isEmpty();
    }

    @Test
    void null_마스크에도_터지지_않는다() {
        assertThat(parser.extract(null).headTopY()).isNull();
        assertThat(parser.extract(new int[0][0]).headTopY()).isNull();
    }

    @Test
    void 마스크에서_뽑은_좌표로_길이_판정까지_이어진다() {
        // 귀 아래로 머리가 길게 내려오는 경우 → 목덜미
        // 귀 상단 60, 턱 120 → 단위 60. 귀 하단 85.
        int[][] m = blank(300, 100);
        paint(m, 10, 135, 25, 75, HAIR);      // 머리 끝 135 = 턱(120) 아래 목덜미 구간
        paint(m, 20, 120, 35, 65, SKIN);
        paint(m, 60, 85, 30, 38, EAR_LEFT);

        HeadMeasurement mm = parser.extract(m);

        assertThat(deriver.derive(mm).get(StyleField.LENGTH).value()).isEqualTo("목덜미");
    }
}
