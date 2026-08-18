package com.mopick.ai;

import com.mopick.stylespec.ConfirmedSpec;
import com.mopick.stylespec.StyleField;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

/**
 * 희망 사진과 작업 사진을 1:1로 비교한다.
 *
 * <p>여기서 나온 결과는 그 자체로 사용자에게 가지 않는다.
 * {@code EvidenceMatchService}가 확정 태그 뼈대와 대조해 통과한 것만 반영한다.
 */
@Service
public class PairwiseComparator {

    private static final Logger log = LoggerFactory.getLogger(PairwiseComparator.class);

    private final AiChatClients clients;
    private final AiCallExecutor executor;

    public PairwiseComparator(AiChatClients clients, AiCallExecutor executor) {
        this.clients = clients;
        this.executor = executor;
    }

    /**
     * 필드별 (상태, 설명). 호출이 불가능하거나 실패하면 빈 맵 → 호출자는 뼈대를 그대로 쓴다.
     *
     * @param requiredStates 확정 태그가 이미 상태를 정한 필드. 프롬프트에 "고정"으로 명시해
     *                       모델이 다른 상태를 고르거나 상태 자리에 태그 값을 넣는 일을 줄인다.
     */
    public Map<StyleField, PairwiseAiResponse.ClaimDto> compare(
            byte[] goalJpeg, byte[] portfolioJpeg, ConfirmedSpec goal, ConfirmedSpec portfolio,
            Map<StyleField, String> requiredStates) {

        if (!clients.isEnabled() || goalJpeg == null || portfolioJpeg == null) {
            return Map.of();
        }
        String instruction = Prompts.pairwise(confirmedContext(goal, portfolio, requiredStates));
        Optional<PairwiseAiResponse> raw = executor.callChain("1:1 비교", clients.chain(),
                provider -> provider.client().prompt()
                        .user(u -> u.text(instruction)
                                .media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(goalJpeg))
                                .media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(portfolioJpeg)))
                        .call()
                        .entity(PairwiseAiResponse.class));

        // 실패하면 빈 맵 → 호출자가 확정 태그 근거만으로 응답한다.
        Map<StyleField, PairwiseAiResponse.ClaimDto> indexed = raw.map(this::index).orElseGet(Map::of);
        if (log.isDebugEnabled()) {
            indexed.forEach((field, dto) ->
                    log.debug("AI 원시 비교 결과: field={}, state={}, explanation={}",
                            field, dto.state(), dto.explanation()));
        }
        return indexed;
    }

    private Map<StyleField, PairwiseAiResponse.ClaimDto> index(PairwiseAiResponse raw) {
        Map<StyleField, PairwiseAiResponse.ClaimDto> map = new EnumMap<>(StyleField.class);
        if (raw == null || raw.claims() == null) {
            return map;
        }
        for (PairwiseAiResponse.ClaimDto dto : raw.claims()) {
            if (dto == null || dto.field() == null) {
                continue;
            }
            try {
                map.putIfAbsent(StyleField.valueOf(dto.field().trim().toUpperCase(Locale.ROOT)), dto);
            } catch (IllegalArgumentException ignored) {
                // 모르는 필드명은 버린다.
            }
        }
        return map;
    }

    private String confirmedContext(ConfirmedSpec goal, ConfirmedSpec portfolio,
                                    Map<StyleField, String> requiredStates) {
        Map<StyleField, String> g = goal.valueByField();
        Map<StyleField, String> p = portfolio.valueByField();
        StringBuilder sb = new StringBuilder();
        for (StyleField field : StyleField.values()) {
            String required = requiredStates.get(field);
            sb.append("- ").append(field.name()).append(" (").append(field.label()).append("): ")
                    .append("희망=").append(g.get(field) == null ? "미정" : g.get(field))
                    .append(", 작업사례=").append(p.get(field) == null ? "미기록" : p.get(field))
                    .append(" → ").append(required == null ? "state 직접 판단" : "state=" + required + " 고정")
                    .append('\n');
        }
        return sb.toString();
    }
}
