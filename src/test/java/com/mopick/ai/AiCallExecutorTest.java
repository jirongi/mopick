package com.mopick.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * 무료 티어 429가 그대로 빈 결과로 흘러 사용자에게 "관찰 실패"처럼 보였던 문제 때문에 추가한 테스트.
 * 재시도해도 소용없는 실패까지 붙잡고 있으면 응답만 느려지므로 구분이 중요하다.
 */
class AiCallExecutorTest {

    // 한도는 넉넉히 — 여기서 검증하려는 건 재시도 판별이지 한도가 아니다.
    private final AiCallExecutor executor =
            new AiCallExecutor(new AiRateLimiter(1000, 60_000, 1_000));

    @Test
    void 한도_초과는_다시_시도할_수_있는_실패로_본다() {
        assertThat(executor.isTransient(new RuntimeException(
                "Failed to generate content", new IllegalStateException("429 RESOURCE_EXHAUSTED")))).isTrue();
        assertThat(executor.isTransient(new RuntimeException("quota exceeded"))).isTrue();
        assertThat(executor.isTransient(new RuntimeException("503 Service Unavailable"))).isTrue();
        assertThat(executor.isTransient(new RuntimeException("read timed out"))).isTrue();
    }

    @Test
    void 인증_실패나_잘못된_요청은_재시도하지_않는다() {
        assertThat(executor.isTransient(new RuntimeException("401 API key not valid"))).isFalse();
        assertThat(executor.isTransient(new RuntimeException("400 Invalid argument"))).isFalse();
    }

    @Test
    void 재시도_불가_실패는_즉시_포기하고_빈_값을_준다() {
        AtomicInteger calls = new AtomicInteger();
        Optional<String> result = executor.call("테스트", () -> {
            calls.incrementAndGet();
            throw new RuntimeException("401 API key not valid");
        });

        assertThat(result).isEmpty();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void 성공하면_재시도하지_않는다() {
        AtomicInteger calls = new AtomicInteger();
        Optional<String> result = executor.call("테스트", () -> {
            calls.incrementAndGet();
            return "ok";
        });

        assertThat(result).contains("ok");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void 응답이_알려준_대기_시간을_따른다() {
        // 고정 백오프로 성급히 재시도하면 한도만 더 태우고 전부 실패한다.
        assertThat(executor.retryHint(new RuntimeException("429 quota. Please retry in 40.85s")))
                .contains(41350L);
        assertThat(executor.retryHint(new RuntimeException("429 quota exceeded"))).isEmpty();
    }

    @Test
    void 대기_시간이_너무_길면_붙잡지_않고_포기한다() {
        assertThat(executor.retryHint(new RuntimeException("Please retry in 600s"))).isEmpty();
    }

    @Test
    void 예외를_밖으로_던지지_않는다() {
        assertThat(executor.call("테스트", () -> {
            throw new RuntimeException("400 Invalid argument");
        })).isEmpty();
    }
}
