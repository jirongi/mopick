package com.mopick.product.consultation;

import com.mopick.product.auth.AuthService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product/consultation-cards")
public class ConsultationCardController {

    private final AuthService authService;
    private final ConsultationCardService consultationCardService;

    public ConsultationCardController(AuthService authService, ConsultationCardService consultationCardService) {
        this.authService = authService;
        this.consultationCardService = consultationCardService;
    }

    @PostMapping
    public ConsultationCardService.ConsultationCardResponse save(
            @RequestHeader("Authorization") String authorization,
            @RequestBody ConsultationCardService.SaveConsultationCardRequest request) {
        return consultationCardService.save(authService.requireUser(authorization), request);
    }

    @GetMapping
    public List<ConsultationCardService.ConsultationCardResponse> list(
            @RequestHeader("Authorization") String authorization) {
        return consultationCardService.list(authService.requireUser(authorization));
    }

    @GetMapping("/{id}")
    public ConsultationCardService.ConsultationCardResponse get(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        return consultationCardService.get(authService.requireUser(authorization), id);
    }
}
