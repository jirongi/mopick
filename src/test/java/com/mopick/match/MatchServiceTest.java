package com.mopick.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopick.portfolio.PortfolioRepository;
import com.mopick.stylespec.ConfirmedSpec;
import com.mopick.stylespec.ConfirmedTag;
import com.mopick.stylespec.StyleField;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * 매칭은 AI를 부르지 않으므로 전부 단위 테스트로 검증할 수 있다.
 * fixture(portfolios.json)를 그대로 쓴다.
 */
class MatchServiceTest {

    private final MatchService service =
            new MatchService(new PortfolioRepository(JsonMapper.builder().build()));

    private ConfirmedSpec goal(Object... fieldValuePairs) {
        List<ConfirmedTag> tags = new java.util.ArrayList<>();
        for (int i = 0; i < fieldValuePairs.length; i += 2) {
            tags.add(new ConfirmedTag((StyleField) fieldValuePairs[i], (String) fieldValuePairs[i + 1]));
        }
        return ConfirmedSpec.of(tags);
    }

    /** pf_0001과 완전히 같은 희망 태그. */
    private ConfirmedSpec exactlyPf0001() {
        return goal(
                StyleField.STYLE_FAMILY, "레이어드",
                StyleField.LENGTH, "쇄골",
                StyleField.BANGS, "시스루뱅",
                StyleField.SIDE_SILHOUETTE, "자연스러움",
                StyleField.BACK_SILHOUETTE, "A라인",
                StyleField.TOP_VOLUME, "보통",
                StyleField.PART, "중앙",
                StyleField.TEXTURE, "C컬");
    }

    private MatchService.MatchResult match(ConfirmedSpec goal) {
        return service.match(new MatchService.MatchCriteria(goal, null, Set.of()));
    }

    private List<String> portfolioIds(MatchService.MatchResult r) {
        return r.candidates().stream().map(MatchService.MatchResult.Candidate::portfolioId).toList();
    }

    @Test
    void 관련도_순으로_후보를_돌려준다() {
        MatchService.MatchResult result = match(exactlyPf0001());

        // pf_0001은 8필드 전부 일치, pf_0004는 일부만 일치 → 순서가 정해진다.
        assertThat(portfolioIds(result)).containsExactly("pf_0001", "pf_0004");
        assertThat(result.reason()).isNull();
    }

    @Test
    void 한_미용사는_가장_근거가_좋은_사례_한_건만_대표로_나온다() {
        // pf_0001과 pf_0002는 둘 다 이서연(st_001). 사례가 많다고 후보 자리를 독점하면 안 된다.
        MatchService.MatchResult result = match(exactlyPf0001());

        assertThat(portfolioIds(result)).contains("pf_0001").doesNotContain("pf_0002");
        assertThat(result.candidates()).extracting(MatchService.MatchResult.Candidate::stylistId)
                .doesNotHaveDuplicates();
    }

    @Test
    void 권리나_게시_검수를_통과하지_못한_사례는_후보가_되지_않는다() {
        // pf_0005는 pf_0001과 태그가 같지만 rightsCleared=false다.
        MatchService.MatchResult result = match(exactlyPf0001());

        assertThat(portfolioIds(result)).doesNotContain("pf_0005");
    }

    @Test
    void 후보가_2명_미만이면_억지로_추천하지_않는다() {
        // 보브+턱선은 pf_0003 하나만 관련이 있다.
        MatchService.MatchResult result = match(goal(
                StyleField.STYLE_FAMILY, "보브",
                StyleField.LENGTH, "턱선"));

        assertThat(result.candidates()).isEmpty();
        assertThat(result.reason()).isNotBlank();
    }

    @Test
    void 하나도_일치하지_않는_사례는_후보가_되지_않는다() {
        // "관련된 작업을 해본 근거가 있는 미용사"여야 하므로 일치 0건은 자격이 없다.
        MatchService.MatchResult result = match(goal(
                StyleField.STYLE_FAMILY, "히메",
                StyleField.LENGTH, "가슴아래"));

        assertThat(result.candidates()).isEmpty();
    }

    @Test
    void 지역_조건을_적용한다() {
        MatchService.MatchResult seongdong = service.match(
                new MatchService.MatchCriteria(exactlyPf0001(), "성동", Set.of()));
        MatchService.MatchResult mapo = service.match(
                new MatchService.MatchCriteria(exactlyPf0001(), "마포", Set.of()));

        // 성동에는 관련 사례가 pf_0003뿐이고 그마저 일치가 없어 후보가 안 된다.
        assertThat(seongdong.candidates()).isEmpty();
        assertThat(portfolioIds(mapo)).containsExactly("pf_0001", "pf_0004");
    }

    @Test
    void 회피_미용사는_제외한다() {
        MatchService.MatchResult result = service.match(
                new MatchService.MatchCriteria(exactlyPf0001(), null, Set.of("st_001")));

        // 이서연이 빠지면 남는 후보가 최지우 한 명뿐 → 억지 추천하지 않는다.
        assertThat(result.candidates()).isEmpty();
        assertThat(result.reason()).isNotBlank();
    }

    @Test
    void 중요_필드는_두_배로_계산한다() {
        var pf0001 = service.score(exactlyPf0001(),
                new PortfolioRepository(JsonMapper.builder().build()).findById("pf_0001").orElseThrow());

        // 중요 3개(2점)+일반 5개(1점)=11점 만점, 전부 일치.
        assertThat(pf0001.comparableWeight()).isEqualTo(MatchWeights.total()).isEqualTo(11);
        assertThat(pf0001.matchedWeight()).isEqualTo(11);
        assertThat(pf0001.ratio()).isEqualTo(1.0);
    }

    @Test
    void 기록되지_않은_필드는_점수에서_제외한다() {
        // pf_0004는 TEXTURE가 null이다. 0점으로 깎으면 꼼꼼히 기록한 미용사가 손해를 본다.
        var pf0004 = service.score(exactlyPf0001(),
                new PortfolioRepository(JsonMapper.builder().build()).findById("pf_0004").orElseThrow());

        assertThat(pf0004.comparableWeight()).isEqualTo(MatchWeights.total() - 1);
        assertThat(pf0004.matchedFields()).doesNotContain(StyleField.TEXTURE);
    }

    @Test
    void 사용자가_모름으로_둔_필드는_점수에_넣지_않는다() {
        MatchService.MatchResult result = match(goal(
                StyleField.STYLE_FAMILY, "레이어드",
                StyleField.BANGS, "시스루뱅"));

        // 두 필드만 비교했는데 둘 다 맞는 pf_0001과 pf_0004가 동률로 올라온다.
        assertThat(portfolioIds(result)).containsExactly("pf_0001", "pf_0004");
    }

    @Test
    void 같은_입력에는_항상_같은_후보가_같은_순서로_나온다() {
        List<String> first = portfolioIds(match(exactlyPf0001()));
        List<String> second = portfolioIds(match(exactlyPf0001()));
        List<String> third = portfolioIds(match(exactlyPf0001()));

        assertThat(first).isEqualTo(second).isEqualTo(third);
    }

    @Test
    void 후보는_최대_3명이다() {
        MatchService.MatchResult result = match(goal(StyleField.STYLE_FAMILY, "레이어드"));

        assertThat(result.candidates()).hasSizeLessThanOrEqualTo(3);
    }
}
