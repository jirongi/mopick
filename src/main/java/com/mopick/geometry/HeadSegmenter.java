package com.mopick.geometry;

import java.util.Optional;

/**
 * 사진에서 머리 관련 좌표를 재는 쪽. 구현은 갈아끼울 수 있다.
 *
 * <p>측정 실패는 예외가 아니라 빈 값이다. 각도·조명·가림 때문에 못 재는 사진이 늘 있고,
 * 그때는 조용히 LLM 관찰로 돌아가면 된다.
 */
public interface HeadSegmenter {

    /** 잴 수 없으면 빈 값. 호출자는 LLM 관찰만으로 진행한다. */
    Optional<HeadMeasurement> measure(byte[] jpeg);

    /** 이 구현이 실제로 동작 가능한 상태인지(모델 파일 적재 여부 등). */
    boolean isReady();
}
