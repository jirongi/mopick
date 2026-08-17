package com.mopick.ai;

import com.mopick.stylespec.AnalysisStatus;
import com.mopick.stylespec.Confidence;
import com.mopick.stylespec.ObservationState;
import com.mopick.stylespec.StyleField;
import com.mopick.stylespec.StyleObservation;
import com.mopick.stylespec.StyleSpec;
import com.mopick.stylespec.Vocabulary;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

/**
 * 사진 1장 → 8개 필드 StyleSpec. 희망 사진과 작업 사진이 같은 스키마·같은 어휘를 공유하고
 * 프롬프트만 다르다. 두 쪽 태그가 같은 기준으로 나와야 이후 비교가 성립한다.
 */
@Service
public class StyleSpecAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(StyleSpecAnalyzer.class);

    private final ChatClient chatClient;
    private final AiAvailability availability;
    private final AiCallExecutor executor;
    private final StyleSpecCache cache;

    public StyleSpecAnalyzer(ChatClient.Builder builder, AiAvailability availability,
                             AiCallExecutor executor, StyleSpecCache cache) {
        this.chatClient = builder.build();
        this.availability = availability;
        this.executor = executor;
        this.cache = cache;
    }

    public StyleSpec analyzeGoal(byte[] jpeg) {
        return analyze(jpeg, Prompts.observeGoal());
    }

    public StyleSpec analyzePortfolio(byte[] jpeg) {
        return analyze(jpeg, Prompts.observePortfolio());
    }

    private StyleSpec analyze(byte[] jpeg, String instruction) {
        if (!availability.isEnabled()) {
            // 키가 없으면 관찰 자체가 불가능하다. 빈 스펙을 주고 사용자가 직접 태그를 고르게 한다.
            log.info("API 키 미설정 - 분석 없이 빈 StyleSpec 반환");
            return StyleSpec.unavailable(AnalysisStatus.AI_DISABLED);
        }
        Optional<StyleSpec> cached = cache.get(jpeg, instruction);
        if (cached.isPresent()) {
            return cached.get();
        }

        Optional<StyleSpecAiResponse> raw = executor.call("StyleSpec 분석", () ->
                chatClient.prompt()
                        .user(u -> u.text(instruction)
                                .media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(jpeg)))
                        .call()
                        .entity(StyleSpecAiResponse.class));

        // 실패를 관찰 결과처럼 보이게 두면 안 된다. 상태로 구분해서 내려보낸다.
        StyleSpec spec = raw.map(this::toStyleSpec)
                .orElseGet(() -> StyleSpec.unavailable(AnalysisStatus.AI_FAILED));
        cache.put(jpeg, instruction, spec);
        return spec;
    }

    /**
     * 모델 응답을 도메인 모델로 옮기면서 검증한다.
     *
     * <p>사전에 없는 value, 모르는 state, 누락된 필드는 모두 여기서 정리된다.
     * OBSERVED인데 값이 사전에 없으면 값을 신뢰할 수 없으므로 UNCLEAR로 낮춘다.
     */
    StyleSpec toStyleSpec(StyleSpecAiResponse raw) {
        if (raw == null) {
            return StyleSpec.unavailable(AnalysisStatus.AI_FAILED);
        }
        Map<StyleField, StyleSpecAiResponse.ObservationDto> byField = new EnumMap<>(StyleField.class);
        if (raw.observations() != null) {
            for (StyleSpecAiResponse.ObservationDto dto : raw.observations()) {
                StyleField field = parseField(dto == null ? null : dto.field());
                if (field != null) {
                    byField.putIfAbsent(field, dto);
                }
            }
        }

        List<StyleObservation> observations = new ArrayList<>();
        for (StyleField field : StyleField.values()) {
            observations.add(toObservation(field, byField.get(field)));
        }

        boolean retake = Boolean.TRUE.equals(raw.retake());
        String reason = retake ? blankToNull(raw.retakeReason()) : null;
        if (retake && reason == null) {
            reason = "사진을 다시 올려주세요.";
        }
        return new StyleSpec(observations, retake, reason, AnalysisStatus.OK);
    }

    private StyleObservation toObservation(StyleField field, StyleSpecAiResponse.ObservationDto dto) {
        if (dto == null) {
            return StyleObservation.notVisible(field);
        }
        ObservationState state = parseState(dto.state());
        if (state != ObservationState.OBSERVED) {
            return new StyleObservation(field, state, null, null);
        }
        String value = Vocabulary.normalize(field, dto.value());
        if (value == null) {
            // 관찰했다면서 사전에 없는 값을 냈다 → 값을 인정하지 않는다.
            log.debug("어휘 사전에 없는 값 무시: field={}, value={}", field, dto.value());
            return StyleObservation.unclear(field);
        }
        return new StyleObservation(field, ObservationState.OBSERVED, value, parseConfidence(dto.confidence()));
    }

    private StyleField parseField(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return StyleField.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ObservationState parseState(String raw) {
        if (raw == null) {
            return ObservationState.NOT_VISIBLE;
        }
        try {
            return ObservationState.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ObservationState.UNCLEAR;
        }
    }

    private Confidence parseConfidence(String raw) {
        if (raw == null) {
            return Confidence.LOW;
        }
        try {
            return Confidence.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Confidence.LOW;
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
