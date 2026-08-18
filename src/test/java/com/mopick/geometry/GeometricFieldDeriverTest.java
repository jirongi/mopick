package com.mopick.geometry;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopick.stylespec.Confidence;
import com.mopick.stylespec.ObservationState;
import com.mopick.stylespec.StyleField;
import org.junit.jupiter.api.Test;

/**
 * 좌표 → 길이 판정. 여기가 LLM이 세 라운드 동안 못 해낸 일을 대신한다.
 *
 * <p>좌표는 실제 사진 측정값을 본떴다. 귀 상단~턱을 단위로 쓰므로 그 둘을 항상 준다.
 */
class GeometricFieldDeriverTest {

    private final GeometricFieldDeriver deriver = new GeometricFieldDeriver();

    private String length(HeadMeasurement m) {
        var o = deriver.derive(m).get(StyleField.LENGTH);
        return o == null ? null : o.value();
    }

    /** 귀상단 218, 턱 396 → 단위 178. 실제 1.jpeg 측정값이다. */
    private HeadMeasurement.Builder realistic() {
        return new HeadMeasurement.Builder().headTop(218).chin(396).ear(218, 301);
    }

    @Test
    void 실제_1번_사진_측정값은_귀위로_판정된다() {
        // 귀아래길이 (332-301)/178 = 0.174 → 임계값 0.25 미만
        assertThat(length(realistic().hairBottom(332).build())).isEqualTo("귀위");
    }

    @Test
    void 실제_3번_사진_측정값은_귀덮음으로_판정된다() {
        // 귀상단 237, 턱 407 → 단위 170. 귀아래길이 (380-324)/170 = 0.329
        var m = new HeadMeasurement.Builder().headTop(237).chin(407).ear(237, 324)
                .hairBottom(380).build();

        assertThat(length(m)).isEqualTo("귀덮음");
    }

    @Test
    void 세_라운드_동안_못_갈랐던_두_사진이_좌표로는_갈린다() {
        String photo1 = length(realistic().hairBottom(332).build());
        String photo3 = length(new HeadMeasurement.Builder().headTop(237).chin(407).ear(237, 324)
                .hairBottom(380).build());

        assertThat(photo1).isEqualTo("귀위");
        assertThat(photo3).isEqualTo("귀덮음");
        assertThat(photo1).isNotEqualTo(photo3);
    }

    @Test
    void 투블럭처럼_귀_위에서_끝나면_귀위() {
        // 실제 2.jpeg: 귀아래길이 -0.283
        var m = new HeadMeasurement.Builder().headTop(236).chin(462).ear(236, 383)
                .hairBottom(319).build();

        assertThat(length(m)).isEqualTo("귀위");
    }

    @Test
    void 턱_아래_목덜미까지_오면_목덜미() {
        assertThat(length(realistic().hairBottom(430).build())).isEqualTo("목덜미");
    }

    @Test
    void 같은_입력에는_항상_같은_값이_나온다() {
        var m = realistic().hairBottom(380).build();

        for (int i = 0; i < 20; i++) {
            assertThat(length(m)).isEqualTo("귀덮음");
        }
    }

    @Test
    void 어깨가_안_보이면_턱선_이하는_판정하지_않는다() {
        var o = deriver.derive(realistic().hairBottom(600).build()).get(StyleField.LENGTH);

        assertThat(o.state()).isEqualTo(ObservationState.UNCLEAR);
    }

    @Test
    void 어깨가_보이면_어깨_길이를_판정한다() {
        var m = realistic().hairBottom(600).shoulder(600).build();

        assertThat(length(m)).isEqualTo("어깨");
    }

    @Test
    void 어깨보다_한참_아래면_쇄골() {
        var m = realistic().hairBottom(700).shoulder(600).build();

        assertThat(length(m)).isEqualTo("쇄골");
    }

    @Test
    void 귀를_못_찾으면_판정하지_않는다() {
        var m = new HeadMeasurement.Builder().headTop(218).chin(396).hairBottom(332).build();

        assertThat(deriver.derive(m)).doesNotContainKey(StyleField.LENGTH);
    }

    @Test
    void 턱을_못_찾으면_판정하지_않는다() {
        // 뒷모습이면 얼굴이 없어 턱이 안 잡힌다. 추정해 넣으면 조용히 틀어진다.
        var m = new HeadMeasurement.Builder().ear(218, 301).hairBottom(332).build();

        assertThat(deriver.derive(m)).doesNotContainKey(StyleField.LENGTH);
    }

    @Test
    void 경계에_가까우면_신뢰도를_낮춘다() {
        // 귀아래길이가 0.25 바로 위
        var m = realistic().hairBottom(348).build();

        assertThat(deriver.derive(m).get(StyleField.LENGTH).confidence())
                .isEqualTo(Confidence.MEDIUM);
    }

    @Test
    void 기하가_다루지_않는_필드는_건드리지_않는다() {
        var derived = deriver.derive(realistic().hairBottom(332).hairTop(21).width(237, 245).build());

        assertThat(derived).containsOnlyKeys(StyleField.LENGTH);
    }

    @Test
    void 측정값이_없으면_아무것도_판정하지_않는다() {
        assertThat(deriver.derive(new HeadMeasurement.Builder().build())).isEmpty();
        assertThat(deriver.derive(null)).isEmpty();
    }
}
