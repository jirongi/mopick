package com.mopick.ai;

import java.util.List;

/** 사진 1:1 비교 원시 응답. 상태와 설명만 받는다. 후보 순서·포함 여부는 여기서 다루지 않는다. */
public record PairwiseAiResponse(List<ClaimDto> claims) {

    public record ClaimDto(
            String field,
            String state,
            String explanation
    ) {
    }
}
