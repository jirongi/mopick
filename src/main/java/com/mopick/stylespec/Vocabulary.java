package com.mopick.stylespec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 필드별 허용 값 사전과 판정 기준.
 *
 * <p>값을 자유 텍스트로 두면 "어깨 아래"와 "쇄골선"이 번갈아 나와 태그 비교가 무너진다.
 * AI 응답은 반드시 이 사전으로 검증하고, 사전에 없는 값은 값으로 인정하지 않는다.
 *
 * <p>어휘는 성별 중립이어야 한다. AI가 성별을 추론하지 않기로 했으므로 "남성용 값"과
 * "여성용 값"으로 나눌 수 없다. 대신 짧은 쪽과 긴 쪽 모두에 충분한 해상도를 둔다.
 * 짧은 쪽 칸이 부족하면 짧은 머리가 전부 한 칸에 뭉쳐 변별력이 사라진다.
 *
 * <p>{@code criteria}는 프롬프트에 함께 실린다. 값 목록만 주면 인접한 값의 경계에서
 * 판정이 흔들리므로, 어디서 갈리는지를 문장으로 명시한다.
 */
public final class Vocabulary {

    /**
     * @param values   허용 값. 순서는 프롬프트 노출 순서이자 사람이 읽는 순서다.
     * @param criteria 값 사이의 경계를 정하는 기준. 프롬프트에만 쓰이고 응답 검증에는 쓰지 않는다.
     */
    public record FieldSpec(List<String> values, String criteria) {
    }

    private static final Map<StyleField, FieldSpec> SPECS = new LinkedHashMap<>();

    static {
        SPECS.put(StyleField.STYLE_FAMILY, new FieldSpec(
                List.of("원랭스", "레이어드", "그라데이션", "샤기", "보브", "투블럭", "짧은단정형", "픽시", "히메"),
                "전체 형태로 정한다. 투블럭=옆·뒤가 확연히 짧고 윗머리가 그 위를 덮어 단차가 눈에 띔. "
                        + "짧은단정형=전체가 짧고 층 차이가 거의 없어 한 덩어리로 보임. "
                        + "레이어드=길이가 다른 층이 뚜렷하게 보임. 샤기=끝이 가볍게 흩어져 결이 드러남. "
                        + "원랭스=끝이 한 선으로 떨어짐. 그라데이션=아래로 갈수록 짧아지는 단차. "
                        + "머리 끝에 층이나 결이 눈에 띄면 짧은단정형이 아니라 레이어드나 샤기다. "
                        + "옆·뒤가 확연히 짧지 않다면 투블럭이 아니다."));

        SPECS.put(StyleField.LENGTH, new FieldSpec(
                List.of("매우짧음", "귀위", "귀덮음", "목덜미", "턱선", "어깨", "쇄골", "가슴", "가슴아래"),
                "기준선은 '귀가 드러나는가'가 아니라 '머리 끝이 귀 아래 라인을 넘는가'다. "
                        + "귀가 보이더라도 뒷머리나 구레나룻이 귀 아래로 길게 내려오면 짧은 머리가 아니다. "
                        + "가장 긴 지점(대개 뒷머리 끝이나 구레나룻)을 기준으로 긴 쪽부터 판정한다. "
                        + "① 머리 끝이 목덜미 선에 닿거나 목을 덮는다 → 목덜미. 더 길면 턱선·어깨·쇄골·가슴·가슴아래. "
                        + "② 목덜미까지는 아니지만 귀 아래로 내려와 귀를 덮거나 지난다 → 귀덮음. "
                        + "③ 머리 끝이 귀 아래로 내려오지 않고 귀가 드러난다 → 귀위. 두피가 실제로 비치면 매우짧음. "
                        + "옆에서 본 사진이면 뒷머리 끝이 어디서 끝나는지를 반드시 확인한다."));

        SPECS.put(StyleField.BANGS, new FieldSpec(
                List.of("없음", "시스루뱅", "일자뱅", "사이드뱅", "커튼뱅", "짧은뱅", "넘김"),
                "이마 쪽 머리 상태다. 앞머리는 이마를 봐야만 판단할 수 있다. "
                        + "이마가 화면에 보이지 않으면 어떤 값도 고르지 않고 반드시 NOT_VISIBLE로 둔다. "
                        + "뒤통수만 보이는 사진에서 '없음'이나 '넘김'을 고르는 것도 금지다. "
                        + "뒤에서 머리가 뒤로 넘어가 보인다고 해서 앞머리를 넘긴 것인지는 알 수 없다. "
                        + "없음=앞머리를 따로 두지 않아 이마가 드러남. 넘김=앞머리를 뒤나 옆으로 넘겨 이마가 드러남. "
                        + "커튼뱅=가운데가 갈라져 양옆으로 나뉨. 시스루뱅=이마가 비쳐 보일 만큼 숱이 적음. "
                        + "일자뱅=끝이 가로로 고름."));

        SPECS.put(StyleField.SIDE_SILHOUETTE, new FieldSpec(
                List.of("짧게깎임", "붙는형", "자연스러움", "부푼형", "귀뒤넘김"),
                "귀 주변 옆선의 부피다. 짧게깎임=옆이 매우 짧아 두상 선이 그대로 드러남. "
                        + "붙는형=옆머리가 두상에 밀착해, 귀 위쪽에서 머리 바깥선과 두상선이 거의 붙어 보임. "
                        + "자연스러움=머리 바깥선이 두상선에서 눈에 띄게 떨어져 있으나 부풀지는 않음. "
                        + "부푼형=옆 폭이 두상 폭보다 확연히 넓음. 귀뒤넘김=옆머리를 귀 뒤로 넘김."));

        SPECS.put(StyleField.BACK_SILHOUETTE, new FieldSpec(
                List.of("짧게깎임", "그라데이션", "일자", "A라인", "U라인", "V라인", "둥근형"),
                "뒷머리 아랫부분의 형태다. 짧게깎임=뒷목이 드러나게 짧음. "
                        + "그라데이션=아래로 갈수록 짧아지는 단차가 보임. 일자/A라인/U라인/V라인/둥근형="
                        + "머리 끝이 한 선을 이룰 때 그 선의 모양. "
                        + "뒷머리가 화면에 보이면 짧은 머리라도 짧게깎임이나 그라데이션으로 기록한다."));

        SPECS.put(StyleField.TOP_VOLUME, new FieldSpec(
                List.of("낮음", "보통", "높음"),
                "정수리 머리가 서 있는 정도다. 보통은 기본값이 아니며, 셋 중 가장 좁은 구간이다. "
                        + "정수리 머리 두께를 옆머리 두께와 견주어 본다. "
                        + "낮음=두상 윤곽을 따라 누워 있어 머리 선과 두상 선이 거의 겹침. "
                        + "높음=머리가 서서 두상 윤곽선 위로 뚜렷한 두께가 얹혀 보이거나, 앞머리 뿌리가 세워져 위로 솟음. "
                        + "펌이나 웨이브로 위쪽이 부풀어 있으면 높음이다. "
                        + "보통=눕지도 서지도 않은 경우에만. 애매하면 보통을 고르지 말고 UNCLEAR로 둔다."));

        SPECS.put(StyleField.PART, new FieldSpec(
                List.of("없음", "중앙", "사이드", "지그재그", "올백"),
                "가르마 선이다. 없음=가르마 선 없이 앞으로 내림. 중앙=가운데. "
                        + "사이드=한쪽으로 치우침. 올백=전체를 뒤로 넘겨 이마가 드러남."));

        SPECS.put(StyleField.TEXTURE, new FieldSpec(
                List.of("직모", "C컬", "S컬", "강한웨이브", "볼륨감"),
                "모발이 휘는 정도다. 직모=곡선이 거의 없음. C컬=끝이 한 방향으로 완만히 말림. "
                        + "S컬=중간부터 굴곡이 반복됨. 강한웨이브=굴곡이 뚜렷하고 촘촘함. "
                        + "볼륨감=곡선보다 부피가 두드러짐."));
    }

    private Vocabulary() {
    }

    public static List<String> valuesOf(StyleField field) {
        return SPECS.get(field).values();
    }

    public static boolean isAllowed(StyleField field, String value) {
        return value != null && SPECS.get(field).values().contains(value);
    }

    /** 앞뒤 공백·인용부호만 제거한 뒤 사전과 대조한다. 사전에 없으면 null. */
    public static String normalize(StyleField field, String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().replaceAll("^[\"'\\s]+|[\"'\\s]+$", "");
        return isAllowed(field, trimmed) ? trimmed : null;
    }

    /** 프롬프트에 그대로 삽입할 필드별 허용값과 판정 기준. */
    public static String promptBlock() {
        StringBuilder sb = new StringBuilder();
        for (StyleField field : StyleField.values()) {
            FieldSpec spec = SPECS.get(field);
            sb.append("- ").append(field.name())
                    .append(" (").append(field.label()).append("): ")
                    .append(String.join(" | ", spec.values())).append('\n')
                    .append("    기준: ").append(spec.criteria()).append('\n');
        }
        return sb.toString();
    }
}
