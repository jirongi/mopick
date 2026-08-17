package com.mopick.common;

/**
 * 받침 유무에 따라 조사를 고른다.
 *
 * <p>설명 문구는 그대로 사용자에게 노출되므로 "길이이(가)" 같은 표기가 나오면 안 된다.
 */
public final class Josa {

    private static final char HANGUL_START = 0xAC00;
    private static final char HANGUL_END = 0xD7A3;
    private static final int JONGSEONG_COUNT = 28;

    private Josa() {
    }

    /** 이/가 */
    public static String iga(String word) {
        return word + (hasFinalConsonant(word) ? "이" : "가");
    }

    /** 을/를 */
    public static String eulReul(String word) {
        return word + (hasFinalConsonant(word) ? "을" : "를");
    }

    /** 은/는 */
    public static String eunNeun(String word) {
        return word + (hasFinalConsonant(word) ? "은" : "는");
    }

    /**
     * 마지막 글자에 받침이 있는지. 한글이 아니면 받침 없음으로 본다.
     * 필드 라벨은 모두 한글이므로 이 정도로 충분하다.
     */
    static boolean hasFinalConsonant(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        char last = word.charAt(word.length() - 1);
        if (last < HANGUL_START || last > HANGUL_END) {
            return false;
        }
        return (last - HANGUL_START) % JONGSEONG_COUNT != 0;
    }
}
