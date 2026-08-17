package com.mopick.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 사진 분석 호출이 가능한 상태인지 판단한다.
 *
 * <p>키가 없어도 애플리케이션은 정상 기동해야 하고, 세 API 모두 fallback으로 응답해야 한다.
 * 그래서 기동 시 예외를 던지는 대신 호출 직전에 이 값을 확인한다.
 *
 * <p>모델 제공자를 바꾸면 여기 프로퍼티 이름도 함께 바꾼다. 나머지 AI 코드는
 * Spring AI {@code ChatClient}만 쓰므로 제공자에 묶여 있지 않다.
 */
@Component
public class AiAvailability {

    private static final String NOT_CONFIGURED = "disabled";

    private final boolean enabled;

    public AiAvailability(@Value("${spring.ai.google.genai.api-key:}") String apiKey) {
        this.enabled = apiKey != null && !apiKey.isBlank() && !NOT_CONFIGURED.equals(apiKey);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
