package com.mopick.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 호출을 스스로 분당 한도 안으로 눌러 담는다.
 *
 * <p>한도를 넘겨 429를 받고 재시도하면 그 재시도가 다시 한도를 먹어 상황이 더 나빠진다.
 * 아예 넘기지 않도록 앞단에서 줄을 세우는 편이 낫다. 사용자 입장에서도
 * "실패했으니 다시 올려주세요"보다 몇 초 기다리는 쪽이 낫다.
 *
 * <p>슬라이딩 윈도우 방식이다. 최근 {@code window} 안의 호출 시각을 들고 있다가
 * 한도에 차 있으면 가장 오래된 호출이 창 밖으로 빠질 때까지 기다린다.
 */
@Component
public class AiRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AiRateLimiter.class);

    private final Object lock = new Object();
    private final Deque<Long> callTimes = new ArrayDeque<>();

    private final int maxCalls;
    private final long windowMs;
    private final long maxWaitMs;

    public AiRateLimiter(
            @Value("${mopick.ai.rate-limit.max-calls:20}") int maxCalls,
            @Value("${mopick.ai.rate-limit.window-ms:60000}") long windowMs,
            @Value("${mopick.ai.rate-limit.max-wait-ms:45000}") long maxWaitMs) {
        this.maxCalls = maxCalls;
        this.windowMs = windowMs;
        this.maxWaitMs = maxWaitMs;
    }

    /**
     * 호출 자리를 하나 얻는다. 자리가 없으면 날 때까지 기다린다.
     *
     * @return 자리를 얻었으면 true. 최대 대기 시간을 넘겼거나 인터럽트되면 false
     *         (호출자는 AI를 부르지 말고 fallback으로 내려가야 한다).
     */
    public boolean acquire() {
        long deadline = System.currentTimeMillis() + maxWaitMs;

        while (true) {
            long sleepMs;
            synchronized (lock) {
                long now = System.currentTimeMillis();
                purgeExpired(now);
                if (callTimes.size() < maxCalls) {
                    callTimes.addLast(now);
                    return true;
                }
                // 가장 오래된 호출이 창 밖으로 빠지는 순간까지. 경계에서 되튕기지 않게 여유를 둔다.
                sleepMs = callTimes.peekFirst() + windowMs - now + 50;
            }

            if (System.currentTimeMillis() + sleepMs > deadline) {
                log.warn("호출 한도 대기 시간 초과 - AI 없이 진행한다 (한도 {}회/{}ms)", maxCalls, windowMs);
                return false;
            }
            log.info("호출 한도에 걸려 {}ms 대기", sleepMs);
            if (!sleep(sleepMs)) {
                return false;
            }
        }
    }

    /** 실제로 호출이 이뤄지지 않았을 때 자리를 돌려준다. */
    public void release() {
        synchronized (lock) {
            callTimes.pollLast();
        }
    }

    private void purgeExpired(long now) {
        long cutoff = now - windowMs;
        while (!callTimes.isEmpty() && callTimes.peekFirst() <= cutoff) {
            callTimes.pollFirst();
        }
    }

    private boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
