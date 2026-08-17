package com.mopick.api.dto;

import com.mopick.stylespec.ConfirmedTag;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @param confirmedTags    사용자가 확정한 8개 태그. 빠진 필드는 "모름"으로 취급한다.
 * @param goalImageBase64  희망 사진(base64). 원본을 저장하지 않으므로 비교가 필요할 때마다 다시 받는다.
 *                         없으면 확정 태그 근거만으로 응답한다.
 * @param portfolioIds     결정론적 매칭이 고른 후보 id. 최대 3개까지만 쓴다.
 */
public record EvidenceMatchRequest(
        List<ConfirmedTag> confirmedTags,
        String goalImageBase64,
        @NotEmpty(message = "portfolioIds는 비어 있을 수 없습니다.")
        List<String> portfolioIds
) {
}
