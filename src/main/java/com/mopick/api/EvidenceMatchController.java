package com.mopick.api;

import com.mopick.api.dto.EvidenceMatchRequest;
import com.mopick.api.dto.EvidenceMatchResponse;
import com.mopick.ai.image.ImageSanitizer;
import com.mopick.evidence.EvidenceMatchService;
import com.mopick.stylespec.ConfirmedSpec;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 후보 사례와 희망 사진을 비교해 근거를 만든다. 후보 포함 여부·순서는 여기서 바뀌지 않는다. */
@RestController
public class EvidenceMatchController {

    private static final Logger log = LoggerFactory.getLogger(EvidenceMatchController.class);

    private final EvidenceMatchService evidenceMatchService;
    private final ImageSanitizer imageSanitizer;

    public EvidenceMatchController(EvidenceMatchService evidenceMatchService, ImageSanitizer imageSanitizer) {
        this.evidenceMatchService = evidenceMatchService;
        this.imageSanitizer = imageSanitizer;
    }

    @PostMapping("/api/evidence-match")
    public EvidenceMatchResponse match(@Valid @RequestBody EvidenceMatchRequest request) {
        String requestId = UUID.randomUUID().toString();
        ConfirmedSpec goal = ConfirmedSpec.of(request.confirmedTags());
        byte[] goalImage = decodeImage(request.goalImageBase64());

        var candidates = evidenceMatchService.match(goal, goalImage, request.portfolioIds());
        log.info("evidence-match requestId={} 요청 {}건 → 응답 {}건",
                requestId, request.portfolioIds().size(), candidates.size());
        return EvidenceMatchResponse.from(requestId, candidates);
    }

    /** 사진이 없거나 깨져도 실패시키지 않는다. 확정 태그 근거만으로 응답하면 된다. */
    private byte[] decodeImage(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            String payload = base64.contains(",") ? base64.substring(base64.indexOf(',') + 1) : base64;
            return imageSanitizer.sanitize(Base64.getDecoder().decode(payload)).bytes();
        } catch (IllegalArgumentException | IOException e) {
            log.warn("희망 사진 디코드 실패 - 확정 태그 근거로 진행: {}", e.toString());
            return null;
        }
    }
}
