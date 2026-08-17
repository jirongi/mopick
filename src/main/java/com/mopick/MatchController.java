package com.mopick.api;

import com.mopick.api.dto.MatchRequest;
import com.mopick.api.dto.MatchResponse;
import com.mopick.match.MatchService;
import com.mopick.stylespec.ConfirmedSpec;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 결정론적 후보 매칭. AI를 부르지 않으므로 API 키 없이도 완전히 동작한다. */
@RestController
public class MatchController {

    private static final Logger log = LoggerFactory.getLogger(MatchController.class);

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/api/matches")
    public MatchResponse match(@RequestBody MatchRequest request) {
        String analysisId = UUID.randomUUID().toString();

        MatchService.MatchResult result = matchService.match(new MatchService.MatchCriteria(
                ConfirmedSpec.of(request.confirmedTags()),
                request.region(),
                request.avoidStylistIds() == null ? Set.of() : Set.copyOf(request.avoidStylistIds())));

        log.info("matches analysisId={} region={} → 후보 {}명",
                analysisId, request.region(), result.candidates().size());
        return new MatchResponse(analysisId, List.copyOf(result.candidates()), result.reason());
    }
}
