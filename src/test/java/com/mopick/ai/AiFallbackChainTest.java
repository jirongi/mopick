package com.mopick.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 폴백 체인. 앞 제공자의 한도가 마르면 뒤 제공자가 받아야 하고,
 * 평소에는 앞 제공자만 쓰여야 한다(뒤가 정확도가 낮으므로).
 */
class AiFallbackChainTest {

    private final AiCallExecutor executor =
            new AiCallExecutor(new AiRateLimiter(1000, 60_000, 1_000));

    /** ChatClient를 만들지 않고 이름만으로 체인을 흉내 낸다. */
    private List<AiChatClients.Provider> chain(String... names) {
        List<AiChatClients.Provider> list = new ArrayList<>();
        for (String n : names) {
            list.add(new AiChatClients.Provider(n, null));
        }
        return list;
    }

    @Test
    void 첫_제공자가_성공하면_뒤는_부르지_않는다() {
        List<String> called = new ArrayList<>();

        Optional<String> result = executor.callChain("테스트", chain("google-genai", "openai"), p -> {
            called.add(p.name());
            return "ok:" + p.name();
        });

        assertThat(result).contains("ok:google-genai");
        assertThat(called).containsExactly("google-genai");
    }

    @Test
    void 첫_제공자가_한도로_실패하면_다음_제공자가_받는다() {
        List<String> called = new ArrayList<>();

        Optional<String> result = executor.callChain("테스트", chain("google-genai", "openai"), p -> {
            called.add(p.name());
            if ("google-genai".equals(p.name())) {
                // 재시도해도 소용없게 비일시적 오류로 즉시 포기시킨다.
                throw new RuntimeException("403 project denied");
            }
            return "ok:" + p.name();
        });

        assertThat(result).contains("ok:openai");
        assertThat(called).containsExactly("google-genai", "openai");
    }

    @Test
    void 모든_제공자가_실패하면_빈_값을_준다() {
        Optional<String> result = executor.callChain("테스트", chain("google-genai", "openai"), p -> {
            throw new RuntimeException("401 invalid key");
        });

        assertThat(result).isEmpty();
    }

    @Test
    void 체인이_비어_있으면_아무것도_부르지_않는다() {
        assertThat(executor.callChain("테스트", List.of(), p -> "ok")).isEmpty();
    }

    @Test
    void 제공자_순서_문자열을_그대로_해석한다() {
        assertThat(AiChatClients.parse("google-genai,openai")).containsExactly("google-genai", "openai");
        assertThat(AiChatClients.parse(" openai , google-genai ")).containsExactly("openai", "google-genai");
        assertThat(AiChatClients.parse("openai,,")).containsExactly("openai");
    }
}
