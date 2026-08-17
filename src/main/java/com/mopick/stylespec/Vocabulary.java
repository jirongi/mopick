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
                "귀를 기준점으로 삼아 아래 순서대로 판정한다. "
                        + "① 옆머리가 귀를 전혀 덮지 않아 귀 전체가 드러난다 → 두피가 실제로 비쳐 보이면 매우짧음, "
                        + "아니면 귀위. 이때 뒷머리가 목덜미 근처까지 내려와 있어도 귀위를 넘지 않는다. "
                        + "② 옆머리가 귀의 일부 이상을 덮지만 목덜미 위에서 끝난다 → 귀덮음. "
                        + "③ 옆머리가 귀를 덮고 뒷머리가 목덜미 선에 닿거나 목을 덮는다 → 목덜미. "
                        + "④ 그보다 길면 턱선·어깨·쇄골·가슴·가슴아래 중 끝이 닿는 곳으로 정한다. "
                        + "짧게 깎였더라도 두피가 비치지 않으면 매우짧음이 아니다."));

        SPECS.put(StyleField.BANGS, new FieldSpec(
                List.of("없음", "시스루뱅", "일자뱅", "사이드뱅", "커튼뱅", "짧은뱅", "넘김"),
                "이마 쪽 머리 상태다. 이마가 화면에 보이는 사진에서만 기록하고, "
                        + "뒤통수만 보여 이마를 확인할 수 없으면 값을 고르지 말고 NOT_VISIBLE로 둔다. "
                        + "없음=앞머리를 따로 두지 않아 이마가 드러남. 넘김=앞머리를 뒤나 옆으로 넘겨 이마가 드러남. "
                        + "커튼뱅=가운데가 갈라져 양옆으로 나뉨. 시스루뱅=이마가 비쳐 보일 만큼 숱이 적음. "
                        + "일자뱅=끝이 가로로 고름."));

        SPECS.put(StyleField.SIDE_SILHOUETTE, new FieldSpec(
                List.of("짧게깎임", "붙는형", "자연스러움", "부푼형", "귀뒤넘김"),
                "귀 주변 옆선의 부피다. 짧게깎임=옆이 매우 짧아 두상 선이 그대로 드러남. "
                        + "붙는형=길이는 있으나 두상에 밀착해 옆 폭이 두상 폭과 거의 같음. "
                        + "자연스러움=약간의 두께가 있으나 옆으로 부풀지 않음. "
                        + "부푼형=옆 폭이 두상 폭보다 확연히 넓음. 귀뒤넘김=옆머리를 귀 뒤로 넘김."));

        SPECS.put(StyleField.BACK_SILHOUETTE, new FieldSpec(
                List.of("짧게깎임", "그라데이션", "일자", "A라인", "U라인", "V라인", "둥근형"),
                "뒷머리 아랫부분의 형태다. 짧게깎임=뒷목이 드러나게 짧음. "
                        + "그라데이션=아래로 갈수록 짧아지는 단차가 보임. 일자/A라인/U라인/V라인/둥근형="
                        + "머리 끝이 한 선을 이룰 때 그 선의 모양. "
                        + "뒷머리가 화면에 보이면 짧은 머리라도 짧게깎임이나 그라데이션으로 기록한다."));

        SPECS.put(StyleField.TOP_VOLUME, new FieldSpec(
                List.of("낮음", "보통", "높음"),
                "정수리 머리카락이 두피 곡선에서 얼마나 떠 있는지다. 보통은 기본값이 아니다. "
                        + "낮음=두피 곡선에 붙어 납작함. 보통=붙지도 솟지도 않은 자연스러운 두께. "
                        + "높음=두피 곡선보다 확연히 떠 있거나 위로 솟음. 웨이브나 컬로 위쪽이 부풀어 있으면 높음이다. "
                        + "세 값 중 어느 쪽인지 정말 판단이 서지 않으면 보통을 고르지 말고 UNCLEAR로 둔다."));

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
