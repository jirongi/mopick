package com.mopick.portfolio;

import com.mopick.stylespec.ConfirmedTag;
import java.util.List;

/**
 * 검수된 작업 사례 1건.
 *
 * @param confirmedTags    미용사가 확인·수정해 확정한 8개 태그. 매칭·비교의 유일한 근거.
 * @param stylistMetadata  실제 시술과 스타일링 조건. AI가 추론하지 않고 미용사가 직접 입력한다.
 * @param rightsCleared    권리·동의 확보 여부
 * @param published        게시 검수 통과 여부
 * @param imagePath        classpath 기준 작업 사진 경로. 없으면 1:1 비교 없이 태그 근거만 쓴다.
 */
public record PortfolioEntry(
        String portfolioId,
        String stylistId,
        String stylistName,
        String region,
        List<ConfirmedTag> confirmedTags,
        StylistMetadata stylistMetadata,
        boolean rightsCleared,
        boolean published,
        String imagePath,
        String dataVersion
) {
    /** 미용사가 직접 입력하는 값. AI 추론 대상이 아니다. */
    public record StylistMetadata(
            String serviceSummary,
            String stylingCondition,
            String note
    ) {
    }

    /** 매칭 후보가 될 수 있는 상태인지. 검수를 통과하지 않은 사례는 절대 노출하지 않는다. */
    public boolean isMatchable() {
        return rightsCleared && published;
    }
}
