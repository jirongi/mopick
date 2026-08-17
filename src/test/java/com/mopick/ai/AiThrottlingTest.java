package com.mopick.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopick.stylespec.AnalysisStatus;
import com.mopick.stylespec.StyleSpec;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** 무료 티어 한도 안에서 버티기 위한 두 장치 — 줄 세우기와 캐시. */
class AiThrottlingTest {

    private final byte[] photoA = "가짜 사진 A".getBytes(StandardCharsets.UTF_8);
    private final byte[] photoB = "가짜 사진 B".getBytes(StandardCharsets.UTF_8);

    private StyleSpec ok() {
        return new StyleSpec(StyleSpec.retake("x").observations(), false, null, AnalysisStatus.OK);
    }

    // --- 줄 세우기 ---

    @Test
    void 한도까지는_기다리지_않고_통과시킨다() {
        AiRateLimiter limiter = new AiRateLimiter(3, 60_000, 0);

        for (int i = 0; i < 3; i++) {
            assertThat(limiter.acquire()).isTrue();
        }
    }

    @Test
    void 한도를_넘으면_대기하고_기다릴_수_없으면_포기한다() {
        // maxWait=0 → 기다릴 수 없으니 바로 false. 호출자는 AI 없이 fallback으로 내려간다.
        AiRateLimiter limiter = new AiRateLimiter(2, 60_000, 0);
        limiter.acquire();
        limiter.acquire();

        assertThat(limiter.acquire()).isFalse();
    }

    @Test
    void 창이_지나면_자리가_다시_난다() {
        AiRateLimiter limiter = new AiRateLimiter(2, 150, 5_000);
        limiter.acquire();
        limiter.acquire();

        // 창(150ms)이 지나갈 만큼 기다려주면 통과해야 한다.
        assertThat(limiter.acquire()).isTrue();
    }

    @Test
    void 호출하지_않은_자리는_돌려준다() {
        AiRateLimiter limiter = new AiRateLimiter(1, 60_000, 0);
        limiter.acquire();
        limiter.release();

        assertThat(limiter.acquire()).isTrue();
    }

    // --- 캐시 ---

    @Test
    void 같은_사진과_같은_프롬프트는_캐시에서_돌려준다() {
        StyleSpecCache cache = new StyleSpecCache(true, 10, 60_000);
        cache.put(photoA, "프롬프트 v1", ok());

        assertThat(cache.get(photoA, "프롬프트 v1")).isPresent();
    }

    @Test
    void 프롬프트가_바뀌면_캐시가_적중하지_않는다() {
        // 어휘나 판정 기준을 고쳤는데 예전 결과가 나오면 개선이 반영 안 된 줄 모르고 측정하게 된다.
        StyleSpecCache cache = new StyleSpecCache(true, 10, 60_000);
        cache.put(photoA, "프롬프트 v1", ok());

        assertThat(cache.get(photoA, "프롬프트 v2")).isEmpty();
    }

    @Test
    void 다른_사진은_캐시가_적중하지_않는다() {
        StyleSpecCache cache = new StyleSpecCache(true, 10, 60_000);
        cache.put(photoA, "프롬프트 v1", ok());

        assertThat(cache.get(photoB, "프롬프트 v1")).isEmpty();
    }

    @Test
    void 실패한_분석은_캐시하지_않는다() {
        // 한도 초과로 실패한 결과가 굳어버리면 잠시 후 재시도해도 계속 빈 결과가 나온다.
        StyleSpecCache cache = new StyleSpecCache(true, 10, 60_000);
        cache.put(photoA, "프롬프트 v1", StyleSpec.unavailable(AnalysisStatus.AI_FAILED));

        assertThat(cache.get(photoA, "프롬프트 v1")).isEmpty();
        assertThat(cache.size()).isZero();
    }

    @Test
    void 수명이_지난_항목은_돌려주지_않는다() {
        StyleSpecCache cache = new StyleSpecCache(true, 10, 0);
        cache.put(photoA, "프롬프트 v1", ok());

        assertThat(cache.get(photoA, "프롬프트 v1")).isEmpty();
    }

    @Test
    void 최대_개수를_넘으면_오래된_것부터_버린다() {
        StyleSpecCache cache = new StyleSpecCache(true, 2, 60_000);
        cache.put("1".getBytes(StandardCharsets.UTF_8), "p", ok());
        cache.put("2".getBytes(StandardCharsets.UTF_8), "p", ok());
        cache.put("3".getBytes(StandardCharsets.UTF_8), "p", ok());

        assertThat(cache.size()).isEqualTo(2);
        assertThat(cache.get("1".getBytes(StandardCharsets.UTF_8), "p")).isEmpty();
        assertThat(cache.get("3".getBytes(StandardCharsets.UTF_8), "p")).isPresent();
    }

    @Test
    void 캐시를_끄면_아무것도_돌려주지_않는다() {
        StyleSpecCache cache = new StyleSpecCache(false, 10, 60_000);
        cache.put(photoA, "프롬프트 v1", ok());

        assertThat(cache.get(photoA, "프롬프트 v1")).isEmpty();
    }
}
