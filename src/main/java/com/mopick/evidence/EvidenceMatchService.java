package com.mopick.evidence;

import com.mopick.ai.PairwiseAiResponse;
import com.mopick.ai.PairwiseComparator;
import com.mopick.ai.image.ImageSanitizer;
import com.mopick.portfolio.PortfolioEntry;
import com.mopick.portfolio.PortfolioRepository;
import com.mopick.stylespec.ConfirmedSpec;
import com.mopick.stylespec.StyleField;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * 후보 사례별로 근거를 만든다.
 *
 * <p>순서가 중요하다. 확정 태그로 뼈대를 먼저 완성한 다음에만 AI를 얹는다.
 * 그래서 AI가 없거나 실패해도 응답 구조는 동일하고, AI는 후보 포함 여부와 순서에 관여할 수 없다.
 */
@Service
public class EvidenceMatchService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceMatchService.class);

    /** 후보는 최대 3명이므로 그만큼만 동시에 돈다. */
    private static final int MAX_CANDIDATES = 3;
    private static final long AI_TIMEOUT_SECONDS = 25;

    private final PortfolioRepository portfolioRepository;
    private final ClaimSkeletonBuilder skeletonBuilder;
    private final PairwiseComparator pairwiseComparator;
    private final ClaimValidator claimValidator;
    private final ImageSanitizer imageSanitizer;

    public EvidenceMatchService(PortfolioRepository portfolioRepository,
                                ClaimSkeletonBuilder skeletonBuilder,
                                PairwiseComparator pairwiseComparator,
                                ClaimValidator claimValidator,
                                ImageSanitizer imageSanitizer) {
        this.portfolioRepository = portfolioRepository;
        this.skeletonBuilder = skeletonBuilder;
        this.pairwiseComparator = pairwiseComparator;
        this.claimValidator = claimValidator;
        this.imageSanitizer = imageSanitizer;
    }

    /** 한 후보에 대한 근거 묶음. */
    public record CandidateEvidence(
            String portfolioId,
            String stylistName,
            String region,
            String dataVersion,
            List<EvidenceClaim> claims,
            EvidenceMode mode
    ) {
    }

    public enum EvidenceMode {
        /** 확정 태그만으로 만든 근거. 사진 없음 / API key 없음 / AI 실패 시. */
        CONFIRMED_EVIDENCE,
        /** 확정 태그 뼈대 위에 검증을 통과한 AI 설명이 반영된 근거. */
        AI_EVIDENCE
    }

    public List<CandidateEvidence> match(ConfirmedSpec goal, byte[] goalImageJpeg, List<String> portfolioIds) {
        List<PortfolioEntry> candidates = revalidate(portfolioIds);
        if (candidates.isEmpty()) {
            return List.of();
        }

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(candidates.size(), MAX_CANDIDATES));
        try {
            List<CompletableFuture<CandidateEvidence>> futures = candidates.stream()
                    .map(entry -> CompletableFuture
                            .supplyAsync(() -> evaluate(goal, goalImageJpeg, entry), pool)
                            .exceptionally(ex -> {
                                log.warn("후보 {} 처리 실패 - 확정 태그 근거로 대체: {}",
                                        entry.portfolioId(), ex.toString());
                                return fallback(goal, entry);
                            }))
                    .toList();

            List<CandidateEvidence> results = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                try {
                    results.add(futures.get(i).get(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS));
                } catch (Exception e) {
                    log.warn("후보 {} 시간 초과 - 확정 태그 근거로 대체", candidates.get(i).portfolioId());
                    results.add(fallback(goal, candidates.get(i)));
                }
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * 후보 재검증. 존재하지 않거나 검수를 통과하지 못한 사례는 조용히 떨어뜨린다.
     * 매칭 단계에서 통과했더라도 그 사이 게시가 내려갔을 수 있으므로 매번 다시 본다.
     */
    private List<PortfolioEntry> revalidate(List<String> portfolioIds) {
        if (portfolioIds == null) {
            return List.of();
        }
        return portfolioIds.stream()
                .distinct()
                .limit(MAX_CANDIDATES)
                .map(portfolioRepository::findById)
                .flatMap(java.util.Optional::stream)
                .filter(entry -> {
                    if (!entry.isMatchable()) {
                        log.info("검수 미통과 후보 제외: {}", entry.portfolioId());
                        return false;
                    }
                    return true;
                })
                .toList();
    }

    private CandidateEvidence evaluate(ConfirmedSpec goal, byte[] goalImageJpeg, PortfolioEntry entry) {
        ConfirmedSpec portfolioSpec = ConfirmedSpec.of(entry.confirmedTags());
        List<EvidenceClaim> skeleton = skeletonBuilder.build(goal, portfolioSpec, entry.portfolioId());

        byte[] portfolioImage = loadPortfolioImage(entry);
        if (goalImageJpeg == null || portfolioImage == null) {
            return toCandidate(entry, skeleton, EvidenceMode.CONFIRMED_EVIDENCE);
        }

        Map<StyleField, PairwiseAiResponse.ClaimDto> aiClaims = pairwiseComparator.compare(
                goalImageJpeg, portfolioImage, goal, portfolioSpec, requiredStates(skeleton));
        if (aiClaims.isEmpty()) {
            return toCandidate(entry, skeleton, EvidenceMode.CONFIRMED_EVIDENCE);
        }

        ClaimValidator.MergeResult result = claimValidator.merge(skeleton, aiClaims);
        log.debug("후보 {} 병합: AI 반영 {}건, 반려 {}건",
                entry.portfolioId(), result.aiAppliedCount(), result.rejectedCount());
        EvidenceMode mode = result.aiAppliedCount() > 0
                ? EvidenceMode.AI_EVIDENCE
                : EvidenceMode.CONFIRMED_EVIDENCE;
        return toCandidate(entry, result.claims(), mode);
    }

    /**
     * 확정 태그가 이미 상태를 정해버린 필드만 추린다. 이 목록을 프롬프트에 "고정"으로 넘겨
     * 모델이 상태를 새로 고르려다 반려당하는 낭비를 줄인다. 검증은 {@link ClaimValidator}가 그대로 한다.
     */
    private Map<StyleField, String> requiredStates(List<EvidenceClaim> skeleton) {
        Map<StyleField, String> required = new EnumMap<>(StyleField.class);
        for (EvidenceClaim claim : skeleton) {
            if (claim.state() == ClaimState.MATCH || claim.state() == ClaimState.DIFFERENCE) {
                required.put(claim.field(), claim.state().name());
            }
        }
        return required;
    }

    private CandidateEvidence fallback(ConfirmedSpec goal, PortfolioEntry entry) {
        List<EvidenceClaim> skeleton = skeletonBuilder.build(
                goal, ConfirmedSpec.of(entry.confirmedTags()), entry.portfolioId());
        return toCandidate(entry, skeleton, EvidenceMode.CONFIRMED_EVIDENCE);
    }

    private CandidateEvidence toCandidate(PortfolioEntry entry, List<EvidenceClaim> claims, EvidenceMode mode) {
        return new CandidateEvidence(entry.portfolioId(), entry.stylistName(), entry.region(),
                entry.dataVersion(), claims, mode);
    }

    /** fixture 이미지는 없을 수 있다. 없으면 1:1 비교를 건너뛰고 태그 근거만 쓴다. */
    private byte[] loadPortfolioImage(PortfolioEntry entry) {
        if (entry.imagePath() == null || entry.imagePath().isBlank()) {
            return null;
        }
        ClassPathResource resource = new ClassPathResource(entry.imagePath());
        if (!resource.exists()) {
            log.debug("작업 사진 없음: {}", entry.imagePath());
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            return imageSanitizer.sanitize(in).bytes();
        } catch (IOException e) {
            log.warn("작업 사진 로드 실패 {}: {}", entry.imagePath(), e.toString());
            return null;
        }
    }
}
