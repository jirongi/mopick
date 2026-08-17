package com.mopick.evidence;

import com.mopick.common.Josa;
import com.mopick.stylespec.ConfirmedSpec;
import com.mopick.stylespec.StyleField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 확정 태그만으로 8개 claim 뼈대를 만든다. AI를 전혀 쓰지 않는다.
 *
 * <p>이 결과가 곧 {@code confirmed_evidence} fallback이다. 사진이 없거나 API key가 없거나
 * AI 호출이 실패해도 사용자는 항상 이 뼈대를 받는다. AI는 이 위에 설명만 얹는다.
 */
@Component
public class ClaimSkeletonBuilder {

    public List<EvidenceClaim> build(ConfirmedSpec goal, ConfirmedSpec portfolio, String portfolioId) {
        Map<StyleField, String> goalValues = goal.valueByField();
        Map<StyleField, String> portfolioValues = portfolio.valueByField();

        List<EvidenceClaim> claims = new ArrayList<>();
        for (StyleField field : StyleField.values()) {
            String g = goalValues.get(field);
            String p = portfolioValues.get(field);
            ClaimState state = resolveState(g, p);
            claims.add(new EvidenceClaim(field, state, g, p, portfolioId,
                    ClaimSource.CONFIRMED_TAG, explain(field, state, g, p)));
        }
        return claims;
    }

    private ClaimState resolveState(String goalValue, String portfolioValue) {
        if (goalValue == null && portfolioValue == null) {
            return ClaimState.NOT_VISIBLE;
        }
        if (goalValue == null || portfolioValue == null) {
            return ClaimState.UNKNOWN;
        }
        return goalValue.equals(portfolioValue) ? ClaimState.MATCH : ClaimState.DIFFERENCE;
    }

    /**
     * 확정 태그만 참조하는 설명 문구. AI 설명이 검증에 걸리면 이 문구로 되돌린다.
     * 실력·보장·재현 같은 표현은 여기에 절대 넣지 않는다.
     */
    String explain(StyleField field, ClaimState state, String goalValue, String portfolioValue) {
        String label = field.label();
        return switch (state) {
            case MATCH -> Josa.iga(label) + " 확정 태그 기준으로 같습니다. (" + goalValue + ")";
            case DIFFERENCE -> Josa.iga(label) + " 다릅니다. 희망 " + goalValue + " / 작업 사례 " + portfolioValue + ".";
            case UNKNOWN -> goalValue == null
                    ? Josa.eulReul(label) + " 아직 정하지 않아 비교하지 않았습니다. 상담에서 확인해 보세요."
                    : Josa.iga(label) + " 작업 사례에 기록되어 있지 않아 비교하지 않았습니다. 상담에서 확인해 보세요.";
            case NOT_VISIBLE -> Josa.eunNeun(label) + " 양쪽 모두 기록이 없어 확인할 수 없습니다.";
        };
    }
}
