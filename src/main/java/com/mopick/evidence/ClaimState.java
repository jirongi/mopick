package com.mopick.evidence;

/** Evidence 비교 결과 상태. UI의 비슷한 점 / 다른 점 / 확인할 점으로 나뉜다. */
public enum ClaimState {
    /** 확정 태그가 같다 → 비슷한 점 */
    MATCH,
    /** 확정 태그가 다르다 → 다른 점 */
    DIFFERENCE,
    /** 한쪽이라도 "모름"이라 비교할 수 없다 → 확인할 점 */
    UNKNOWN,
    /** 사진·태그에 해당 부위가 없다 → 확인할 점 */
    NOT_VISIBLE
}
