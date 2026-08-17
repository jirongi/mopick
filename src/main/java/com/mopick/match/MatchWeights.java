package com.mopick.match;

import com.mopick.stylespec.StyleField;
import java.util.EnumMap;
import java.util.Map;

/**
 * 필드별 가중치. 스펙의 "중요 필드 2배"를 구현한다.
 *
 * <p>어떤 필드가 중요한지는 제품 판단이다. 여기서는 <b>스타일의 정체성을 결정하는 세 가지</b>를
 * 중요 필드로 본다. 계열·길이·앞머리가 다르면 나머지가 아무리 같아도 다른 머리로 보인다.
 * 반대로 볼륨이나 가르마는 같은 커트 안에서 스타일링으로 달라질 수 있다.
 *
 * <p>이 선택은 사용자 피드백을 보고 조정해야 한다. 바꿀 때는 여기 한 곳만 고치면 된다.
 */
public final class MatchWeights {

    private static final int IMPORTANT = 2;
    private static final int NORMAL = 1;

    private static final Map<StyleField, Integer> WEIGHTS = new EnumMap<>(StyleField.class);

    static {
        WEIGHTS.put(StyleField.STYLE_FAMILY, IMPORTANT);
        WEIGHTS.put(StyleField.LENGTH, IMPORTANT);
        WEIGHTS.put(StyleField.BANGS, IMPORTANT);
        WEIGHTS.put(StyleField.SIDE_SILHOUETTE, NORMAL);
        WEIGHTS.put(StyleField.BACK_SILHOUETTE, NORMAL);
        WEIGHTS.put(StyleField.TOP_VOLUME, NORMAL);
        WEIGHTS.put(StyleField.PART, NORMAL);
        WEIGHTS.put(StyleField.TEXTURE, NORMAL);
    }

    private MatchWeights() {
    }

    public static int of(StyleField field) {
        return WEIGHTS.getOrDefault(field, NORMAL);
    }

    /** 8개 필드 가중치 총합. 커버리지 분모로 쓴다. */
    public static int total() {
        return WEIGHTS.values().stream().mapToInt(Integer::intValue).sum();
    }
}
