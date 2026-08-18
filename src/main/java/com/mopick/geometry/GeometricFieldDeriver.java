package com.mopick.geometry;

import com.mopick.stylespec.Confidence;
import com.mopick.stylespec.ObservationState;
import com.mopick.stylespec.StyleField;
import com.mopick.stylespec.StyleObservation;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;
import org.springframework.stereotype.Component;

/**
 * 측정값을 8필드 중 <b>기하로 결정할 수 있는 필드</b>의 값으로 옮긴다.
 *
 * <p>LLM에게 눈대중을 시키면 인접한 값의 경계에서 판정이 흔들린다. 실제로 LENGTH는
 * 세 라운드에 걸쳐 기준 문장을 고쳤는데도 두 사진을 끝내 구분하지 못했다. 픽셀로 재면
 * 같은 사진에는 항상 같은 값이 나오고, 두 사진의 차이도 숫자로 드러난다.
 *
 * <p><b>기하로 다루는 필드는 LENGTH 하나다.</b> 나머지는 실측 결과 근거가 부족해 뺐다.
 * <ul>
 *   <li>TOP_VOLUME — 두상 최상단이 언제나 머리카락에 덮여 있어 "두상 위 머리 두께"를 잴 수 없다.
 *       귀 상단 기준 대체 지표를 실측했더니 정답과 순서가 반대로 나왔다.</li>
 *   <li>SIDE_SILHOUETTE — 옆·뒷모습에서는 보이는 얼굴 폭이 작아져 비율이 부풀려진다.
 *       가장 붙은 머리(투블럭 뒷모습)가 가장 큰 값으로 나왔다.</li>
 *   <li>STYLE_FAMILY·BANGS·TEXTURE — 의미·질감 판단이라 애초에 기하로 환원되지 않는다.</li>
 * </ul>
 * 이 필드들은 계속 LLM이 맡는다.
 *
 * <p>임계값은 귀 상단~턱을 1로 한 비율이다. 사진 속 인물 크기와 무관해진다.
 */
@Component
public class GeometricFieldDeriver {

    /**
     * 머리끝이 귀 하단보다 이만큼 아래로 내려와야 "귀를 덮는다"고 본다.
     *
     * <p>실측으로 정했다. 짧은 커트도 뒷목 잔머리가 늘 귀 아래로 조금은 내려오기 때문에
     * 0으로 두면 전부 귀덮음이 된다. 사진 4장 측정값:
     * 투블럭 -0.28 / 짧은펌 -0.03 / 귀 드러난 커트 0.17 / 목덜미까지 오는 커트 0.33.
     */
    private static final double BELOW_EAR_THRESHOLD = 0.25;

    /** 턱 아래 이 정도까지가 목덜미 구간. */
    private static final double NAPE_BELOW_CHIN = 0.30;

    /** 어깨 판정 허용 오차. */
    private static final double SHOULDER_MARGIN = 0.10;

    /**
     * 기하로 판정한 필드만 담아 돌려준다. 판정 못 한 필드는 아예 넣지 않는다.
     *
     * <p>현재는 LENGTH 하나다. 나머지 두 후보는 실측에서 근거가 부족해 뺐다.
     */
    public Map<StyleField, StyleObservation> derive(HeadMeasurement m) {
        Map<StyleField, StyleObservation> result = new EnumMap<>(StyleField.class);
        if (m == null) {
            return result;
        }
        StyleObservation length = length(m);
        if (length != null) {
            result.put(StyleField.LENGTH, length);
        }
        return result;
    }

    /**
     * 머리카락 최하단이 귀 하단보다 얼마나 아래로 내려오는지로 정한다.
     *
     * <p>"끝이 어디까지 오는지"를 사람 말로 설명하면 짧은 커트가 전부 한 칸에 뭉쳤는데,
     * 귀 하단 기준으로 재면 그 차이가 그대로 드러난다. 실제로 세 라운드 동안 LLM이
     * 구분하지 못한 두 사진이 0.17과 0.33으로 갈렸다.
     */
    StyleObservation length(HeadMeasurement m) {
        if (m.hairBottomY() == null || m.earBottomY() == null || m.chinY() == null) {
            return null;
        }
        OptionalDouble unitOpt = m.scaleUnit();
        if (unitOpt.isEmpty()) {
            return null;
        }
        double unit = unitOpt.getAsDouble();
        double belowEar = (m.hairBottomY() - m.earBottomY()) / unit;

        if (belowEar < BELOW_EAR_THRESHOLD) {
            return observed("귀위", Confidence.HIGH);
        }
        if (m.hairBottomY() < m.chinY()) {
            return observed("귀덮음", nearBoundary(belowEar, BELOW_EAR_THRESHOLD));
        }
        if (m.hairBottomY() < m.chinY() + NAPE_BELOW_CHIN * unit) {
            return observed("목덜미", Confidence.HIGH);
        }
        // 턱선 아래로는 어깨 위치를 알아야 구간을 나눌 수 있다. 없으면 추측하지 않는다.
        if (m.shoulderY() == null) {
            return new StyleObservation(StyleField.LENGTH, ObservationState.UNCLEAR, null, null);
        }
        double shoulderGap = (m.hairBottomY() - m.shoulderY()) / unit;
        if (shoulderGap < -SHOULDER_MARGIN) {
            return observed("턱선", Confidence.MEDIUM);
        }
        if (shoulderGap <= SHOULDER_MARGIN) {
            return observed("어깨", Confidence.HIGH);
        }
        return observed("쇄골", Confidence.MEDIUM);
    }

    /** 경계에 가까우면 신뢰도를 낮춘다. 사용자가 확인할 때 참고가 된다. */
    private Confidence nearBoundary(double value, double boundary) {
        return Math.abs(value - boundary) < 0.05 ? Confidence.MEDIUM : Confidence.HIGH;
    }

    private StyleObservation observed(String value, Confidence confidence) {
        return new StyleObservation(StyleField.LENGTH, ObservationState.OBSERVED, value, confidence);
    }
}
