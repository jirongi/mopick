package com.mopick.product.registration;

import com.mopick.api.ApiException;
import com.mopick.product.persistence.RegistrationStatus;
import com.mopick.product.persistence.StylistRegistrationEntity;
import com.mopick.product.persistence.StylistRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StylistRegistrationService {

    private final StylistRegistrationRepository stylistRegistrationRepository;

    public StylistRegistrationService(StylistRegistrationRepository stylistRegistrationRepository) {
        this.stylistRegistrationRepository = stylistRegistrationRepository;
    }

    @Transactional
    public RegistrationResponse submit(SubmitRegistrationRequest request) {
        StylistRegistrationEntity entity = stylistRegistrationRepository.save(new StylistRegistrationEntity(
                request.applicantName(),
                request.email(),
                request.salonName(),
                request.region(),
                request.introduction()));
        return toResponse(entity);
    }

    public RegistrationResponse get(Long id) {
        return stylistRegistrationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException("REGISTRATION_NOT_FOUND", "등록 신청을 찾을 수 없습니다."));
    }

    private RegistrationResponse toResponse(StylistRegistrationEntity entity) {
        return new RegistrationResponse(
                entity.getId(),
                entity.getApplicantName(),
                entity.getEmail(),
                entity.getSalonName(),
                entity.getRegion(),
                entity.getIntroduction(),
                entity.getStatus(),
                entity.getCreatedAt());
    }

    public record SubmitRegistrationRequest(
            String applicantName,
            String email,
            String salonName,
            String region,
            String introduction
    ) {
    }

    public record RegistrationResponse(
            Long id,
            String applicantName,
            String email,
            String salonName,
            String region,
            String introduction,
            RegistrationStatus status,
            java.time.Instant createdAt
    ) {
    }
}
