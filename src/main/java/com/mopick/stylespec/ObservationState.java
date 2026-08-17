package com.mopick.stylespec;

/** 한 필드를 사진에서 어떻게 관찰했는지. */
public enum ObservationState {
    /** 사진에서 명확히 보였고 값을 특정했다. */
    OBSERVED,
    /** 보이긴 하나 값을 특정할 수 없다. */
    UNCLEAR,
    /** 사진 각도·가림으로 해당 부위가 보이지 않는다. */
    NOT_VISIBLE
}
