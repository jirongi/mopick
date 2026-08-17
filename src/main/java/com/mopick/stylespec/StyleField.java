package com.mopick.stylespec;

/**
 * 희망 사진과 작업 사진을 동일한 기준으로 구조화하는 8개 관찰 필드.
 *
 * <p>이 enum은 AI 분석, 결정론적 매칭, Evidence 비교가 공유하는 단일 계약이다.
 * 필드를 추가·삭제하면 세 곳이 모두 영향을 받으므로 함부로 바꾸지 않는다.
 */
public enum StyleField {
    STYLE_FAMILY("스타일 계열"),
    LENGTH("길이"),
    BANGS("앞머리"),
    SIDE_SILHOUETTE("옆 실루엣"),
    BACK_SILHOUETTE("뒤 실루엣"),
    TOP_VOLUME("상단 볼륨"),
    PART("가르마"),
    TEXTURE("질감");

    private final String label;

    StyleField(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
