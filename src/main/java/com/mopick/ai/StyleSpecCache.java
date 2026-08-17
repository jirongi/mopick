package com.mopick.ai;

import com.mopick.stylespec.StyleSpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 같은 사진을 같은 기준으로 다시 분석하지 않는다.
 *
 * <p>데모나 테스트에서는 같은 사진을 반복해 올리게 되는데, 그때마다 API를 부르면
 * 한도만 태운다. 사진 내용이 같고 프롬프트도 같으면 결과도 같아야 하므로 캐시가 성립한다.
 *
 * <p>키에 <b>프롬프트 지문을 반드시 포함</b>한다. 어휘나 판정 기준을 고쳤는데 예전 결과가
 * 그대로 나오면 개선이 반영되지 않은 것을 모른 채 측정하게 된다.
 *
 * <p>실패는 캐시하지 않는다. 한도 초과로 실패한 결과가 굳어버리면 안 된다.
 * 원본 사진은 저장하지 않으므로 키는 바이트 해시만 쓴다.
 */
@Component
public class StyleSpecCache {

    private static final Logger log = LoggerFactory.getLogger(StyleSpecCache.class);

    private record Entry(StyleSpec spec, long storedAt) {
    }

    private final boolean enabled;
    private final int maxEntries;
    private final long ttlMs;

    private final Map<String, Entry> store;

    public StyleSpecCache(
            @Value("${mopick.ai.cache.enabled:true}") boolean enabled,
            @Value("${mopick.ai.cache.max-entries:200}") int maxEntries,
            @Value("${mopick.ai.cache.ttl-ms:21600000}") long ttlMs) {
        this.enabled = enabled;
        this.maxEntries = maxEntries;
        this.ttlMs = ttlMs;
        this.store = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                return size() > StyleSpecCache.this.maxEntries;
            }
        };
    }

    public Optional<StyleSpec> get(byte[] image, String instruction) {
        if (!enabled) {
            return Optional.empty();
        }
        String key = key(image, instruction);
        synchronized (store) {
            Entry entry = store.get(key);
            if (entry == null) {
                return Optional.empty();
            }
            // 나이가 수명에 도달하면 만료. ttl 0은 "저장하자마자 만료"를 뜻한다.
            if (System.currentTimeMillis() - entry.storedAt() >= ttlMs) {
                store.remove(key);
                return Optional.empty();
            }
            log.debug("StyleSpec 캐시 적중 - API 호출 생략");
            return Optional.of(entry.spec());
        }
    }

    /** 성공한 분석만 저장한다. */
    public void put(byte[] image, String instruction, StyleSpec spec) {
        if (!enabled || spec == null || spec.status() != com.mopick.stylespec.AnalysisStatus.OK) {
            return;
        }
        synchronized (store) {
            store.put(key(image, instruction), new Entry(spec, System.currentTimeMillis()));
        }
    }

    public void clear() {
        synchronized (store) {
            store.clear();
        }
    }

    int size() {
        synchronized (store) {
            return store.size();
        }
    }

    /** 사진 내용 + 프롬프트 내용. 둘 중 하나만 달라도 다른 키가 된다. */
    private String key(byte[] image, String instruction) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(image);
            digest.update(instruction.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
