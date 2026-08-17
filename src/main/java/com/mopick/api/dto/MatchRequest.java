package com.mopick.api.dto;

import com.mopick.stylespec.ConfirmedTag;
import java.util.List;

/**
 * @param confirmedTags   사용자가 확정한 8개 태그. 빠진 필드는 "모름"으로 취급하며 점수에 넣지 않는다.
 * @param region          희망 지역. 비우면 지역을 따지지 않는다.
 * @param avoidStylistIds 제외할 미용사.
 */
public record MatchRequest(
        List<ConfirmedTag> confirmedTags,
        String region,
        List<String> avoidStylistIds
) {
}
