package com.mopick.ai;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 사진 분석 호출이 가능한 상태인지.
 *
 * <p>키가 없어도 애플리케이션은 정상 기동해야 하고, 세 API 모두 fallback으로 응답해야 한다.
 * 실제 판단은 {@link AiChatClients}의 체인이 비었는지로 하고, 여기서는 그 결과를 노출만 한다.
 */
@Component
public class AiAvailability {

    private final AiChatClients clients;

    public AiAvailability(AiChatClients clients) {
        this.clients = clients;
    }

    public boolean isEnabled() {
        return clients.isEnabled();
    }

    /** 우선순위 순 제공자 이름. 응답·로그에서 어디를 쓰는지 확인할 때 쓴다. */
    public List<String> providers() {
        return clients.names();
    }
}
