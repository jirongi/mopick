package com.mopick.ai;

import java.util.Locale;
import java.util.Optional;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AI 호출을 한 번 감싸서 일시적 실패를 재시도한다.
 *
 * <p>무료 티어는 분당 요청 수 제한이 있어 429가 흔하다. 그런데 google-genai SDK가 이를
 * {@code RuntimeException("Failed to generate content")}로 감싸버려서 Spring AI의
 * HTTP 코드 기반 재시도가 걸리지 않는다. 그래서 예외 메시지로 판별한다.
 *
 * <p>사용자가 사진을 올렸는데 순간적인 한도 초과 때문에 빈 결과를 받는 일은 없어야 한다.
 * 다만 무한정 붙잡고 있을 수도 없으므로 시도 횟수를 제한하고, 끝내 실패하면 호출자가
 * fallback으로 내려가게 빈 값을 돌려준다.
 */
@Component
public class AiCallExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiCallExecutor.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 3_000;
    private static final long MAX_BACKOFF_MS = 60_000;

    /**
     * 한도 초과 응답은 "Please retry in 40.85s"처럼 언제 풀리는지 알려준다.
     * 이를 무시하고 짧은 고정 백오프로 재시도하면 한도만 더 태우고 전부 실패한다.
     */
    private static final Pattern RETRY_HINT = Pattern.compile("retry in ([0-9]+(?:\\.[0-9]+)?)s");

    private final AiRateLimiter rateLimiter;

    public AiCallExecutor(AiRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * @return 성공하면 결과, 재시도 후에도 실패하면 빈 값. 예외를 던지지 않는다.
     */
    public <T> Optional<T> call(String label, Supplier<T> action) {
        long backoff = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // 한도를 넘겨 429를 받느니 앞단에서 기다린다. 429는 재시도까지 한도를 먹어 손해가 크다.
            if (!rateLimiter.acquire()) {
                log.warn("{} 포기 - 호출 한도 대기 시간 초과", label);
                return Optional.empty();
            }
            try {
                return Optional.ofNullable(action.get());
            } catch (Exception e) {
                boolean lastAttempt = attempt == MAX_ATTEMPTS;
                if (!isTransient(e) || lastAttempt) {
                    log.warn("{} 실패 ({}회 시도): {}", label, attempt, describe(e));
                    return Optional.empty();
                }
                long wait = retryHint(e).orElse(backoff);
                log.info("{} 일시 실패, {}ms 후 재시도 ({}/{}): {}",
                        label, wait, attempt, MAX_ATTEMPTS, describe(e));
                if (!sleep(wait)) {
                    return Optional.empty();
                }
                backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
            }
        }
        return Optional.empty();
    }

    /**
     * 다시 시도하면 성공할 수 있는 실패인지. 한도 초과·일시적 불가·타임아웃만 해당한다.
     * 잘못된 요청이나 인증 실패는 재시도해도 같으므로 바로 포기한다.
     */
    boolean isTransient(Throwable e) {
        String text = collectMessages(e).toLowerCase(Locale.ROOT);
        return text.contains("429")
                || text.contains("resource_exhausted")
                || text.contains("quota")
                || text.contains("rate limit")
                || text.contains("503")
                || text.contains("unavailable")
                || text.contains("overloaded")
                || text.contains("timeout")
                || text.contains("timed out");
    }

    /**
     * 제공자 체인을 순서대로 시도한다. 앞의 제공자가 재시도 후에도 실패하면 다음으로 넘어간다.
     *
     * <p>평소에는 첫 제공자에서 끝난다. 뒤 제공자는 앞이 한도에 걸리거나 죽었을 때만 쓰인다.
     * 정확도가 낮은 제공자라도 빈 결과보다는 낫다는 판단이다.
     *
     * @return 어느 제공자든 성공하면 그 결과, 전부 실패하면 빈 값
     */
    public <T> Optional<T> callChain(String label, List<AiChatClients.Provider> chain,
                                     Function<AiChatClients.Provider, T> action) {
        for (int i = 0; i < chain.size(); i++) {
            AiChatClients.Provider provider = chain.get(i);
            Optional<T> result = call(label + "[" + provider.name() + "]", () -> action.apply(provider));
            if (result.isPresent()) {
                if (i > 0) {
                    log.info("{} - 앞선 제공자 실패로 '{}'가 응답했다", label, provider.name());
                }
                return result;
            }
            if (i + 1 < chain.size()) {
                log.warn("{} - '{}' 실패, '{}'로 넘어간다", label, provider.name(), chain.get(i + 1).name());
            }
        }
        return Optional.empty();
    }

    /** 응답이 알려준 대기 시간(초)을 밀리초로. 없으면 빈 값. */
    Optional<Long> retryHint(Throwable e) {
        Matcher m = RETRY_HINT.matcher(collectMessages(e));
        if (!m.find()) {
            return Optional.empty();
        }
        long millis = (long) (Double.parseDouble(m.group(1)) * 1000);
        // 서버가 아주 긴 대기를 알려주면 붙잡고 있지 말고 포기하는 편이 낫다.
        return millis > MAX_BACKOFF_MS ? Optional.empty() : Optional.of(Math.max(millis + 500, 1000));
    }

    /** 원인 사슬 전체를 훑는다. SDK가 실제 사유를 안쪽 예외에 숨겨두기 때문이다. */
    private String collectMessages(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = e; t != null && sb.length() < 4000; t = t.getCause()) {
            sb.append(t.getClass().getName()).append(' ').append(t.getMessage()).append('\n');
            if (t.getCause() == t) {
                break;
            }
        }
        return sb.toString();
    }

    private String describe(Throwable e) {
        String messages = collectMessages(e).replace('\n', ' ');
        return messages.length() > 300 ? messages.substring(0, 300) + "..." : messages;
    }

    private boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
