package com.mopick.product.consultation;

import com.mopick.product.persistence.ConsultationCardEntity;
import com.mopick.product.persistence.ConsultationCardRepository;
import com.mopick.product.persistence.UserEntity;
import com.mopick.stylespec.ConfirmedTag;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class ConsultationCardService {

    private final ConsultationCardRepository consultationCardRepository;
    private final ObjectMapper objectMapper;

    public ConsultationCardService(ConsultationCardRepository consultationCardRepository, ObjectMapper objectMapper) {
        this.consultationCardRepository = consultationCardRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConsultationCardResponse save(UserEntity user, SaveConsultationCardRequest request) {
        String json = toJson(request.confirmedTags());
        ConsultationCardEntity entity = consultationCardRepository.save(new ConsultationCardEntity(
                user.getId(),
                request.title(),
                request.stylistId(),
                request.portfolioId(),
                request.stylistName(),
                request.salonName(),
                json));
        return toResponse(entity);
    }

    public List<ConsultationCardResponse> list(UserEntity user) {
        return consultationCardRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public ConsultationCardResponse get(UserEntity user, Long cardId) {
        ConsultationCardEntity entity = consultationCardRepository.findById(cardId)
                .filter(card -> card.getUserId().equals(user.getId()))
                .orElseThrow(() -> new com.mopick.api.ApiException("CARD_NOT_FOUND", "상담 카드를 찾을 수 없습니다."));
        return toResponse(entity);
    }

    private ConsultationCardResponse toResponse(ConsultationCardEntity entity) {
        return new ConsultationCardResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getStylistId(),
                entity.getPortfolioId(),
                entity.getStylistName(),
                entity.getSalonName(),
                fromJson(entity.getConfirmedTagsJson()),
                entity.getCreatedAt());
    }

    private String toJson(List<ConfirmedTag> tags) {
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (Exception e) {
            throw new IllegalStateException("상담 카드 태그 직렬화 실패", e);
        }
    }

    private List<ConfirmedTag> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ConfirmedTag>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("상담 카드 태그 역직렬화 실패", e);
        }
    }

    public record SaveConsultationCardRequest(
            String title,
            String stylistId,
            String portfolioId,
            String stylistName,
            String salonName,
            List<ConfirmedTag> confirmedTags
    ) {
    }

    public record ConsultationCardResponse(
            Long id,
            String title,
            String stylistId,
            String portfolioId,
            String stylistName,
            String salonName,
            List<ConfirmedTag> confirmedTags,
            java.time.Instant createdAt
    ) {
    }
}
