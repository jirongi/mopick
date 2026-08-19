package com.mopick.match;

import com.mopick.common.Josa;
import com.mopick.portfolio.PortfolioEntry;
import com.mopick.stylespec.ConfirmedSpec;
import com.mopick.stylespec.StyleField;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** PDF p.24~25 스타일의 매칭 한 줄 요약 생성. */
@Component
public class MatchSummaryGenerator {

    public String summarize(CandidateScore score, ConfirmedSpec goal) {
        if (score.comparableWeight() == 0) {
            return "비교할 수 있는 기록이 아직 충분하지 않아요";
        }
        double ratio = score.ratio();
        List<StyleField> mismatched = findMismatched(goal, score);

        if (ratio >= 0.95 && score.coverage() >= 0.35) {
            return "전체적인 스타일이 매우 유사해요";
        }
        if (mismatched.size() == 1) {
            return Josa.eulReul(mismatched.get(0).label()) + " 제외하면 대부분 일치해요";
        }
        if (mismatched.size() == 2) {
            return mismatched.get(0).label() + "·" + mismatched.get(1).label() + "을(를) 제외하면 대부분 일치해요";
        }
        if (ratio >= 0.75) {
            return "여러 부분에서 유사해요";
        }
        if (ratio >= 0.5) {
            return "일부 스타일 요소가 맞아요";
        }
        return "상담에서 자세히 확인해 보세요";
    }

    private List<StyleField> findMismatched(ConfirmedSpec goal, CandidateScore score) {
        var goalValues = goal.valueByField();
        var portfolioValues = ConfirmedSpec.of(score.entry().confirmedTags()).valueByField();
        Set<StyleField> matched = Set.copyOf(score.matchedFields());
        List<StyleField> mismatched = new ArrayList<>();
        for (StyleField field : StyleField.values()) {
            String g = goalValues.get(field);
            String p = portfolioValues.get(field);
            if (g != null && p != null && !matched.contains(field)) {
                mismatched.add(field);
            }
        }
        return mismatched;
    }
}
