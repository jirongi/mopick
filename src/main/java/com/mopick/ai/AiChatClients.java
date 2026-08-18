package com.mopick.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 우선순위가 있는 AI 제공자 목록.
 *
 * <p>앞의 제공자가 실패하면 다음으로 넘어간다. 정확도가 가장 높은 제공자를 앞에 두고,
 * 그 제공자의 한도가 마르는 순간에만 뒤로 내려간다. 평소에는 앞 제공자만 쓰인다.
 *
 * <p>측정 결과 Gemini가 정확도·안정성 모두 앞서므로 기본 순서는 Gemini → OpenAI다.
 * 두 모델을 합쳐 쓰는 방식(합의 앙상블)은 실측에서 오히려 나빴다. 의견이 갈린 필드에서
 * Gemini가 전부 옳았기 때문에, 합의를 요구하면 맞는 답을 버리게 된다.
 *
 * <p>키가 설정되지 않은 제공자는 체인에서 빠진다. 체인이 비면 AI 없이 fallback으로 동작한다.
 */
@Component
public class AiChatClients {

    private static final Logger log = LoggerFactory.getLogger(AiChatClients.class);
    private static final String NOT_CONFIGURED = "disabled";

    /** 체인의 한 칸. 로그에 이름을 남겨 어느 제공자가 응답했는지 알 수 있게 한다. */
    public record Provider(String name, ChatClient client) {
    }

    private final List<Provider> chain;

    public AiChatClients(
            @Value("${mopick.ai.providers:google-genai,openai}") String providerOrder,
            @Value("${spring.ai.google.genai.api-key:}") String geminiKey,
            @Value("${spring.ai.openai.api-key:}") String openAiKey,
            ObjectProvider<GoogleGenAiChatModel> geminiModel,
            ObjectProvider<OpenAiChatModel> openAiModel) {

        List<Provider> built = new ArrayList<>();
        for (String raw : providerOrder.split(",")) {
            String name = raw.trim().toLowerCase(Locale.ROOT);
            if (name.isEmpty()) {
                continue;
            }
            ChatModel model = switch (name) {
                case "google-genai" -> usable(geminiKey) ? geminiModel.getIfAvailable() : null;
                case "openai" -> usable(openAiKey) ? openAiModel.getIfAvailable() : null;
                default -> null;
            };
            if (model == null) {
                log.info("AI 제공자 '{}' 건너뜀 (키 미설정 또는 미탑재)", name);
                continue;
            }
            built.add(new Provider(name, ChatClient.create(model)));
        }
        this.chain = List.copyOf(built);

        if (chain.isEmpty()) {
            log.warn("사용 가능한 AI 제공자가 없다 - 확정 태그 fallback으로만 동작한다");
        } else {
            log.info("AI 제공자 체인: {}", chain.stream().map(Provider::name).toList());
        }
    }

    private boolean usable(String key) {
        return key != null && !key.isBlank() && !NOT_CONFIGURED.equals(key);
    }

    /** 우선순위 순. 비어 있으면 AI를 쓸 수 없다. */
    public List<Provider> chain() {
        return chain;
    }

    public boolean isEnabled() {
        return !chain.isEmpty();
    }

    public List<String> names() {
        return chain.stream().map(Provider::name).toList();
    }

    static List<String> parse(String order) {
        return Arrays.stream(order.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
