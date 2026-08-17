package com.mopick.ai;

import java.util.List;

/**
 * 모델이 돌려주는 원시 응답. enum이 아니라 String으로 받는다.
 * 모델이 허용 목록 밖의 값을 뱉어도 역직렬화가 깨지지 않고 Java 검증 단계에서 걸러지게 하기 위함이다.
 */
public record StyleSpecAiResponse(
        List<ObservationDto> observations,
        Boolean retake,
        String retakeReason
) {
    public record ObservationDto(
            String field,
            String state,
            String value,
            String confidence
    ) {
    }
}
