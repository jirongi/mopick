package com.mopick.evidence;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * AI 설명에 들어가면 안 되는 표현을 잡아내는 결정론적 게이트.
 *
 * <p>프롬프트로도 금지하지만 최종 판정은 여기서 한다. 모델이 지시를 어겨도
 * 사용자에게는 절대 노출되지 않아야 하므로, 걸리면 확정 태그 설명으로 대체한다.
 */
@Component
public class ForbiddenPhrasePolicy {

    /** 미용사 평가 / 결과 보장 / 시술 추론 / 인적 속성 추론. */
    private static final List<Pattern> PATTERNS = List.of(
            // 실력·등급·순위 평가
            Pattern.compile("실력|숙련|잘하|전문가\\s*수준|베테랑|등급|랭킹|순위|1위|top\\s*\\d", Pattern.CASE_INSENSITIVE),
            Pattern.compile("성공률|만족도\\s*\\d|평점|별점|점수"),
            // 결과·재현 보장
            Pattern.compile("보장|확실히|틀림없|반드시\\s*(같|나오|된)|똑같이\\s*(나오|가능)|재현\\s*(가능|됩)"),
            Pattern.compile("완벽|100%|무조건"),
            // 시술 가능성·시술법 추론 ("확인 가능합니다" 같은 정상 표현까지 막지 않도록 좁게 잡는다)
            Pattern.compile("시술\\s*가능|동일하게\\s*가능|모발\\s*(상태|손상)|약제|파마약|탈색|염모제"),
            Pattern.compile("커트\\s*방법|시술법|기법으로\\s*(했|시술)"),
            // 인적 속성 추론
            Pattern.compile("얼굴형|이목구비|외모|미인|잘생|동안|나이대?\\s*\\d|\\d+\\s*대\\s*(초|중|후)"),
            Pattern.compile("남성|여성|남자|여자|성별|인종|국적")
    );

    /** 금지 표현이 하나라도 있으면 true. */
    public boolean violates(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.KOREAN);
        return PATTERNS.stream().anyMatch(p -> p.matcher(normalized).find());
    }
}
